package feastcraft.expeditions.gametest;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.base.BaseBounds;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.base.BaseFuel;
import feastcraft.expeditions.base.FuelTank;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Stasis, tested in the real base level. The fuel tank is shared, so each test is its own batch
 * and batches run one after another. Tests that need a chunk ticking without fuel force it with a
 * ticket of their own, so stasis is the only thing that can stop the tick.
 */
@GameTestHolder(Expeditions.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StasisTests {
    private static final long PLENTY = 1_000_000;
    private static final UUID TEST_TICKETS = UUID.nameUUIDFromBytes("feastcraft_expeditions:stasis_tests".getBytes());

    private StasisTests() {
    }

    @GameTest(template = "empty", batch = "stasis_furnace", timeoutTicks = 800)
    public static void furnaceStallsWhileFrozenAndSmeltsOnceFueled(GameTestHelper helper) {
        ServerLevel base = base(helper);
        BlockPos pos = new BlockPos(40, 100, 40);
        setFuel(base, 0);
        forceTicking(base, pos, true);
        FurnaceBlockEntity furnace = placeLoadedFurnace(base, pos);

        helper.startSequence()
                .thenIdle(250)
                .thenExecute(() -> {
                    helper.assertTrue(furnace.getItem(2).isEmpty(), "a frozen base's furnace smelted");
                    setFuel(base, PLENTY);
                })
                .thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "the fueled furnace has not smelted yet"))
                .thenExecute(() -> {
                    base.removeBlock(pos, false);
                    forceTicking(base, pos, false);
                    setFuel(base, 0);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "stasis_wheat", timeoutTicks = 600)
    public static void wheatStaysWhileFrozenAndGrowsOnceFueled(GameTestHelper helper) {
        ServerLevel base = base(helper);
        MinecraftServer server = base.getServer();
        GameRules.IntegerValue randomTickSpeed = server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
        int previousSpeed = randomTickSpeed.get();
        BlockPos crop = new BlockPos(-40, 101, 40);
        setFuel(base, 0);
        forceTicking(base, crop, true);
        base.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
        base.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
        randomTickSpeed.set(4096, server);

        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertTrue(base.getBlockState(crop).getValue(CropBlock.AGE) == 0, "wheat grew in a frozen base");
                    setFuel(base, PLENTY);
                })
                .thenWaitUntil(() -> helper.assertTrue(base.getBlockState(crop).getValue(CropBlock.AGE) > 0, "the fueled wheat has not grown yet"))
                .thenExecute(() -> {
                    randomTickSpeed.set(previousSpeed, server);
                    base.removeBlock(crop, false);
                    base.removeBlock(crop.below(), false);
                    forceTicking(base, crop, false);
                    setFuel(base, 0);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "stasis_interaction")
    public static void buildingCraftingAndStorageWorkWhileFrozen(GameTestHelper helper) {
        ServerLevel base = base(helper);
        setFuel(base, 0);
        FakePlayer player = FakePlayerFactory.getMinecraft(base);
        BlockPos table = new BlockPos(40, 100, -40);
        BlockPos chest = table.east(2);
        BlockPos placed = table.above();
        base.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
        base.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());

        base.getBlockState(table).use(base, player, InteractionHand.MAIN_HAND, hitTop(table));
        helper.assertTrue(player.containerMenu instanceof CraftingMenu, "the crafting table did not open");
        player.closeContainer();

        base.getBlockState(chest).use(base, player, InteractionHand.MAIN_HAND, hitTop(chest));
        helper.assertTrue(player.containerMenu instanceof ChestMenu, "the chest did not open");
        player.closeContainer();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitTop(table)));
        helper.assertTrue(base.getBlockState(placed).is(Blocks.STONE), "could not build in a frozen base");

        base.removeBlock(placed, false);
        base.removeBlock(chest, false);
        base.removeBlock(table, false);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "stasis_border", timeoutTicks = 600)
    public static void fueledBaseRunsToItsBorderCornerWithNoPlayerThere(GameTestHelper helper) {
        ServerLevel base = base(helper);
        BaseBounds bounds = BaseDimension.bounds();
        int far = bounds.widthInChunks() * 16 - 1;
        BlockPos corner = new BlockPos(bounds.minBlockX() + far, 100, bounds.minBlockZ() + far);
        setFuel(base, PLENTY);
        FurnaceBlockEntity furnace = placeLoadedFurnace(base, corner);

        helper.assertTrue(base.players().isEmpty(), "a player is in the base");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "the corner furnace has not smelted yet"))
                .thenExecute(() -> {
                    base.removeBlock(corner, false);
                    setFuel(base, 0);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "stasis_elsewhere", timeoutTicks = 600)
    public static void frozenBaseLeavesOtherDimensionsAlone(GameTestHelper helper) {
        setFuel(base(helper), 0);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        FurnaceBlockEntity furnace = placeLoadedFurnace(helper.getLevel(), pos);
        helper.succeedWhen(() -> helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "the overworld furnace has not smelted yet"));
    }

    @GameTest(template = "empty", batch = "stasis_world_border")
    public static void overworldBorderChangeLeavesTheBaseBorderAlone(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel base = base(helper);
        BaseBounds bounds = BaseDimension.bounds();
        double previousSize = server.overworld().getWorldBorder().getSize();

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "worldborder set 1000");
        helper.assertTrue(server.overworld().getWorldBorder().getSize() == 1000, "the overworld border did not change");
        helper.assertTrue(base.getWorldBorder().getSize() == bounds.borderSize(), "the base border followed the overworld");
        helper.assertTrue(base.getWorldBorder().getCenterX() == bounds.borderCenterX(), "the base border moved");

        server.overworld().getWorldBorder().setSize(previousSize);
        helper.succeed();
    }

    private static ServerLevel base(GameTestHelper helper) {
        return BaseDimension.level(helper.getLevel().getServer());
    }

    /** Random ticks without a player nearby need a Forge ticking ticket; a vanilla forced chunk does not get them. */
    private static void forceTicking(ServerLevel base, BlockPos pos, boolean add) {
        ForgeChunkManager.forceChunk(base, Expeditions.MOD_ID, TEST_TICKETS, pos.getX() >> 4, pos.getZ() >> 4, add, true);
    }

    private static void setFuel(ServerLevel base, long ticks) {
        BaseFuel.get(base).update(base, tank -> new FuelTank(ticks));
    }

    private static FurnaceBlockEntity placeLoadedFurnace(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
        FurnaceBlockEntity furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        return furnace;
    }

    private static BlockHitResult hitTop(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
    }
}
