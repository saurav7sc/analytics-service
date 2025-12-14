package com.analytics.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MockEventGenerator {

    private static final List<String> EVENT_TYPES = List.of("page_view", "click", "scroll");
    private static final List<String> PAGES = List.of("/home", "/products", "/products/electronics", "/cart", "/checkout");

    public static void main(String[] args) {
        String endpoint = args.length > 0 ? args[0] : "http://localhost:8080/api/v1/events";
        int intervalMillis = args.length > 1 ? Integer.parseInt(args[1]) : 200;

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));

        executor.scheduleAtFixedRate(() -> {
            try {
                ObjectNode event = mapper.createObjectNode();
                String userId = "usr_" + (random.nextInt(15) + 1);
                String sessionId = "sess_" + UUID.randomUUID().toString().substring(0, 8);
                event.put("timestamp", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
                event.put("user_id", userId);
                event.put("event_type", EVENT_TYPES.get(random.nextInt(EVENT_TYPES.size())));
                event.put("page_url", PAGES.get(random.nextInt(PAGES.size())));
                event.put("session_id", sessionId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(event.toString()))
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .exceptionally(ex -> {
                            System.err.println("Failed to send event: " + ex.getMessage());
                            return null;
                        });
            } catch (Exception ex) {
                System.err.println("Generator error: " + ex.getMessage());
            }
        }, 0, intervalMillis, TimeUnit.MILLISECONDS);

        System.out.printf("Mock generator running against %s every %d ms. Press Ctrl+C to stop.%n", endpoint, intervalMillis);
    }
}
