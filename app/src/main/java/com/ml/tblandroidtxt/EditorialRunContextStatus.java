package com.ml.tblandroidtxt;

/** Read-only exact event/closed-run/lineage/binding projection. */
public final class EditorialRunContextStatus {
    private final EditorialRunContextStatusCode code;
    private final String closureEventIdentity;
    private final String closureEventFingerprint;
    private final String closedRunIdentity;
    private final String lineageIdentity;
    private final String bindingIdentity;
    private final String detail;

    private EditorialRunContextStatus(EditorialRunContextStatusCode code,
                                      String closureEventIdentity,
                                      String closureEventFingerprint,
                                      String closedRunIdentity,
                                      String lineageIdentity,
                                      String bindingIdentity,
                                      String detail) {
        this.code = code;
        this.closureEventIdentity = safe(closureEventIdentity);
        this.closureEventFingerprint = safe(closureEventFingerprint);
        this.closedRunIdentity = safe(closedRunIdentity);
        this.lineageIdentity = safe(lineageIdentity);
        this.bindingIdentity = safe(bindingIdentity);
        this.detail = safe(detail);
    }

    static EditorialRunContextStatus of(EditorialRunContextStatusCode code,
                                        EditorialRunClosureEvent event,
                                        String closedRunIdentity,
                                        String lineageIdentity,
                                        String bindingIdentity,
                                        String detail) {
        return new EditorialRunContextStatus(code,
                event == null ? "" : event.eventIdentity(),
                event == null ? "" : event.eventFingerprint(),
                closedRunIdentity, lineageIdentity, bindingIdentity, detail);
    }

    public EditorialRunContextStatusCode code() { return code; }
    public String closureEventIdentity() { return closureEventIdentity; }
    public String closureEventFingerprint() { return closureEventFingerprint; }
    public String closedRunIdentity() { return closedRunIdentity; }
    public String lineageIdentity() { return lineageIdentity; }
    public String bindingIdentity() { return bindingIdentity; }
    public String detail() { return detail; }

    private static String safe(String value) { return value == null ? "" : value; }
}
