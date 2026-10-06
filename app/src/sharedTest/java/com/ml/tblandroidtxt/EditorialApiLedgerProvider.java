package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fixture-runner wrapper: reserves the pinned worst case in the group ledger before every request and settles the real
 * charge after it. A charge above the reservation is never clamped: the ledger refuses it, the reservation stays pending
 * (so the group stops on UNKNOWN cost) and the exact amounts are kept in {@link #overruns()} for the run evidence. An answer
 * whose price is unknown is not settled either. Shared by the JVM tests and the instrumented runner.
 */
public final class EditorialApiLedgerProvider implements EditorialApiProvider {
    /** Receives every request with its answer, in order (prompt and response retention outside Git). */
    public interface Recorder {
        void record(EditorialApiFlow.Request request, EditorialApiFlow.StepResponse response) throws IOException;
    }

    /** One call whose actual charge was above what had been reserved for it. */
    public record Overrun(String callId, BigDecimal reserved, BigDecimal actual) { }

    private final EditorialApiProvider delegate;
    private final EditorialP6GroupSpendLedger ledger;
    private final EditorialApiRunService.Pricing pricing;
    private final String identity;
    private final String model;
    private final Recorder recorder;
    private final List<Overrun> overruns = Collections.synchronizedList(new ArrayList<>());

    public EditorialApiLedgerProvider(EditorialApiProvider delegate, EditorialP6GroupSpendLedger ledger,
                                      EditorialApiRunService.Pricing pricing, String identity, String model, Recorder recorder) {
        this.delegate = delegate;
        this.ledger = ledger;
        this.pricing = pricing;
        this.identity = identity;
        this.model = model;
        this.recorder = recorder;
    }

    public List<Overrun> overruns() { return List.copyOf(overruns); }

    @Override public void cancel() { delegate.cancel(); }

    @Override public EditorialApiFlow.StepResponse call(EditorialApiFlow.Request request, String modelName, int maxOutput, long timeoutMillis) {
        BigDecimal worst = pricing.inputPerToken(model).multiply(BigDecimal.valueOf(request.prompt().estimatedInputTokens()))
                .add(pricing.outputPerToken(model).multiply(BigDecimal.valueOf(maxOutput)));
        String callId = sha256(identity + "|" + request.step() + "|" + request.attempt());
        ledger.reserve(callId, request.step().name(), worst);
        EditorialApiFlow.StepResponse response = delegate.call(request, modelName, maxOutput, timeoutMillis);
        try {
            recorder.record(request, response);
        } catch (IOException failure) {
            throw new IllegalStateException("P6_RESPONSE_CAPTURE_INVALID", failure);
        }
        if (response.costKnown()) {
            if (response.cost().compareTo(worst) > 0) overruns.add(new Overrun(callId, worst, response.cost()));
            else ledger.settle(callId, response.cost());
        }
        return response;
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
