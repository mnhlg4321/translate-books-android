package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class EditorialPackImportCoordinatorTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test public void pickerCancelNeverCallsImporterOrCreatesResult() {
        AtomicBoolean called = new AtomicBoolean();
        List<EditorialPackImportUiState> states = new ArrayList<>();
        EditorialPackImportCoordinator coordinator = coordinator((stream, progress) -> {
            called.set(true);
            return ready();
        }, states);
        assertTrue(coordinator.beginPicker());
        coordinator.onPickerCancelled();
        assertFalse(called.get());
        assertEquals(EditorialPackImportUiState.Phase.CANCELLED, states.get(states.size() - 1).phase());
        coordinator.close();
    }

    @Test public void doubleClickCreatesOnlyOneImport() {
        EditorialPackImportCoordinator coordinator = coordinator((stream, progress) -> ready(), new ArrayList<>());
        assertTrue(coordinator.beginPicker());
        assertFalse(coordinator.beginPicker());
        coordinator.onPickerCancelled();
        coordinator.close();
    }

    @Test public void importRunsOnWorkerAndClosesSourceAndRefreshesExactHash() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "editorial-pack-import-worker"));
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<EditorialPackImportUiState> finalState = new AtomicReference<>();
        AtomicBoolean worker = new AtomicBoolean();
        CloseAwareStream source = new CloseAwareStream(new byte[]{1, 2, 3});
        EditorialPackImportCoordinator coordinator = new EditorialPackImportCoordinator(
                uri -> source,
                (stream, progress) -> {
                    worker.set(!Thread.currentThread().getName().equals("main"));
                    return ready();
                },
                executor,
                state -> {
                    finalState.set(state);
                    if (state.phase() == EditorialPackImportUiState.Phase.READY_FOR_CERTIFICATION) done.countDown();
                });
        assertTrue(coordinator.beginPicker());
        coordinator.onSourceSelected(source);
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertTrue(worker.get());
        assertTrue(source.closed);
        assertEquals(HASH, finalState.get().canonicalHash());
        assertTrue(finalState.get().detail().contains("chưa được chứng nhận"));
        coordinator.close();
    }

    @Test public void importerFailureStillClosesSourceAndUsesSpecificInvalidWording() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<EditorialPackImportUiState> finalState = new AtomicReference<>();
        CloseAwareStream source = new CloseAwareStream(new byte[]{1});
        EditorialPackImportCoordinator coordinator = new EditorialPackImportCoordinator(
                uri -> source,
                (stream, progress) -> { throw new IllegalStateException("truncated ZIP"); },
                executor,
                state -> { finalState.set(state); if (state.phase() == EditorialPackImportUiState.Phase.INVALID) done.countDown(); });
        assertTrue(coordinator.beginPicker());
        coordinator.onSourceSelected(source);
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertTrue(source.closed);
        assertEquals(EditorialPackImportUiState.Phase.INVALID, finalState.get().phase());
        assertTrue(finalState.get().detail().contains("SNAPSHOT_WRITE_FAILED"));
        coordinator.close();
    }

    @Test public void blockedClassesNeverUseRunnableWording() {
        EditorialPackImportResult result = new EditorialPackImportResult("i", EditorialPackImportState.STORED_BLOCKED,
                EditorialPackImportError.COMPATIBILITY_BLOCKED, "missing adapter", "pack", "2.0.0", HASH,
                EditorialPackCompatibilityClass.ADAPTER_REQUIRED, "" );
        EditorialPackImportUiState state = EditorialPackImportResultMapper.result(result);
        assertEquals(EditorialPackImportUiState.Phase.STORED_BLOCKED, state.phase());
        assertTrue(state.title().contains("bị khóa"));
        assertTrue(state.detail().contains("ADAPTER_REQUIRED"));
        assertFalse(state.detail().contains("đã chứng nhận"));
        assertFalse(state.detail().contains("đã kích hoạt"));
    }

    private static EditorialPackImportCoordinator coordinator(EditorialPackImportCoordinator.ZipImporter importer,
                                                               List<EditorialPackImportUiState> states) {
        return new EditorialPackImportCoordinator(uri -> new ByteArrayInputStream(new byte[]{1}), importer,
                Runnable::run, states::add);
    }

    private static EditorialPackImportResult ready() {
        return new EditorialPackImportResult("i", EditorialPackImportState.STORED_READY_FOR_CERTIFICATION,
                EditorialPackImportError.NONE, "", "pack", "1.0.0", HASH,
                EditorialPackCompatibilityClass.DATA_COMPATIBLE, HASH);
    }

    private static final class CloseAwareStream extends ByteArrayInputStream {
        boolean closed;
        CloseAwareStream(byte[] data) { super(data); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
