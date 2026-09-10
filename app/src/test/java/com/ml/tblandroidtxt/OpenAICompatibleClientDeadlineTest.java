package com.ml.tblandroidtxt;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Test-first proof for the bounded RAW transport deadline. */
public final class OpenAICompatibleClientDeadlineTest {
    @Test public void dripBodyCannotOutliveTheExplicitAttemptDeadline() throws Exception {
        AppSettings settings = localSettings();
        try (DripServer server = new DripServer()) {
            settings.baseUrl = "http://127.0.0.1:" + server.port() + "/v1/chat/completions";
            long deadlineNanos = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(450L);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            long started = System.nanoTime();
            Future<OpenAICompatibleClient.ChatResult> call = executor.submit(() ->
                    OpenAICompatibleClient.chatWithUsage(settings,
                            new PromptPair("system", "synthetic body-read deadline"), 16,
                            "local-deadline", null, false, deadlineNanos,
                            new OpenAICompatibleClient.CallControl()));
            try {
                call.get(3, TimeUnit.SECONDS);
                fail("a body that keeps dripping past the attempt deadline must fail");
            } catch (java.util.concurrent.ExecutionException expected) {
                assertTrue(expected.getCause() instanceof SocketTimeoutException
                        || expected.getCause() instanceof IOException);
            } catch (TimeoutException timeout) {
                fail("client outlived the explicit attempt deadline");
            } finally {
                call.cancel(true);
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertTrue("deadline enforcement was not bounded: " + elapsedMillis,
                    elapsedMillis < 2_500L);
            assertTrue(server.requestCount.get() == 1);
        }
    }

    private static AppSettings localSettings() {
        AppSettings settings = new AppSettings();
        settings.provider = "openrouter";
        settings.model = "openai/gpt-5.6-luna";
        settings.apiKey = "local-test-key";
        settings.timeoutSeconds = 60;
        return settings;
    }

    private static final class DripServer implements AutoCloseable {
        private final ServerSocket server = new ServerSocket(0);
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final AtomicInteger requestCount = new AtomicInteger();
        private volatile Socket accepted;

        private DripServer() throws IOException {
            server.setSoTimeout(2_000);
            executor.submit(this::serve);
        }

        private int port() { return server.getLocalPort(); }

        private void serve() {
            try (Socket socket = server.accept()) {
                accepted = socket;
                requestCount.incrementAndGet();
                socket.setSoTimeout(2_000);
                readRequest(socket);
                byte[] first = "{".getBytes(StandardCharsets.UTF_8);
                byte[] header = ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"
                        + "Content-Length: 16\r\nConnection: close\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII);
                OutputStream output = socket.getOutputStream();
                output.write(header);
                output.write(first);
                output.flush();
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(100L);
                    output.write(' ');
                    output.flush();
                }
            } catch (Throwable ignored) {
                // The client is expected to close the socket at its deadline.
            }
        }

        private static void readRequest(Socket socket) throws IOException {
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            int previous = -1;
            int current;
            int length = 0;
            while ((current = input.read()) != -1) {
                headers.write(current);
                if (headers.size() > 32 * 1024) throw new IOException("headers too large");
                if (previous == '\r' && current == '\n' && headers.toString(StandardCharsets.US_ASCII)
                        .endsWith("\r\n\r\n")) break;
                previous = current;
            }
            for (String line : headers.toString(StandardCharsets.US_ASCII).split("\\r?\\n")) {
                if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                    length = Integer.parseInt(line.substring(15).trim());
                }
            }
            while (length > 0) {
                int read = input.read();
                if (read < 0) throw new IOException("request body truncated");
                length--;
            }
        }

        @Override public void close() throws Exception {
            try { server.close(); } catch (IOException ignored) { }
            Socket socket = accepted;
            if (socket != null) try { socket.close(); } catch (IOException ignored) { }
            executor.shutdownNow();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                throw new AssertionError("drip server cleanup incomplete");
            }
        }
    }
}
