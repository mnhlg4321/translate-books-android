package com.ml.tblandroidtxt.editorial.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * The HOST SOURCE MANIFEST that opens turn 1 of a V5 chain. The pack asks every turn to record a Source Manifest with the
 * ID, series and version, each file's name, character and line count, first and last anchors and a SHA-256 "if the host
 * provides it", and tells the model not to hash text itself. This class is the host: it counts and hashes the exact bytes it
 * attaches, so the model only has to quote the block. Pure; the anchors travel only to the provider, as part of the same
 * request that carries the files, and {@link #redactedLines} gives a form without them for reports.
 */
public final class V5HostSourceManifest {
    public static final String VERSION = "V5-SAFE.4.1.3-FULL";
    public static final String REVISION = "V5-HOST-MANIFEST-1";
    public static final int ANCHOR_CODE_POINTS = 40;
    public static final String BEGIN = "=== HOST SOURCE MANIFEST ===";
    public static final String END = "=== END HOST SOURCE MANIFEST ===";

    public record Entry(String role, String name, long bytes, long chars, int lines, int nonblankLines,
                        String firstAnchor, String lastAnchor, String sha256) { }

    private V5HostSourceManifest() { }

    /** One entry per file, in the pack's canonical role order. */
    public static List<Entry> entries(List<EditInputs.OriginalSourceFile> files) {
        List<Entry> out = new ArrayList<>();
        for (EditInputs.OriginalSourceFile file : V5SourcePackPreflight.inRoleOrder(files)) {
            byte[] bytes = file.content().getBytes(StandardCharsets.UTF_8);
            String text = file.content().replace("\r\n", "\n").replace('\r', '\n');
            String[] lines = text.split("\n", -1);
            int count = lines.length;
            if (count > 0 && lines[count - 1].isEmpty()) count--;
            if (text.isEmpty()) count = 0;
            int nonblank = 0;
            String firstLine = "";
            String lastLine = "";
            for (int i = 0; i < count; i++) {
                if (lines[i].isBlank()) continue;
                nonblank++;
                if (nonblank == 1) firstLine = lines[i];
                lastLine = lines[i];
            }
            out.add(new Entry(file.role(), file.name(), bytes.length, file.content().codePointCount(0, file.content().length()),
                    count, nonblank, anchor(firstLine), anchor(lastLine), sha256(bytes)));
        }
        return out;
    }

    /** The block, ending without a trailing newline. */
    public static String render(V5SourceIdentity identity, List<EditInputs.OriginalSourceFile> files) {
        List<Entry> entries = entries(files);
        StringBuilder out = new StringBuilder(BEGIN).append('\n');
        out.append("manifest_revision=").append(REVISION).append('\n');
        out.append("ID=").append(identity.chainId()).append('\n');
        out.append("SERIES=").append(identity.series()).append('\n');
        out.append("VERSION=").append(VERSION).append('\n');
        out.append("EXECUTION_MODE=ONE_CHAT_L1_L2_L3\n");
        out.append("sha256_basis=exact attached bytes, computed by the host; the model does not recompute or normalize\n");
        out.append("count_basis=chars are Unicode code points; lines are LF/CRLF/CR separated and a final terminator adds none\n");
        out.append("anchor_basis=first and last non-blank line, at most ").append(ANCHOR_CODE_POINTS).append(" code points, escaped\n");
        out.append("FILE_COUNT=").append(entries.size()).append('\n');
        for (Entry e : entries) {
            out.append("FILE role=").append(e.role())
                    .append(" name=\"").append(escape(e.name())).append('"')
                    .append(" bytes=").append(e.bytes())
                    .append(" chars=").append(e.chars())
                    .append(" lines=").append(e.lines())
                    .append(" nonblank_lines=").append(e.nonblankLines())
                    .append(" first_anchor=\"").append(e.firstAnchor()).append('"')
                    .append(" last_anchor=\"").append(e.lastAnchor()).append('"')
                    .append(" sha256=").append(e.sha256())
                    .append(" bytes_readable=yes\n");
        }
        out.append(END);
        return out.toString();
    }

    /** The same lines without the anchors: safe for reports that must not contain book text. */
    public static List<String> redactedLines(V5SourceIdentity identity, List<EditInputs.OriginalSourceFile> files) {
        List<String> lines = new ArrayList<>();
        lines.add("ID=" + identity.chainId());
        lines.add("SERIES=" + identity.series());
        lines.add("VERSION=" + VERSION);
        for (Entry e : entries(files)) {
            lines.add("FILE role=" + e.role() + " name=\"" + escape(e.name()) + "\" bytes=" + e.bytes() + " chars=" + e.chars()
                    + " lines=" + e.lines() + " nonblank_lines=" + e.nonblankLines() + " sha256=" + e.sha256() + " bytes_readable=yes");
        }
        return lines;
    }

    static String anchor(String line) {
        String text = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
        int length = text.codePointCount(0, text.length());
        boolean cut = length > ANCHOR_CODE_POINTS;
        if (cut) text = text.substring(0, text.offsetByCodePoints(0, ANCHOR_CODE_POINTS));
        return escape(text) + (cut ? "…" : "");
    }

    static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20 || c == 0x7F || c == 0xFEFF) out.append(String.format("\\u%04X", (int) c));
            else out.append(c);
        }
        return out.toString();
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
