package com.ml.tblandroidtxt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.widget.TextView;

import com.ml.tblandroidtxt.editorial.pack.BundledEditorialEngineContractProfileRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineProfileResolver;

import java.util.concurrent.Executors;

/** UI-only import surface. All ZIP/security/persistence decisions remain headless. */
@SuppressLint({"SetTextI18n", "ObsoleteSdkInt"})
final class EditorialPackImportPageFactory {
    private final MainActivity activity;
    private AlertDialog progressDialog;
    private TextView progressText;

    EditorialPackImportPageFactory(MainActivity activity) { this.activity = activity; }

    static void showInstructions(MainActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Import Editorial Pack ZIP")
                .setMessage("ZIP cần một manifest canonical (editorial-pack.json) và đúng ba file nghiệp vụ theo manifest: PROJECT_INSTRUCTION, PROMPT và WORKFLOW. Import không có nghĩa là chứng nhận; pack có thể được lưu nhưng vẫn bị khóa.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Chọn ZIP", (dialog, which) -> new EditorialPackImportPageFactory(activity).start())
                .show();
    }

    private void start() {
        progressText = new TextView(activity);
        progressText.setText("Đang mở bộ chọn ZIP…");
        progressText.setTextColor(activity.TEXT);
        progressText.setPadding(activity.dp(22), activity.dp(18), activity.dp(22), activity.dp(18));
        progressText.setSingleLine(false);
        progressDialog = new AlertDialog.Builder(activity)
                .setTitle("Editorial Pack ZIP")
                .setView(progressText)
                .setNegativeButton("Đóng", null)
                .create();
        progressDialog.show();

        EditorialEngineProfileResolver loadedResolver;
        try {
            loadedResolver = new EditorialEngineProfileResolver(BundledEditorialEngineContractProfileRegistry.load());
        } catch (RuntimeException error) {
            // No fallback facts/profile are permitted. The resolver can only return
            // an unattested fail-closed blocker for this import.
            loadedResolver = EditorialEngineProfileResolver.failedClosed();
        }
        final EditorialEngineProfileResolver resolver = loadedResolver;
        EditorialPackImportCoordinator coordinator = new EditorialPackImportCoordinator(
                uri -> activity.getContentResolver().openInputStream(uri),
                (stream, listener) -> {
                    try (TranslationRepository database = new TranslationRepository(activity)) {
                        EditorialPackImportService service = new EditorialPackImportService(activity, database, resolver);
                        return service.importZip(stream, listener);
                    }
                },
                Executors.newSingleThreadExecutor(),
                this::render);
        activity.attachEditorialPackImportCoordinator(coordinator);
        if (!coordinator.beginPicker()) return;
        try {
            EditorialPackSafBridge.launch(activity);
        } catch (RuntimeException error) {
            coordinator.onPickerCancelled();
            render(EditorialPackImportUiState.phase(EditorialPackImportUiState.Phase.ERROR,
                    "Không thể mở bộ chọn ZIP", error.getMessage()));
        }
    }

    private void render(EditorialPackImportUiState state) {
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        activity.runOnUiThread(() -> {
            if (progressText == null) return;
            String text = state.title();
            if (!state.detail().isEmpty()) text += "\n\n" + state.detail();
            progressText.setText(text);
            if (state.phase() == EditorialPackImportUiState.Phase.READY_FOR_CERTIFICATION
                    || state.phase() == EditorialPackImportUiState.Phase.STORED_BLOCKED) {
                if (progressDialog != null) {
                    progressDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Xem pack detail", (dialog, which) -> {
                        if (!state.canonicalHash().isEmpty()) new EditorialPackManagementPageFactory(activity).show(state.canonicalHash());
                    });
                }
            }
        });
    }

}
