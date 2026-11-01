package com.ml.tblandroidtxt;

/** Small, user-facing model labels for Translate headers and job cards. */
final class TranslateModelLabel {
    private TranslateModelLabel() {}

    static String shortName(String model) {
        String value = model == null ? "" : model.trim();
        if (value.isEmpty()) return "chưa chọn model";
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash < value.length() - 1) value = value.substring(slash + 1);
        return value;
    }

    static String header(String source, String target, String model) {
        String from = source == null || source.trim().isEmpty() ? "Ngôn ngữ nguồn" : source.trim();
        String to = target == null || target.trim().isEmpty() ? "Ngôn ngữ đích" : target.trim();
        return from + " → " + to + " • " + shortName(model);
    }

    static String details(String provider, String model) {
        String providerLabel = provider == null || provider.trim().isEmpty() ? "chưa chọn provider" : provider.trim();
        String modelId = model == null || model.trim().isEmpty() ? "chưa chọn model" : model.trim();
        return "Provider: " + providerLabel + "\nExact model ID: " + modelId;
    }

    static String jobCard(String model) {
        return model == null || model.trim().isEmpty() ? "chưa gửi request" : shortName(model);
    }
}
