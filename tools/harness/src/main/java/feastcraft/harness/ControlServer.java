package feastcraft.harness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;

/**
 * The HTTP control API. Every endpoint takes its arguments as query parameters, accepts GET or
 * POST, and answers with JSON: the action's result, or {@code {"error": ...}}.
 */
final class ControlServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    private static final Map<String, Function<Params, CompletableFuture<JsonElement>>> ENDPOINTS = Map.ofEntries(
            Map.entry("status", ClientActions::status),
            Map.entry("chat", ClientActions::chat),
            Map.entry("command", ClientActions::command),
            Map.entry("look", ClientActions::look),
            Map.entry("find", ClientActions::find),
            Map.entry("use", ClientActions::use),
            Map.entry("attack", ClientActions::attack),
            Map.entry("sneak", ClientActions::sneak),
            Map.entry("select", ClientActions::select),
            Map.entry("wait", ClientActions::waitTicks),
            Map.entry("click", ClientActions::click),
            Map.entry("screenshot", ClientActions::screenshot),
            Map.entry("quit", ClientActions::quit));

    private ControlServer() {
    }

    static void start(int port) {
        HttpServer server = bind(port);
        ENDPOINTS.forEach((name, action) -> server.createContext("/" + name, exchange -> handle(exchange, action)));
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "Harness control");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        LOGGER.info("Harness control API listening on port {}", port);
    }

    private static HttpServer bind(int port) {
        try {
            return HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("could not bind the harness control port " + port, e);
        }
    }

    private static void handle(HttpExchange exchange, Function<Params, CompletableFuture<JsonElement>> action) throws IOException {
        int status = 200;
        JsonElement body;
        try {
            Params params = parse(exchange.getRequestURI().getRawQuery());
            long timeout = params.has("timeout") ? params.integer("timeout", 1, 3600) : DEFAULT_TIMEOUT_SECONDS;
            body = action.apply(params).get(timeout, TimeUnit.SECONDS);
        } catch (Refusal refusal) {
            status = refusal.status;
            body = error(refusal.getMessage());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Refusal refusal) {
                status = refusal.status;
                body = error(refusal.getMessage());
            } else {
                LOGGER.error("Harness action {} failed", exchange.getRequestURI(), e.getCause());
                status = 500;
                body = error(e.getCause().toString());
            }
        } catch (TimeoutException e) {
            status = 504;
            body = error("timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = 503;
            body = error("interrupted");
        }

        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Params parse(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            Arrays.stream(rawQuery.split("&")).forEach(pair -> {
                int equals = pair.indexOf('=');
                String key = equals < 0 ? pair : pair.substring(0, equals);
                String value = equals < 0 ? "" : pair.substring(equals + 1);
                values.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
            });
        }
        return new Params(values);
    }

    private static JsonObject error(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("error", message);
        return json;
    }
}
