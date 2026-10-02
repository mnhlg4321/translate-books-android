package com.ml.tblandroidtxt.editorial.pack;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * App-owned inventory of the RAW snapshot: every non-empty line is one unit with a stable id. The model never
 * invents ids; it only refers to them, and the app checks that every reference exists and that coverage ranges
 * close over the whole inventory.
 *
 * <p>Rules (frozen by tests, revision {@value #REVISION}): the bytes must be valid UTF-8; a leading BOM is
 * removed; lines are split on {@code \n}; one trailing {@code \r} per line is removed (CRLF and LF give the same
 * ids); a line that is blank (any Unicode whitespace, U+3000 and U+FEFF included) or consists only of an image
 * marker such as {@code [IMG_001]} is excluded and reported with its reason; the id of a unit is
 * {@code u:<1-based physical line>:<first 8 hex of sha256(line text)>}. A lone {@code \r} is not a separator.</p>
 */
public final class EditorialRawInventory {
    public static final String REVISION = "RAW_LINE_INVENTORY_V1";
    private static final Pattern IMAGE_MARKER = Pattern.compile(
            "^[\\s\\u3000\\uFEFF]*[\\[\\uFF3B](?:IMG|IMAGE|IMAGES|\\u633F\\u7D75|\\u30A4\\u30E9\\u30B9\\u30C8)"
                    + "[^\\]\\uFF3D]*[\\]\\uFF3D][\\s\\u3000]*$", Pattern.CASE_INSENSITIVE);

    public record Unit(String id, int line, String text, String textSha256) { }

    public record Excluded(int line, String reason) { }

    public record Range(String fromId, String toId, String status) { }

    public static final class Inventory {
        private final String rawSha256;
        private final int physicalLines;
        private final List<Unit> units;
        private final List<Excluded> excluded;
        private final String inventorySha256;
        private final Map<String, Integer> indexById;

        Inventory(String rawSha256, int physicalLines, List<Unit> units, List<Excluded> excluded) {
            this.rawSha256 = rawSha256;
            this.physicalLines = physicalLines;
            this.units = List.copyOf(units);
            this.excluded = List.copyOf(excluded);
            HashMap<String, Integer> index = new HashMap<>();
            StringBuilder ids = new StringBuilder(REVISION).append('\n');
            for (int i = 0; i < units.size(); i++) {
                index.put(units.get(i).id(), i);
                ids.append(units.get(i).id()).append('\n');
            }
            this.indexById = Collections.unmodifiableMap(index);
            this.inventorySha256 = EditorialCanonicalJson.sha256Hex(ids.toString().getBytes(StandardCharsets.UTF_8));
        }

        public String rawSha256() { return rawSha256; }
        public int physicalLines() { return physicalLines; }
        public List<Unit> units() { return units; }
        public List<Excluded> excluded() { return excluded; }
        public String inventorySha256() { return inventorySha256; }
        public boolean has(String id) { return indexById.containsKey(id); }
        public int indexOf(String id) { Integer i = indexById.get(id); return i == null ? -1 : i; }
        public Unit unit(String id) { int i = indexOf(id); return i < 0 ? null : units.get(i); }
    }

    private EditorialRawInventory() { }

    /** Builds the inventory or throws {@link IllegalArgumentException} with a typed, safe message. */
    public static Inventory build(byte[] raw) {
        if (raw == null) throw new IllegalArgumentException("INPUT_RAW_MISSING");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("INPUT_RAW_NOT_UTF8");
        }
        if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        String[] lines = text.split("\n", -1);
        List<Unit> units = new ArrayList<>();
        List<Excluded> excluded = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
            int number = i + 1;
            if (isBlank(line)) {
                excluded.add(new Excluded(number, "BLANK"));
            } else if (IMAGE_MARKER.matcher(line).matches()) {
                excluded.add(new Excluded(number, "IMAGE_MARKER"));
            } else {
                String sha = EditorialCanonicalJson.sha256Hex(line.getBytes(StandardCharsets.UTF_8));
                units.add(new Unit("u:" + number + ":" + sha.substring(0, 8), number, line, sha));
            }
        }
        return new Inventory(EditorialCanonicalJson.sha256Hex(raw), lines.length, units, excluded);
    }

    private static boolean isBlank(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (!Character.isWhitespace(c) && c != '﻿' && c != '　') return false;
        }
        return true;
    }

    /**
     * Coverage must be an ordered, contiguous, non-overlapping run of ranges that closes over every unit.
     * Returns sorted typed issue codes; empty means the ranges cover the inventory exactly once.
     */
    public static List<String> coverageIssues(Inventory inventory, List<Range> ranges) {
        List<String> issues = new ArrayList<>();
        if (ranges == null || ranges.isEmpty()) {
            if (!inventory.units().isEmpty()) issues.add("COVERAGE_EMPTY");
            return issues;
        }
        int next = 0;
        for (Range range : ranges) {
            int from = inventory.indexOf(range.fromId());
            int to = inventory.indexOf(range.toId());
            if (from < 0) { issues.add("COVERAGE_UNKNOWN_ID:" + range.fromId()); continue; }
            if (to < 0) { issues.add("COVERAGE_UNKNOWN_ID:" + range.toId()); continue; }
            if (to < from) { issues.add("COVERAGE_REVERSED:" + range.fromId()); continue; }
            if (from < next) issues.add("COVERAGE_OVERLAP:" + range.fromId());
            else if (from > next) issues.add("COVERAGE_GAP:" + inventory.units().get(next).id());
            next = Math.max(next, to + 1);
        }
        if (next < inventory.units().size() && issues.stream().noneMatch(i -> i.startsWith("COVERAGE_GAP"))) {
            issues.add("COVERAGE_GAP:" + inventory.units().get(next).id());
        }
        java.util.Collections.sort(issues);
        return issues;
    }

    // ---- chunking ----

    /** Frozen defaults; callers may pass smaller values, tests pin these. */
    public static final int DEFAULT_MAX_OWNED_UNITS = 200;
    public static final int DEFAULT_OVERLAP_UNITS = 6;

    /**
     * One chunk: the chunk owns units {@code ownedFrom..ownedTo} (inclusive indices) and may read context
     * units {@code contextFrom..contextTo} around them. Findings belong to the chunk that owns their first RAW
     * anchor; context units are never reported on by the chunk that only reads them.
     */
    public record Chunk(int index, int ownedFrom, int ownedTo, int contextFrom, int contextTo) {
        public int ownedCount() { return ownedTo - ownedFrom + 1; }
    }

    /** Balanced, deterministic windows: every unit is owned by exactly one chunk, none is dropped. */
    public static List<Chunk> planChunks(int unitCount, int maxOwnedUnits, int overlapUnits) {
        if (unitCount < 0 || maxOwnedUnits < 1 || overlapUnits < 0) throw new IllegalArgumentException("CHUNK_PARAMETERS_INVALID");
        List<Chunk> chunks = new ArrayList<>();
        if (unitCount == 0) return chunks;
        int count = (unitCount + maxOwnedUnits - 1) / maxOwnedUnits;
        int base = unitCount / count;
        int extra = unitCount % count;
        int start = 0;
        for (int i = 0; i < count; i++) {
            int size = base + (i < extra ? 1 : 0);
            int end = start + size - 1;
            chunks.add(new Chunk(i, start, end, Math.max(0, start - overlapUnits), Math.min(unitCount - 1, end + overlapUnits)));
            start = end + 1;
        }
        return chunks;
    }

    /** Index of the chunk that owns a unit index, or -1. */
    public static int ownerOf(List<Chunk> chunks, int unitIndex) {
        for (Chunk chunk : chunks) if (unitIndex >= chunk.ownedFrom() && unitIndex <= chunk.ownedTo()) return chunk.index();
        return -1;
    }
}
