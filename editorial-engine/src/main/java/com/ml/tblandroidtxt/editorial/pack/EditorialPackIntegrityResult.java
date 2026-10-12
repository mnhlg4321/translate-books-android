package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collections;
import java.util.List;

public final class EditorialPackIntegrityResult {
    private final boolean valid;
    private final EditorialPackManifest manifest;
    private final List<Issue> issues;

    EditorialPackIntegrityResult(boolean valid, EditorialPackManifest manifest, List<Issue> issues) {
        this.valid = valid;
        this.manifest = manifest;
        this.issues = Collections.unmodifiableList(List.copyOf(issues));
    }

    public boolean valid() { return valid; }
    public EditorialPackManifest manifest() { return manifest; }
    public List<Issue> issues() { return issues; }
    public EditorialPackValidationCode primaryCode() { return issues.isEmpty() ? EditorialPackValidationCode.VALID : issues.get(0).code(); }

    public record Issue(EditorialPackValidationCode code, String path, String message) {
        public Issue {
            if (code == null) throw new IllegalArgumentException("Issue code is required");
            path = path == null ? "" : path;
            message = message == null ? "" : message;
        }
    }
}
