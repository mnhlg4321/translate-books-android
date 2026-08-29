package com.ml.tblandroidtxt;

/** Immutable source range plus optional reference-only context. */
public class Chunk {
    public int index;
    public String stableId;
    public int startOffset;
    public int endOffset;
    /** 1-based inclusive paragraph range; -1 means the legacy range is unknown. */
    public int paragraphStart = -1;
    public int paragraphEnd = -1;
    public int contextStartOffset;
    public int contextEndOffset;
    public String sourceHash;
    public String normalizedSourceHash;
    public String parentStableId;
    public String contextBefore;
    public String mainContent;
    public String contextAfter;

    public Chunk(int index, String contextBefore, String mainContent, String contextAfter) {
        this(index, -1, -1, contextBefore, mainContent, contextAfter, "");
    }

    public Chunk(int index, int startOffset, int endOffset, String contextBefore,
                 String mainContent, String contextAfter, String parentStableId) {
        this.index = index;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.contextBefore = safe(contextBefore);
        this.mainContent = safe(mainContent);
        this.contextAfter = safe(contextAfter);
        this.contextStartOffset = Math.max(0, startOffset - this.contextBefore.length());
        this.contextEndOffset = endOffset < 0 ? -1 : endOffset + this.contextAfter.length();
        this.sourceHash = HashUtil.sha256(this.mainContent);
        this.normalizedSourceHash = HashUtil.sha256(Chunker.normalizeSource(this.mainContent));
        this.parentStableId = safe(parentStableId);
        this.stableId = stableIdentity(startOffset, endOffset, this.sourceHash, this.parentStableId);
    }

    public static String stableIdentity(int start, int end, String sourceHash, String parent) {
        return HashUtil.sha256(start + ":" + end + ":" + safe(sourceHash) + ":" + safe(parent)).substring(0, 24);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
