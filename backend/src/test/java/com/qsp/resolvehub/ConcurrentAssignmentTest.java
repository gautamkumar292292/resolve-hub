
package com.qsp.resolvehub;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentAssignmentTest {

    private static final String BASE_URL =
            "http://localhost:8080";

    // Paste a fresh, valid admin JWT here.
    private static final String ADMIN_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJ0b2tlblZlcnNpb24iOjM5LCJzdWIiOiJyZXNvbHZlaHViLnRlc3QuMjAyNjEwMjVAZXhhbXBsZS5jb20iLCJpYXQiOjE3OTMyNzgzMDAsImV4cCI6Nzc5MzI3ODMwMH0.yoYM4G-76ovCtrv59jRQUrnOW5vpZl6fcIX_mEOpHvU";

    private static final long TICKET_ID = 7;

    private static final HttpClient CLIENT =
            HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> request1 = executor.submit(
                    () -> sendAssignment(3, ready, start)
            );

            Future<?> request2 = executor.submit(
                    () -> sendAssignment(5, ready, start)
            );

            // Wait until both workers are ready.
            ready.await();

            System.out.println(
                    "Both requests ready. Sending now!"
            );

            // Release both workers together.
            start.countDown();

            request1.get();
            request2.get();

        } finally {
            executor.shutdown();
        }
    }

    private static void sendAssignment(
            long agentId,
            CountDownLatch ready,
            CountDownLatch start) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            BASE_URL + "/api/admin/tickets/"
                                    + TICKET_ID
                                    + "/assign?agentId="
                                    + agentId
                    ))
                    .header(
                            "Authorization",
                            "Bearer " + ADMIN_TOKEN
                    )
                    .POST(
                            HttpRequest.BodyPublishers.noBody()
                    )
                    .build();

            // Tell the main thread this worker is ready.
            ready.countDown();

            // Wait until both workers are released.
            start.await();

            HttpResponse<String> response = CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                    "Agent " + agentId
                            + " -> HTTP "
                            + response.statusCode()
            );

            System.out.println(
                    "Response: " + response.body()
            );

        } catch (Exception e) {
            ready.countDown();

            System.err.println(
                    "Agent " + agentId + " failed: "
                            + e.getClass().getName()
                            + " - " + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}
