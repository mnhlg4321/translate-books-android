package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditInputs;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.V5HostSourceManifest;
import com.ml.tblandroidtxt.editorial.api.V5SourceIdentity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Offline transport proof for V5_CHAT: three requests, one ordered history, one extracted FINAL. */
public final class V5ChatEditorialApiProviderTest {
    @Test public void sendsThreeTurnsWithThePriorAssistantAnswersAndExtractsFinal() throws Exception {
        try (SequenceServer server = new SequenceServer(List.of("REPORT_L1", "REPORT_L2", "QA <FINAL>draft</FINAL>"))) {
            AppSettings settings = settings(server);
            V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                    "unique project instruction", "global\nLƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
            EditorialApiFlow flow = new EditorialApiFlow(inputs(),
                    EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
            EditorialApiFlow.StepResponse answer = provider.call(flow.nextRequest(), settings.model, 100, 10_000);
            assertEquals("", answer.error());
            assertEquals(3, provider.physicalCalls());
            flow.accept(answer);
            assertTrue(flow.outcome().state() == EditorialApiContract.RunState.FINAL_OK
                    || flow.outcome().state() == EditorialApiContract.RunState.FINAL_NOTES);
            assertEquals("draft", flow.outcome().finalText());
            assertTrue(answer.inputTokens() > 0);
            assertTrue(answer.cost().signum() > 0);
            assertEquals(3, server.bodies.size());
            assertEquals(2, new JSONObject(server.bodies.get(0)).getJSONArray("messages").length());
            assertEquals(4, new JSONObject(server.bodies.get(1)).getJSONArray("messages").length());
            JSONArray third = new JSONObject(server.bodies.get(2)).getJSONArray("messages");
            assertEquals(6, third.length());
            assertEquals("assistant", third.getJSONObject(2).getString("role"));
            assertEquals("REPORT_L1", third.getJSONObject(2).getString("content"));
            assertEquals("assistant", third.getJSONObject(4).getString("role"));
            assertEquals("REPORT_L2", third.getJSONObject(4).getString("content"));
            for (String body : server.bodies) {
                JSONObject request = new JSONObject(body);
                JSONArray messages = request.getJSONArray("messages");
                int projectCount = 0;
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject message = messages.getJSONObject(i);
                    if ("system".equals(message.getString("role")) && "unique project instruction".equals(message.getString("content"))) projectCount++;
                }
                assertEquals(1, projectCount);
                assertEquals(V5ChatEditorialApiProvider.MIN_OUTPUT_TOKENS, request.getInt("max_tokens"));
                assertEquals("medium", request.optString("reasoning_effort"));
            }
            String first = new JSONObject(server.bodies.get(0)).getJSONArray("messages").getJSONObject(1).getString("content");
            for (String name : new String[] {RAW_NAME, DRAFT_NAME, GLOSSARY_NAME, PRONOUN_NAME}) assertTrue(first.contains("=== FILE: " + name + " ==="));
            assertFalse("generic placeholder names are never sent", first.contains("=== FILE: RAW.txt") || first.contains("=== FILE: GLOSSARY.csv"));
            assertTrue(first.contains("source,target,category,note,priority\nterm,term,term,,1"));
            assertTrue(first.contains("from,speaker,target,self,call,scope,note\n"));
            assertTrue(first.contains("raw source exact\r\n"));
            assertFalse(first.contains("APP DETECTIONS"));
            assertFalse(first.contains("QUALITY STANDARD"));
            assertFalse(first.contains("REFERENCE AND APP CONTEXT"));
            assertFalse(first.contains("# RAW\nsource"));
        }
    }

    @Test public void lengthOnTheFirstTurnStopsWithoutSendingTheSecond() throws Exception {
        try (SequenceServer server = new SequenceServer(List.of("partial"), List.of("length"))) {
            AppSettings settings = settings(server);
            V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                    "project", "LƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
            EditorialApiFlow flow = new EditorialApiFlow(inputs(),
                    EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
            EditorialApiFlow.StepResponse answer = provider.call(flow.nextRequest(), settings.model, 100, 10_000);
            assertEquals("V5_TRUNCATED_L1", answer.error());
            assertEquals(1, provider.physicalCalls());
            flow.accept(answer);
            assertEquals(EditorialApiContract.RunState.RETRY_REQUIRED, flow.outcome().state());
            assertEquals(1, server.bodies.size());
        }
    }

    @Test public void stopReceiptOnTheFirstTurnStopsBeforeAnyLaterProviderCall() throws Exception {
        try (SequenceServer server = new SequenceServer(List.of("stop_class: INPUT_REQUIRED\nreason_code: INPUT_GLOSSARY_SCHEMA_INVALID"))) {
            AppSettings settings = settings(server);
            V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                    "project", "LƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
            EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
            EditorialApiFlow.StepResponse answer = provider.call(flow.nextRequest(), settings.model, 100, 10_000);
            assertEquals("V5_STOP_INPUT_GLOSSARY_SCHEMA_INVALID", answer.error());
            assertEquals(1, provider.physicalCalls());
            flow.accept(answer);
            assertEquals(EditorialApiContract.RunState.RETRY_REQUIRED, flow.outcome().state());
            assertEquals(1, server.bodies.size());
        }
    }

    @Test public void turnOneOpensWithTheHostManifestThenThePromptAndTheFourBlocksWithOriginalNames() throws Exception {
        try (SequenceServer server = new SequenceServer(List.of("REPORT_L1", "REPORT_L2", "<FINAL>draft</FINAL>"))) {
            AppSettings settings = settings(server);
            V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                    "project", "PROMPT HEAD\nLƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
            EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
            provider.call(flow.nextRequest(), settings.model, 100, 10_000);
            String first = new JSONObject(server.bodies.get(0)).getJSONArray("messages").getJSONObject(1).getString("content");
            assertTrue("turn 1 begins with the manifest", first.startsWith(V5HostSourceManifest.BEGIN + "\n"));
            int manifestEnd = first.indexOf(V5HostSourceManifest.END);
            assertTrue(manifestEnd > 0);
            String manifest = first.substring(0, manifestEnd + V5HostSourceManifest.END.length());
            // the manifest is exactly the one the host renders for these files, with real hashes of the attached bytes
            assertEquals(V5HostSourceManifest.render(new V5SourceIdentity("007", "SAMPLE_SERIES_VOL1"), files()), manifest);
            assertTrue(manifest.contains("\nID=007\n"));
            assertTrue(manifest.contains("\nSERIES=SAMPLE_SERIES_VOL1\n"));
            assertTrue(manifest.contains("\nVERSION=V5-SAFE.4.1.3-FULL\n"));
            for (EditInputs.OriginalSourceFile file : files()) {
                assertTrue(file.name(), manifest.contains("name=\"" + file.name() + "\""));
                assertTrue(file.name(), manifest.contains("bytes=" + file.content().getBytes(StandardCharsets.UTF_8).length + " "));
                assertTrue(file.name(), manifest.contains("sha256=" + sha256(file.content()) + " bytes_readable=yes"));
            }
            // order: manifest, then the prompt text, then the workflow, then the four named blocks in role order
            int prompt = first.indexOf("PROMPT HEAD");
            int workflow = first.indexOf("WORKFLOW\nworkflow");
            int raw = first.indexOf("=== FILE: " + RAW_NAME + " ===");
            int draft = first.indexOf("=== FILE: " + DRAFT_NAME + " ===");
            int glossary = first.indexOf("=== FILE: " + GLOSSARY_NAME + " ===");
            int pronoun = first.indexOf("=== FILE: " + PRONOUN_NAME + " ===");
            assertTrue(manifestEnd < prompt && prompt < workflow && workflow < raw);
            assertTrue(raw < draft && draft < glossary && glossary < pronoun);
            // the content inside each block is exactly the attached text
            assertTrue(first.contains("=== FILE: " + RAW_NAME + " ===\n" + RAW_TEXT + "=== END FILE ==="));
            assertEquals("the manifest appears once, in turn 1 only", 1, first.split(java.util.regex.Pattern.quote(V5HostSourceManifest.BEGIN), -1).length - 1);
            assertFalse(new JSONObject(server.bodies.get(1)).getJSONArray("messages").getJSONObject(3).getString("content").contains(V5HostSourceManifest.BEGIN));
        }
    }

    @Test public void aMissingIdOrSeriesStopsBeforeAnyProviderCall() throws Exception {
        for (V5SourceIdentity identity : new V5SourceIdentity[] {V5SourceIdentity.NONE, new V5SourceIdentity("007", ""), new V5SourceIdentity("", "SAMPLE")}) {
            try (SequenceServer server = new SequenceServer(List.of("never"))) {
                AppSettings settings = settings(server);
                V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                        "project", "LƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
                EditorialApiFlow flow = new EditorialApiFlow(inputs(identity), EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
                EditorialApiFlow.StepResponse answer = provider.call(flow.nextRequest(), settings.model, 100, 10_000);
                assertEquals("V5_IDENTITY_MISSING", answer.error());
                assertEquals(0, provider.physicalCalls());
                assertEquals(0, server.bodies.size());
            }
        }
    }

    @Test public void placeholderNamesAreRefusedBeforeAnyProviderCall() throws Exception {
        try (SequenceServer server = new SequenceServer(List.of("never"))) {
            AppSettings settings = settings(server);
            V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                    "project", "LƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
            List<EditInputs.OriginalSourceFile> generic = List.of(
                    new EditInputs.OriginalSourceFile("RAW", "RAW.txt", RAW_TEXT), new EditInputs.OriginalSourceFile("DRAFT", "DRAFT.txt", DRAFT_TEXT),
                    new EditInputs.OriginalSourceFile("GLOSSARY", "GLOSSARY.csv", GLOSSARY_TEXT), new EditInputs.OriginalSourceFile("PRONOUN", "PRONOUN.csv", PRONOUN_TEXT));
            EditInputs placeholder = new EditInputs(RAW_TEXT, DRAFT_TEXT, "Vietnamese", List.of(), "", generic, new V5SourceIdentity("007", "SAMPLE_SERIES_VOL1"));
            EditorialApiFlow flow = new EditorialApiFlow(placeholder, EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
            EditorialApiFlow.StepResponse answer = provider.call(flow.nextRequest(), settings.model, 100, 10_000);
            assertEquals("V5_SOURCE_NAME_INVALID", answer.error());
            assertEquals(0, server.bodies.size());
        }
    }

    private static String sha256(String text) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void lunaCostFallbackUsesThePinnedMediumPricing() throws Exception {
        try (SequenceServer server = new SequenceServer(List.of("REPORT_L1", "REPORT_L2", "<FINAL>draft</FINAL>"))) {
            AppSettings settings = settings(server);
            V5ChatEditorialApiProvider provider = new V5ChatEditorialApiProvider(settings,
                    "project", "LƯỢT 1\none\nLƯỢT 2\ntwo\nLƯỢT 3\nthree", "workflow", "medium", null);
            EditorialApiFlow flow = new EditorialApiFlow(inputs(), EditorialApiFlow.Config.of(EditorialApiContract.Mode.V5_CHAT));
            EditorialApiFlow.StepResponse answer = provider.call(flow.nextRequest(), settings.model, 64, 10_000);
            assertTrue(answer.costKnown());
            assertEquals(0, new java.math.BigDecimal("0.000024").compareTo(answer.cost()));
        }
    }

    static final String RAW_NAME = "007_RAW_SAMPLE_SERIES_VOL1.txt";
    static final String DRAFT_NAME = "007_SAMPLE_SERIES_VOL1_DRAFT.txt";
    static final String GLOSSARY_NAME = "007_SAMPLE_SERIES_VOL1_chapter_glossary.csv";
    static final String PRONOUN_NAME = "007_PRONOUN_SAMPLE_SERIES_VOL1.csv";
    static final String RAW_TEXT = "raw source exact\r\n";
    static final String DRAFT_TEXT = "draft text exact\n";
    static final String GLOSSARY_TEXT = "source,target,category,note,priority\nterm,term,term,,1\n";
    static final String PRONOUN_TEXT = "from,speaker,target,self,call,scope,note\n";

    private static List<EditInputs.OriginalSourceFile> files() {
        return List.of(new EditInputs.OriginalSourceFile("RAW", RAW_NAME, RAW_TEXT),
                new EditInputs.OriginalSourceFile("DRAFT", DRAFT_NAME, DRAFT_TEXT),
                new EditInputs.OriginalSourceFile("GLOSSARY", GLOSSARY_NAME, GLOSSARY_TEXT),
                new EditInputs.OriginalSourceFile("PRONOUN", PRONOUN_NAME, PRONOUN_TEXT));
    }

    private static EditInputs inputs() {
        return inputs(new V5SourceIdentity("007", "SAMPLE_SERIES_VOL1"));
    }

    private static EditInputs inputs(V5SourceIdentity identity) {
        return new EditInputs(RAW_TEXT, DRAFT_TEXT, "Vietnamese", List.of(), "", files(), identity);
    }

    private static AppSettings settings(SequenceServer server) {
        AppSettings settings = new AppSettings();
        settings.apiKey = "offline-test-key";
        settings.model = "openai/gpt-5.6-luna";
        settings.baseUrl = "http://127.0.0.1:" + server.port() + "/v1/chat/completions";
        settings.timeoutSeconds = 10;
        return settings;
    }

    private static final class SequenceServer implements AutoCloseable {
        private final ServerSocket socket;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final List<String> bodies = new ArrayList<>();
        private final List<String> finishes;
        private final List<String> answers;

        SequenceServer(List<String> answers) throws IOException { this(answers, List.of("stop", "stop", "stop")); }
        SequenceServer(List<String> answers, List<String> finishes) throws IOException {
            this.socket = new ServerSocket(0);
            this.answers = answers;
            this.finishes = finishes;
            executor.submit(this::serve);
        }
        int port() { return socket.getLocalPort(); }

        private void serve() {
            try {
                for (int i = 0; i < answers.size(); i++) {
                    try (Socket client = socket.accept()) {
                        byte[] request = readRequest(client);
                        synchronized (bodies) { bodies.add(new String(request, StandardCharsets.UTF_8)); }
                        String finish = finishes.get(Math.min(i, finishes.size() - 1));
                        String json = new JSONObject()
                                .put("id", "offline-" + i)
                                .put("model", "openai/gpt-5.6-luna")
                                .put("choices", new JSONArray().put(new JSONObject()
                                        .put("finish_reason", finish)
                                        .put("message", new JSONObject().put("content", answers.get(i)))))
                                .put("usage", new JSONObject().put("prompt_tokens", 10).put("completion_tokens", 5).put("total_tokens", 15))
                                .toString();
                        byte[] body = json.getBytes(StandardCharsets.UTF_8);
                        OutputStream out = client.getOutputStream();
                        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + body.length
                                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                        out.write(body); out.flush();
                    }
                }
            } catch (Throwable ignored) { }
        }

        private static byte[] readRequest(Socket client) throws IOException {
            BufferedInputStream in = new BufferedInputStream(client.getInputStream());
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            int prev = -1, cur, length = 0;
            while ((cur = in.read()) != -1) {
                headers.write(cur);
                String soFar = headers.toString(StandardCharsets.US_ASCII);
                if (soFar.endsWith("\r\n\r\n")) break;
                prev = cur;
            }
            for (String line : headers.toString(StandardCharsets.US_ASCII).split("\\r?\\n")) {
                if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) length = Integer.parseInt(line.substring(15).trim());
            }
            byte[] body = in.readNBytes(length);
            return body;
        }

        @Override public void close() throws Exception { socket.close(); executor.shutdownNow(); }
    }
}
