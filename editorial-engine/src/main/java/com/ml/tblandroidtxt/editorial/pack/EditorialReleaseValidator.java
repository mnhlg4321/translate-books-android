package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Checks release artifact boundary without performing release or activation. */
public final class EditorialReleaseValidator {
    public record Artifact(String role, String contentContract, String sha256,
                           boolean executable) {
        public Artifact {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(contentContract, "contentContract");
            Objects.requireNonNull(sha256, "sha256");
        }
    }

    public record Request(List<Artifact> artifacts, int releaseAttemptCount,
                          boolean autoActivate, boolean autoRebind) {
        public Request { artifacts = List.copyOf(artifacts == null ? List.of() : artifacts); }
    }

    public record Result(boolean valid, List<String> issues) {
        public Result { issues = List.copyOf(issues == null ? List.of() : issues); }
    }

    public Result validate(Request request) {
        if (request == null) return new Result(false, List.of("RELEASE_REQUEST_MISSING"));
        List<String> issues = new ArrayList<>();
        Set<String> roles = new HashSet<>();
        for (Artifact artifact : request.artifacts()) {
            if (artifact == null) { issues.add("RELEASE_ARTIFACT_MISSING"); continue; }
            if (!roles.add(artifact.role())) issues.add("RELEASE_ARTIFACT_DUPLICATE:" + artifact.role());
            if (!EditorialSafe4Contract.RELEASE_ARTIFACT_ROLES.contains(artifact.role())) issues.add("RELEASE_ARTIFACT_UNKNOWN:" + artifact.role());
            if (!artifact.sha256().matches("[0-9a-fA-F]{64}")) issues.add("RELEASE_ARTIFACT_HASH_INVALID:" + artifact.role());
            if (artifact.executable()) issues.add("RELEASE_EXECUTABLE_FORBIDDEN:" + artifact.role());
        }
        if (!roles.equals(new HashSet<>(EditorialSafe4Contract.RELEASE_ARTIFACT_ROLES))) issues.add("RELEASE_ARTIFACT_SET_INCOMPLETE");
        if (request.releaseAttemptCount() != 0) issues.add("RELEASE_COUNTER_NONZERO");
        if (request.autoActivate()) issues.add("RELEASE_AUTO_ACTIVATE_FORBIDDEN");
        if (request.autoRebind()) issues.add("RELEASE_AUTO_REBIND_FORBIDDEN");
        return new Result(issues.isEmpty(), issues);
    }
}
