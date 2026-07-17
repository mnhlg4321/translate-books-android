package com.ml.tblandroidtxt;

import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

public class ApiErrorParser {
    public static String fromHttp(int code, String responseText) {
        String detail = extractProviderMessage(responseText);
        String tail = detail.isEmpty() ? preview(responseText, 900) : preview(detail, 900);
        String hint;
        if (code == 400) hint = "HTTP 400: Request không hợp lệ. Kiểm tra model, max_tokens, prompt hoặc endpoint.";
        else if (code == 401) hint = "HTTP 401: API key sai, hết hạn hoặc chưa được provider chấp nhận.";
        else if (code == 403) hint = "HTTP 403: API key không có quyền dùng model/endpoint này.";
        else if (code == 404) hint = "HTTP 404: Không tìm thấy endpoint hoặc model. Kiểm tra Base URL và tên model.";
        else if (code == 408) hint = "HTTP 408: Provider timeout trước khi trả kết quả.";
        else if (code == 409) hint = "HTTP 409: Provider báo xung đột request. Thử lại sau.";
        else if (code == 413) hint = "HTTP 413: Prompt/request quá lớn. Giảm chunk size hoặc glossary/pronoun inject limit.";
        else if (code == 422) hint = "HTTP 422: Provider không chấp nhận payload. Kiểm tra model, messages, max_tokens hoặc temperature.";
        else if (code == 429) hint = "HTTP 429: Rate limit/quota. Chờ rồi retry, giảm tốc độ hoặc kiểm tra số dư tài khoản.";
        else if (code >= 500 && code <= 599) hint = "HTTP " + code + ": Lỗi phía provider/API gateway. Có thể thử lại sau hoặc đổi provider/model.";
        else hint = "HTTP " + code + ": API trả lỗi.";
        return tail.isEmpty() ? hint : hint + " Chi tiết: " + tail;
    }

    public static String describe(Throwable t) {
        if (t == null) return "unknown error";
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof FileUtil.FileAccessException) return preview(root.getMessage(), 1200);
        if (root instanceof UnknownHostException) return "Không phân giải được host/API endpoint. Kiểm tra mạng hoặc Base URL.";
        if (root instanceof SocketTimeoutException) return "API timeout. Tăng REQUEST_TIMEOUT hoặc kiểm tra mạng/provider.";
        String msg = root.getMessage();
        if (msg == null || msg.trim().isEmpty()) msg = t.getClass().getSimpleName();
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("canceled") || lower.contains("cancelled") || lower.contains("request đã bị hủy")) return "Request đã bị hủy";
        if (lower.contains("failed to connect")) return "Không kết nối được API endpoint. Kiểm tra mạng, VPN/proxy hoặc Base URL.";
        if (lower.contains("json") && (lower.contains("unterminated") || lower.contains("expected") || lower.contains("malformed"))) {
            return "API trả response không đúng JSON OpenAI-compatible. Kiểm tra Base URL/provider. Chi tiết: " + preview(msg, 900);
        }
        if (lower.contains("no choices returned")) return "API không trả choices. Có thể sai endpoint/model hoặc provider trả format khác OpenAI-compatible.";
        if (lower.contains("empty response") || lower.contains("0 tokens")) return "Model trả response rỗng/0 token. Thử tăng max output tokens hoặc đổi model.";
        return preview(msg, 1200);
    }

    public static String extractProviderMessage(String responseText) {
        String raw = responseText == null ? "" : responseText.trim();
        if (raw.isEmpty()) return "";
        try {
            JSONObject o = new JSONObject(raw);
            JSONObject err = o.optJSONObject("error");
            if (err != null) {
                String message = err.optString("message", "");
                String type = err.optString("type", "");
                String code = err.optString("code", "");
                StringBuilder sb = new StringBuilder();
                if (!message.trim().isEmpty()) sb.append(message.trim());
                if (!type.trim().isEmpty()) sb.append(sb.length() == 0 ? "" : " | ").append("type=").append(type.trim());
                if (!code.trim().isEmpty()) sb.append(sb.length() == 0 ? "" : " | ").append("code=").append(code.trim());
                return sb.toString();
            }
            String message = o.optString("message", "");
            if (!message.trim().isEmpty()) return message.trim();
        } catch (Exception ignored) {}
        return "";
    }

    public static String preview(String s, int max) {
        if (s == null) return "";
        String one = s.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        if (one.length() <= max) return one;
        return one.substring(0, Math.max(0, max)) + "...";
    }
}
