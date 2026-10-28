package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.OpenAICompatibleClient;
import com.ml.tblandroidtxt.RunSnapshot;
import com.ml.tblandroidtxt.TranslationEngineCore;
import com.ml.tblandroidtxt.VolumePlan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QA data for the screenshots, offline: writes self-written QA_ chapters, Glossary and Pronoun files, runs a two-chapter volume with a
 * scripted provider (no network, no cost) so the history has something to show, then builds a phone-style library ZIP and settings
 * profile from a second data folder. Usage: {@code QaSeed <dataDir> <seedDir>}. Never touches the real data folder.
 */
public final class QaSeed {
    private QaSeed() {}

    public static void main(String[] args) throws Exception {
        Path data = Path.of(args[0]);
        Path seed = Path.of(args[1]);
        Path raw = Files.createDirectories(seed.resolve("volume").resolve("raw"));
        Path gl = Files.createDirectories(seed.resolve("volume").resolve("glossary"));
        Path pr = Files.createDirectories(seed.resolve("volume").resolve("pronoun"));
        for (int n = 1; n <= 3; n++) {
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < 4; p++) {
                sb.append("甲").append(n).append(p == 0 ? "" : "x");
                for (int i = 0; i < 84; i++) sb.append('日');
                sb.append("。\n\n");
            }
            write(raw.resolve("00" + n + "_QA_chuong.txt"), sb.toString());
        }
        write(gl.resolve("001_QA_glossary.csv"), "source,target,category,note\n甲1,QA thuật ngữ một,term,\n");
        write(gl.resolve("002_QA_glossary.csv"), "source,target,category,note\n甲2,QA thuật ngữ hai,term,\n");
        write(pr.resolve("001_QA_pronoun.csv"), "from,speaker,target,self,call,scope,note\nA,A,B,tao,mày,all,\n");
        write(pr.resolve("002_QA_pronoun.csv"), "from,speaker,target,self,call,scope,note\nB,B,A,tôi,anh,all,\n");

        AtomicInteger calls = new AtomicInteger();
        TranslationEngineCore.ProviderClient fake = (settings, prompt, limit, id, observer) -> {
            OpenAICompatibleClient.ChatResult r = new OpenAICompatibleClient.ChatResult();
            r.content = "<TRANSLATION>QA bản dịch " + calls.incrementAndGet() + ".</TRANSLATION>";
            r.finishReason = "stop";
            r.usageReported = true;
            r.promptTokens = 120; r.completionTokens = 60; r.totalTokens = 180;
            r.providerCostReported = true;
            r.providerCost = 0.0004;
            return r;
        };
        DesktopRuntime.UiEvents quiet = new DesktopRuntime.UiEvents() {
            @Override public void onSnapshot(RunSnapshot s) { }
            @Override public void onNotification(String text, int done, int total) { }
            @Override public void onRunEnded() { }
            @Override public void onCancelledEnd(String message) { }
        };
        try (DesktopRuntime rt = DesktopRuntime.open(data, quiet, fake)) {
            rt.secrets().save("TEST_KEY_NOT_SECRET");
            rt.importRawFolder(raw);
            rt.importGlossaryFolder(gl);
            rt.importPronounFolder(pr);
            List<String> two = new ArrayList<>();
            for (var item : rt.raws().list()) if (item.chapterNo <= 2) two.add(item.id);
            VolumePlan plan = rt.newVolumePlan(two);
            AppSettings s = rt.loadSettings();
            s.model = "qa/fixture-model";
            s.reasoningEffort = "off";
            s.optimizationPreset = "full";
            s.maxTokensPerChunk = 80;
            rt.saveSettings(s);
            Path out = Files.createDirectories(seed.resolve("out"));
            if (!rt.startVolume(plan, s, null, null, out)) throw new IllegalStateException("seed run did not start");
            long deadline = System.currentTimeMillis() + 60_000;
            Thread.sleep(300);
            while (rt.isRunning() && System.currentTimeMillis() < deadline) Thread.sleep(50);
        }
        // add the third chapter in a second pass so the first run stays a two-chapter run
        try (DesktopRuntime rt = DesktopRuntime.open(data, quiet, fake)) {
            Path only3 = Files.createDirectories(seed.resolve("volume").resolve("raw3"));
            Files.copy(raw.resolve("003_QA_chuong.txt"), only3.resolve("003_QA_chuong.txt"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            rt.importRawFolder(only3);
        }

        // the "phone" side: a different library and profile, exported to files
        Path phone = seed.resolve("phone-data");
        try (DesktopRuntime rt = DesktopRuntime.open(phone, quiet, fake)) {
            Path p1 = write(seed.resolve("phone_glossary.csv"), "source,target,category,note\n乙,QA thuật ngữ điện thoại,term,\n");
            Path p2 = write(seed.resolve("phone_pronoun.csv"), "from,speaker,target,self,call,scope,note\nC,C,D,mình,bạn,all,\n");
            Path p3 = write(seed.resolve("004_QA_phone.txt"), "乙" + "日".repeat(120) + "。\n\n乙" + "月".repeat(120) + "。\n");
            rt.importGlossaries(List.of(p1));
            rt.importPronouns(List.of(p2));
            rt.importRawFiles(List.of(p3));
            AppSettings s = rt.loadSettings();
            s.model = "qa/phone-model";
            s.reasoningEffort = "high";
            s.maxTokensPerChunk = 1500;
            s.contextChars = 300;
            rt.saveSettings(s);
            rt.exportLibrary(seed.resolve("QA_library_phone.zip"));
            write(seed.resolve("QA_profile_phone.json"), rt.exportProfileJson());
        }
        System.out.println("QA seed written: calls=" + calls.get());
    }

    private static Path write(Path file, String text) throws Exception {
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
