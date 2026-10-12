package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Crash-safe, hash-chained group spend ledger. An unsettled reservation is treated as UNKNOWN. */
public final class EditorialP6GroupSpendLedger {
    public static final BigDecimal RUNNER_MAX_GROUP_CAP_USD = new BigDecimal("6.00");
    private static final String GENESIS = "0".repeat(64);
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();
    public record Snapshot(BigDecimal exposedUsd, BigDecimal settledUsd, int pendingCalls,
                           int entries, String lastEntryHash) { }
    private record CallState(String phase, BigDecimal reserved, BigDecimal settled) { }
    private record ReadState(List<Map<String, Object>> entries, Map<String, CallState> calls,
                             String lastHash, String storedCap) { }

    private final Path path;
    private final String groupId;
    private final BigDecimal maximumUsd;
    private final Object lock;

    public EditorialP6GroupSpendLedger(Path path, String groupId, BigDecimal maximumUsd) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (groupId == null || groupId.isBlank()) throw new IllegalArgumentException("P6_SPEND_GROUP_REQUIRED");
        this.groupId = groupId;
        this.maximumUsd = Objects.requireNonNull(maximumUsd, "maximum USD").stripTrailingZeros();
        if (this.maximumUsd.signum() <= 0) throw new IllegalArgumentException("P6_SPEND_CAP_INVALID");
        this.lock = LOCKS.computeIfAbsent(this.path, ignored -> new Object());
    }

    /** Shared runner policy: Q2 may reserve up to the approved USD 6 group cap. */
    public static boolean isValidRunnerGroupCap(BigDecimal value) {
        return value != null && value.signum() > 0 && value.compareTo(RUNNER_MAX_GROUP_CAP_USD) <= 0;
    }

    /** One ledger identity per provider phase; a production chain attempt can contain several calls. */
    static String callId(String attemptIdentity, String phase) {
        requireText(attemptIdentity, "P6_SPEND_ATTEMPT_ID_REQUIRED");
        requireText(phase, "P6_SPEND_PHASE_REQUIRED");
        return EditorialCanonicalJson.sha256Hex(("P6_GROUP_CALL_ID_V1\n" + attemptIdentity + "\n" + phase)
                .getBytes(StandardCharsets.UTF_8));
    }

    /** Worst-case input and output cost on the pinned R5 rate basis. P4 must reprice before live use. */
    public static BigDecimal worstCase(EditorialL2Execution.Budget budget) {
        Objects.requireNonNull(budget, "budget");
        BigDecimal input = BigDecimal.valueOf((budget.maximumInputBytes() + 1L) / 2L);
        return input.multiply(new BigDecimal("0.25"))
                .add(BigDecimal.valueOf(budget.maximumOutputTokens()).multiply(new BigDecimal("1.20")))
                .movePointLeft(6);
    }

    /** Must be called before dispatch. Exposure includes settled actual cost plus any pending reservation. */
    public Snapshot reserve(String callId, String phase, BigDecimal worstCaseUsd) {
        requireText(callId, "P6_SPEND_CALL_ID_REQUIRED");
        requireText(phase, "P6_SPEND_PHASE_REQUIRED");
        BigDecimal reserve = amount(worstCaseUsd);
        synchronized (lock) {
            ReadState state = readState();
            assertCap(state);
            if (state.calls().values().stream().anyMatch(call -> call.settled() == null)) {
                throw new IllegalStateException("P6_SPEND_UNKNOWN_PREDECESSOR");
            }
            if (state.calls().containsKey(callId)) throw new IllegalStateException("P6_SPEND_CALL_ALREADY_RECORDED");
            if (exposure(state.calls()).add(reserve).compareTo(maximumUsd) > 0) {
                throw new IllegalStateException("P6_SPEND_GROUP_CAP_EXCEEDED");
            }
            append(state, "RESERVE", callId, phase, reserve);
            return snapshot(readState());
        }
    }

    /** Appends known provider cost. If the process dies before this, the reservation stays UNKNOWN. */
    public Snapshot settle(String callId, BigDecimal actualUsd) {
        requireText(callId, "P6_SPEND_CALL_ID_REQUIRED");
        BigDecimal actual = amount(actualUsd);
        synchronized (lock) {
            ReadState state = readState();
            assertCap(state);
            CallState call = state.calls().get(callId);
            if (call == null) throw new IllegalStateException("P6_SPEND_RESERVATION_MISSING");
            if (call.settled() != null) throw new IllegalStateException("P6_SPEND_CALL_ALREADY_SETTLED");
            append(state, "SETTLE", callId, call.phase(), actual);
            ReadState after = readState();
            if (actual.compareTo(call.reserved()) > 0) throw new IllegalStateException("P6_SPEND_CALL_OVERRUN");
            if (exposure(after.calls()).compareTo(maximumUsd) > 0) throw new IllegalStateException("P6_SPEND_GROUP_CAP_OVERRUN");
            return snapshot(after);
        }
    }

    public Snapshot inspect() {
        synchronized (lock) {
            ReadState state = readState();
            assertCap(state);
            return snapshot(state);
        }
    }

    private void append(ReadState state, String kind, String callId, String phase, BigDecimal amount) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(path)) {
                throw new IllegalStateException("P6_SPEND_LEDGER_SYMLINK_REFUSED");
            }
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("amountUsd", amount.toPlainString());
            body.put("callId", callId);
            body.put("groupId", groupId);
            body.put("groupMaximumUsd", maximumUsd.toPlainString());
            body.put("kind", kind);
            body.put("phase", phase);
            body.put("previousHash", state.lastHash());
            body.put("sequence", BigDecimal.valueOf(state.entries().size() + 1L));
            String hash = EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(body).getBytes(StandardCharsets.UTF_8));
            body.put("entryHash", hash);
            byte[] bytes = (EditorialCanonicalJson.canonicalize(body) + "\n").getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
        } catch (IOException error) {
            throw new IllegalStateException("P6_SPEND_LEDGER_WRITE_FAILED", error);
        }
    }

    private ReadState readState() {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return new ReadState(List.of(), Map.of(), GENESIS, "");
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("P6_SPEND_LEDGER_FILE_INVALID");
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<Map<String, Object>> entries = new ArrayList<>();
            Map<String, CallState> calls = new HashMap<>();
            String prior = GENESIS;
            String cap = "";
            for (String line : lines) {
                if (line.isBlank()) throw new IllegalStateException("P6_SPEND_LEDGER_EMPTY_ENTRY");
                Map<String, Object> entry = EditorialCanonicalJson.parseObject(line.getBytes(StandardCharsets.UTF_8));
                String hash = text(entry, "entryHash");
                LinkedHashMap<String, Object> projection = new LinkedHashMap<>(entry);
                projection.remove("entryHash");
                String expected = EditorialCanonicalJson.sha256Hex(EditorialCanonicalJson.canonicalize(projection).getBytes(StandardCharsets.UTF_8));
                if (!hash.equals(expected) || !prior.equals(text(entry, "previousHash"))
                        || !groupId.equals(text(entry, "groupId")) || number(entry, "sequence") != entries.size() + 1L) {
                    throw new IllegalStateException("P6_SPEND_LEDGER_HASH_CHAIN_INVALID");
                }
                String eventCap = text(entry, "groupMaximumUsd");
                if (cap.isEmpty()) cap = eventCap;
                else if (!cap.equals(eventCap)) throw new IllegalStateException("P6_SPEND_LEDGER_CAP_CHANGED");
                String callId = text(entry, "callId");
                String phase = text(entry, "phase");
                BigDecimal value = decimal(entry, "amountUsd");
                String kind = text(entry, "kind");
                if ("RESERVE".equals(kind)) {
                    if (calls.containsKey(callId)) throw new IllegalStateException("P6_SPEND_LEDGER_DUPLICATE_RESERVATION");
                    calls.put(callId, new CallState(phase, value, null));
                } else if ("SETTLE".equals(kind)) {
                    CallState old = calls.get(callId);
                    if (old == null || old.settled() != null || !old.phase().equals(phase)) {
                        throw new IllegalStateException("P6_SPEND_LEDGER_SETTLEMENT_INVALID");
                    }
                    calls.put(callId, new CallState(phase, old.reserved(), value));
                } else throw new IllegalStateException("P6_SPEND_LEDGER_EVENT_INVALID");
                entries.add(entry);
                prior = hash;
            }
            return new ReadState(List.copyOf(entries), Map.copyOf(calls), prior, cap);
        } catch (IOException error) {
            throw new IllegalStateException("P6_SPEND_LEDGER_READ_FAILED", error);
        }
    }

    private void assertCap(ReadState state) {
        if (!state.storedCap().isEmpty() && !state.storedCap().equals(maximumUsd.toPlainString())) {
            throw new IllegalStateException("P6_SPEND_LEDGER_CAP_CHANGED");
        }
    }

    private static Snapshot snapshot(ReadState state) {
        BigDecimal settled = BigDecimal.ZERO;
        int pending = 0;
        for (CallState call : state.calls().values()) {
            if (call.settled() == null) pending++;
            else settled = settled.add(call.settled());
        }
        return new Snapshot(exposure(state.calls()), settled, pending, state.entries().size(), state.lastHash());
    }

    private static BigDecimal exposure(Map<String, CallState> calls) {
        BigDecimal total = BigDecimal.ZERO;
        for (CallState call : calls.values()) total = total.add(call.settled() == null ? call.reserved() : call.settled());
        return total;
    }

    private static BigDecimal decimal(Map<String, Object> entry, String key) {
        try { return new BigDecimal(text(entry, key)); }
        catch (NumberFormatException invalid) { throw new IllegalStateException("P6_SPEND_LEDGER_AMOUNT_INVALID"); }
    }
    private static long number(Map<String, Object> entry, String key) {
        Object value = entry.get(key);
        if (!(value instanceof Number number)) throw new IllegalStateException("P6_SPEND_LEDGER_SEQUENCE_INVALID");
        return number.longValue();
    }
    private static String text(Map<String, Object> entry, String key) {
        Object value = entry.get(key);
        if (!(value instanceof String string) || string.isBlank()) throw new IllegalStateException("P6_SPEND_LEDGER_FIELD_INVALID");
        return string;
    }
    private static void requireText(String value, String code) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(code);
    }
    private static BigDecimal amount(BigDecimal value) {
        BigDecimal result = Objects.requireNonNull(value, "amount").stripTrailingZeros();
        if (result.signum() < 0) throw new IllegalArgumentException("P6_SPEND_AMOUNT_NEGATIVE");
        return result;
    }
}
