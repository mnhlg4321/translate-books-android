package com.ml.tblandroidtxt;

import android.net.Uri;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Coordinates SAF selection and the headless ZIP importer without owning pack logic. */
public final class EditorialPackImportCoordinator implements AutoCloseable {
    @FunctionalInterface
    public interface StreamOpener {
        InputStream open(Uri uri) throws Exception;
    }

    @FunctionalInterface
    public interface ZipImporter {
        EditorialPackImportResult importZip(InputStream stream, EditorialPackImportProgressListener progress);
    }

    private final StreamOpener opener;
    private final ZipImporter importer;
    private final Executor executor;
    private final Consumer<EditorialPackImportUiState> stateSink;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private volatile boolean closed;

    public EditorialPackImportCoordinator(StreamOpener opener, ZipImporter importer,
                                          Executor executor, Consumer<EditorialPackImportUiState> stateSink) {
        if (opener == null || importer == null || executor == null || stateSink == null) {
            throw new IllegalArgumentException("Import coordinator dependencies are required");
        }
        this.opener = opener;
        this.importer = importer;
        this.executor = executor;
        this.stateSink = stateSink;
    }

    /** Returns false on a double click while another import is active. */
    public boolean beginPicker() {
        if (closed || !busy.compareAndSet(false, true)) return false;
        emit(EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.PICKER_OPEN,
                "Chọn Editorial Pack ZIP", "ZIP sẽ được đọc một lần; hủy chọn không thay đổi registry."));
        return true;
    }

    public void onPickerCancelled() {
        if (!busy.compareAndSet(true, false)) return;
        emit(EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.CANCELLED,
                "Đã hủy chọn pack", "Không tạo import row, staging hoặc thay đổi registry."));
    }

    /** Called with the one selected SAF URI. The URI is opened once on the worker executor. */
    public void onUriSelected(Uri uri) {
        if (uri == null || closed || !busy.get()) {
            if (uri == null) finishInvalid(null, "Không mở được stream ZIP đã chọn.");
            return;
        }
        emit(EditorialPackImportResultMapper.progress(EditorialPackImportState.STAGING));
        submit(() -> opener.open(uri));
    }

    /** Test/lifecycle seam for a stream already opened by a trusted Android bridge. */
    public void onSourceSelected(InputStream source) {
        if (source == null || closed || !busy.get()) {
            if (source == null) finishInvalid(null, "Không mở được stream ZIP đã chọn.");
            return;
        }
        emit(EditorialPackImportResultMapper.progress(EditorialPackImportState.STAGING));
        submit(() -> source);
    }

    public boolean isBusy() { return busy.get(); }

    private void finishInvalid(EditorialPackImportResult result, String detail) {
        busy.set(false);
        if (!closed) emit(result == null
                ? EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.INVALID, "Không thể nhập pack", detail)
                : EditorialPackImportResultMapper.result(result));
    }

    private void submit(StreamSource source) {
        executor.execute(() -> {
            EditorialPackImportResult result;
            try (InputStream stream = source.open()) {
                if (stream == null) throw new IOException("Content provider returned no stream");
                result = importer.importZip(stream, state -> emit(EditorialPackImportResultMapper.progress(state)));
            } catch (Exception error) {
                result = new EditorialPackImportResult(
                        java.util.UUID.randomUUID().toString(), EditorialPackImportState.STAGING,
                        EditorialPackImportError.SNAPSHOT_WRITE_FAILED,
                        safeMessage(error), "", "", "", null, "");
            }
            if (closed) return;
            busy.set(false);
            emit(EditorialPackImportResultMapper.result(result));
        });
    }

    private void emit(EditorialPackImportUiState state) {
        if (!closed) {
            try { stateSink.accept(state); } catch (RuntimeException ignored) { }
        }
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    @FunctionalInterface
    private interface StreamSource { InputStream open() throws Exception; }

    @Override public void close() {
        closed = true;
        busy.set(false);
        if (executor instanceof ExecutorService service) service.shutdownNow();
    }
}
