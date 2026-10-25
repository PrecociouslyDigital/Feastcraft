package feastcraft.harness;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Hands work from the control server's threads to the client thread. Anything that touches the
 * game goes through {@link #onTick} or {@link #onFrame}; {@link #afterTicks} only measures time.
 */
final class ClientTicks {
    private record Due(long tick, CompletableFuture<Void> future) {
    }

    private static final Queue<Runnable> nextTick = new ConcurrentLinkedQueue<>();
    private static final Queue<Runnable> nextFrame = new ConcurrentLinkedQueue<>();
    private static final Queue<Due> waits = new ConcurrentLinkedQueue<>();
    private static volatile long tick;

    private ClientTicks() {
    }

    /** Runs {@code work} at the end of the next client tick. */
    static <T> CompletableFuture<T> onTick(Supplier<T> work) {
        return enqueue(nextTick, work);
    }

    /** Runs {@code work} right after the next frame is drawn, while the frame is still in the render target. */
    static <T> CompletableFuture<T> onFrame(Supplier<T> work) {
        return enqueue(nextFrame, work);
    }

    static CompletableFuture<Void> afterTicks(int ticks) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        waits.add(new Due(tick + ticks, future));
        return future;
    }

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = ++tick;
        waits.removeIf(due -> due.tick() <= now && due.future().complete(null));
        drain(nextTick);
    }

    @SubscribeEvent
    static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            drain(nextFrame);
        }
    }

    private static <T> CompletableFuture<T> enqueue(Queue<Runnable> queue, Supplier<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(() -> {
            try {
                future.complete(work.get());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    // Only what was queued before draining began, so work that queues more work waits a tick.
    private static void drain(Queue<Runnable> queue) {
        for (int n = queue.size(); n > 0; n--) {
            queue.remove().run();
        }
    }
}
