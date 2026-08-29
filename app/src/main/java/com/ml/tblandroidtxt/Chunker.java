package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, offset-based chunking. Main ranges cover normalized input exactly once. */
public class Chunker {
    public static String normalizeSource(String text) {
        return (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n');
    }

    public static List<Chunk> chunkText(String text, AppSettings settings) {
        AppSettings s = settings == null ? new AppSettings() : settings;
        String src = normalizeSource(text);
        ArrayList<Chunk> out = new ArrayList<>();
        if (src.isEmpty()) return out;
        int soft = Math.max(1, adaptiveLimit(s, s.effectiveSoftLimit()));
        int hard = Math.max(soft, adaptiveLimit(s, s.effectiveHardLimit()));
        int start = 0;
        while (start < src.length()) {
            checkCancelled(start);
            int hardEnd = advanceToMeasure(src, start, hard, s.chunkMode);
            hardEnd = extendPastProtectedRegion(src,start,hardEnd);
            int softEnd = advanceToMeasure(src, start, soft, s.chunkMode);
            int end = hardEnd >= src.length() ? src.length() : chooseBoundary(src, start, softEnd, hardEnd);
            end = safeUnicodeBoundary(src, start, Math.max(start + 1, end));
            if (end <= start) end = safeUnicodeBoundary(src, start, Math.min(src.length(), start + 1));
            String main = src.substring(start, end);
            int context = s.contextOverlapEnabled ? Math.max(0, s.contextChars) : 0;
            int beforeStart = safeUnicodeBoundary(src, 0, Math.max(0, start - context));
            int afterEnd = safeUnicodeBoundary(src, end, Math.min(src.length(), end + context));
            out.add(new Chunk(out.size(), start, end, src.substring(beforeStart, start), main,
                    src.substring(end, afterEnd), ""));
            start = end;
        }
        Coverage coverage = verifyCoverage(src, out);
        if (!coverage.valid) throw new IllegalStateException(coverage.message);
        assignParagraphRanges(out);
        return out;
    }

    /**
     * Reconstructs the normalized full source from ordered main ranges and
     * annotates each chunk with its inclusive 1-based paragraph range.
     * Paragraph metadata is deliberately not part of identity, hashes or
     * persisted chunk schema.
     */
    public static void assignParagraphRanges(List<Chunk> chunks) {
        assignParagraphRangesInternal(chunks, 0, true);
    }

    private static void assignParagraphRangesInternal(List<Chunk> chunks, int paragraphBase, boolean validateOffsets) {
        if (chunks == null || chunks.isEmpty()) return;
        StringBuilder rebuilt = new StringBuilder();
        boolean anyKnownOffset = false;
        boolean allKnownOffsets = true;
        int previousEnd = -1;
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            if (chunk == null) throw new IllegalArgumentException("Cannot assign paragraph ranges to a null chunk at " + i);
            String main = normalizeSource(chunk.mainContent);
            rebuilt.append(main);
            boolean known = chunk.startOffset >= 0 && chunk.endOffset >= chunk.startOffset;
            anyKnownOffset |= known;
            allKnownOffsets &= known;
            if (validateOffsets && known) {
                if (i == 0 && chunk.startOffset != 0) throw new IllegalArgumentException("Chunk ranges do not start at offset 0");
                if (previousEnd >= 0 && chunk.startOffset != previousEnd) {
                    throw new IllegalArgumentException("Chunk ranges are not continuous before chunk " + i);
                }
                if (chunk.endOffset - chunk.startOffset != main.length()) {
                    throw new IllegalArgumentException("Chunk range/text length mismatch at chunk " + i);
                }
                previousEnd = chunk.endOffset;
            }
        }
        if (validateOffsets && anyKnownOffset && !allKnownOffsets) {
            throw new IllegalArgumentException("Cannot validate continuity when only some chunk offsets are known");
        }
        if (validateOffsets && allKnownOffsets) {
            Chunk last = chunks.get(chunks.size() - 1);
            if (last.endOffset != rebuilt.length()) {
                throw new IllegalArgumentException("Chunk ranges do not cover the reconstructed source continuously");
            }
        }
        ArrayList<ParagraphBlock> paragraphs = paragraphBlocks(rebuilt.toString());
        int cursor = 0;
        for (Chunk chunk : chunks) {
            String main = normalizeSource(chunk.mainContent);
            int end = cursor + main.length();
            int first = -1, last = -1;
            for (int i = 0; i < paragraphs.size(); i++) {
                ParagraphBlock paragraph = paragraphs.get(i);
                if (paragraph.end > cursor && paragraph.start < end) {
                    if (first < 0) first = i + 1;
                    last = i + 1;
                }
            }
            if (first < 0 && end > cursor) {
                int previousParagraph = -1;
                int nextParagraph = -1;
                for (int i = 0; i < paragraphs.size(); i++) {
                    ParagraphBlock paragraph = paragraphs.get(i);
                    if (paragraph.end <= cursor) previousParagraph = i + 1;
                    else if (paragraph.start >= end) { nextParagraph = i + 1; break; }
                }
                if (previousParagraph > 0 && nextParagraph > 0) {
                    first = previousParagraph;
                    last = nextParagraph;
                } else if (previousParagraph > 0) first = last = previousParagraph;
                else if (nextParagraph > 0) first = last = nextParagraph;
            }
            if (first < 0) {
                chunk.paragraphStart = -1;
                chunk.paragraphEnd = -1;
            } else {
                chunk.paragraphStart = paragraphBase + first;
                chunk.paragraphEnd = paragraphBase + last;
            }
            cursor = end;
        }
    }

    private static ArrayList<ParagraphBlock> paragraphBlocks(String source) {
        ArrayList<ParagraphBlock> result = new ArrayList<>();
        int paragraphStart = -1;
        int paragraphEnd = -1;
        int lineStart = 0;
        while (lineStart <= source.length()) {
            int newline = source.indexOf('\n', lineStart);
            int lineEnd = newline < 0 ? source.length() : newline;
            String line = source.substring(lineStart, lineEnd);
            if (!line.trim().isEmpty()) {
                if (paragraphStart < 0) paragraphStart = lineStart;
                paragraphEnd = lineEnd;
            } else if (paragraphStart >= 0) {
                result.add(new ParagraphBlock(paragraphStart, paragraphEnd));
                paragraphStart = -1;
                paragraphEnd = -1;
            }
            if (newline < 0) break;
            lineStart = newline + 1;
        }
        if (paragraphStart >= 0) result.add(new ParagraphBlock(paragraphStart, paragraphEnd));
        return result;
    }

    private static final class ParagraphBlock {
        final int start;
        final int end;
        ParagraphBlock(int start, int end) { this.start = start; this.end = end; }
    }

    /** Splits only the translatable range; parent becomes SUPERSEDED in persistence. */
    public static List<Chunk> splitForContextOverflow(Chunk parent, AppSettings settings) {
        AppSettings reduced = settings == null ? new AppSettings() : settings.copy();
        if ("char".equalsIgnoreCase(reduced.chunkMode)) reduced.maxCharsPerChunk = Math.max(1, parent.mainContent.codePointCount(0,parent.mainContent.length()) / 2);
        else reduced.maxTokensPerChunk = Math.max(1, approxTokens(parent.mainContent) / 2);
        List<Chunk> local = chunkText(parent.mainContent, reduced);
        ArrayList<Chunk> result = new ArrayList<>();
        for (Chunk child : local) {
            int absoluteStart = Math.max(0, parent.startOffset) + child.startOffset;
            int absoluteEnd = Math.max(0, parent.startOffset) + child.endOffset;
            result.add(new Chunk(parent.index + result.size(), absoluteStart, absoluteEnd,
                    child.contextBefore, child.mainContent, child.contextAfter, parent.stableId));
        }
        assignParagraphRangesInternal(result, Math.max(0, parent.paragraphStart - 1), false);
        return result;
    }

    public static Coverage verifyCoverage(String normalizedSource, List<Chunk> chunks) {
        String source = normalizeSource(normalizedSource);
        if (source.isEmpty() && (chunks == null || chunks.isEmpty())) return new Coverage(true, "ok");
        if (chunks == null || chunks.isEmpty()) return new Coverage(false, "no chunks");
        int cursor = 0;
        StringBuilder rebuilt = new StringBuilder(source.length());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            if (c.index != i) return new Coverage(false, "unstable index at " + i);
            if (c.startOffset != cursor) return new Coverage(false, "gap or overlap before chunk " + i);
            if (c.endOffset < c.startOffset || c.endOffset > source.length()) return new Coverage(false, "invalid range at " + i);
            if (!source.substring(c.startOffset, c.endOffset).equals(c.mainContent)) return new Coverage(false, "range/text mismatch at " + i);
            rebuilt.append(c.mainContent);
            cursor = c.endOffset;
        }
        if (cursor != source.length()) return new Coverage(false, "final chunk does not reach source end");
        if (!rebuilt.toString().equals(source)) return new Coverage(false, "reconstruction mismatch");
        return new Coverage(true, "ok");
    }

    public static class Coverage {
        public final boolean valid; public final String message;
        Coverage(boolean valid, String message) { this.valid = valid; this.message = message; }
    }

    /** Linear scan. The 4.4 implementation rebuilt and re-tokenized a growing substring per code point. */
    private static int advanceToMeasure(String src, int start, int limit, String mode) {
        int end = start, units = 0;
        double tokenScore = 0;
        boolean asciiWord = false;
        boolean chars = "char".equalsIgnoreCase(mode);
        int scanned = 0;
        while (end < src.length()) {
            if ((scanned++ & 4095) == 0) checkCancelled(end);
            int cp = src.codePointAt(end);
            int next = end + Character.charCount(cp);
            if (chars) {
                units++;
            } else if (isCjk(cp) || isKana(cp)) {
                tokenScore += 1; asciiWord = false;
            } else if (Character.isWhitespace(cp)) {
                asciiWord = false;
            } else if (cp < 128 && Character.isLetterOrDigit(cp)) {
                tokenScore += asciiWord ? .25 : 1; asciiWord = true;
            } else {
                tokenScore += .5; asciiWord = false;
            }
            int measured = chars ? units : Math.max(1, (int)Math.ceil(tokenScore));
            if (measured > limit && end > start) break;
            end = next;
            if (measured >= limit) break;
        }
        return end;
    }

    private static void checkCancelled(int position) {
        if (Thread.currentThread().isInterrupted()) throw new PreparationCancelledException("Chunk preparation cancelled at " + position);
    }

    public static final class PreparationCancelledException extends RuntimeException {
        PreparationCancelledException(String message) { super(message); }
    }

    private static int chooseBoundary(String src, int start, int softEnd, int hardEnd) {
        boolean inFence = isInsideFence(src, start), inYaml=isInsideYaml(src,start);
        int japaneseDepth = 0;
        int best = -1, bestScore = -1;
        for (int i = start; i < hardEnd; i++) {
            char c = src.charAt(i);
            if (c == '「' || c == '『') japaneseDepth++;
            else if ((c == '」' || c == '』') && japaneseDepth > 0) japaneseDepth--;
            if (i + 2 < src.length() && src.startsWith("```", i)) inFence = !inFence;
            else if(isYamlMarker(src,i))inYaml=!inYaml;
            int end = i + 1;
            if (end < softEnd || inFence || inYaml || japaneseDepth > 0) continue;
            int score = boundaryScore(src, end);
            if (score > bestScore || (score == bestScore && end > best)) { best = end; bestScore = score; }
        }
        return best > start ? best : hardEnd;
    }

    private static int extendPastProtectedRegion(String src,int start,int end){boolean fence=isInsideFence(src,start),yaml=isInsideYaml(src,start);int depth=0;for(int i=start;i<end;i++){char c=src.charAt(i);if(c=='「'||c=='『')depth++;else if((c=='」'||c=='』')&&depth>0)depth--;if(i+2<src.length()&&src.startsWith("```",i)){fence=!fence;i+=2;}else if(isYamlMarker(src,i)){yaml=!yaml;i+=2;}}
        int i=end;while(i<src.length()&&(depth>0||fence||yaml)){char c=src.charAt(i);if(c=='「'||c=='『')depth++;else if((c=='」'||c=='』')&&depth>0)depth--;if(i+2<src.length()&&src.startsWith("```",i)){fence=!fence;i+=2;}else if(isYamlMarker(src,i)){yaml=!yaml;i+=2;}i++;}return i;}
    private static boolean isYamlMarker(String s,int i){return i>=0&&i+3<=s.length()&&s.startsWith("---",i)&&(i==0||s.charAt(i-1)=='\n')&&(i+3==s.length()||s.charAt(i+3)=='\n');}
    private static boolean isInsideYaml(String s,int position){boolean in=false;for(int i=0;i<position;i++)if(isYamlMarker(s,i)){in=!in;i+=2;}return in;}

    private static int boundaryScore(String src, int end) {
        char c = src.charAt(end - 1);
        if (end >= 2 && src.charAt(end - 1) == '\n' && src.charAt(end - 2) == '\n') return 100;
        if (c == '\n') return lineLooksStructural(src, end) ? 120 : 80;
        if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?' || c == '」' || c == '』') return 60;
        if (Character.isWhitespace(c) || c == ',' || c == '、' || c == ';' || c == ':') return 30;
        return 0;
    }

    private static boolean lineLooksStructural(String src, int end) {
        int from = Math.max(0, end - 80);
        String line = src.substring(from, end).trim();
        return line.matches("(?i).*(chapter|part|section|第.{0,12}[章話節]|---|===)$");
    }

    private static boolean isInsideFence(String src, int position) {
        int count = 0, at = 0;
        while ((at = src.indexOf("```", at)) >= 0 && at < position) { count++; at += 3; }
        return (count & 1) == 1;
    }

    private static int safeUnicodeBoundary(String src, int min, int candidate) {
        int end = Math.max(min, Math.min(src.length(), candidate));
        if (end > min && end < src.length() && Character.isHighSurrogate(src.charAt(end - 1)) && Character.isLowSurrogate(src.charAt(end))) end++;
        while (end > min && end < src.length()) {
            int cp = src.codePointAt(end);
            int type = Character.getType(cp);
            if (type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK && type != Character.ENCLOSING_MARK) break;
            end += Character.charCount(cp);
        }
        return Math.min(src.length(), end);
    }

    public static int measure(String text, String mode) { return "char".equalsIgnoreCase(mode) ? (text == null ? 0 : text.codePointCount(0, text.length())) : approxTokens(text); }
    public static int approxTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        double score = 0; boolean word = false;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if (isCjk(cp) || isKana(cp)) { score += 1; word = false; }
            else if (Character.isWhitespace(cp)) word = false;
            else if (cp < 128 && Character.isLetterOrDigit(cp)) { score += word ? .25 : 1; word = true; }
            else { score += .5; word = false; }
        }
        return Math.max(1, (int)Math.ceil(score));
    }

    static int adaptiveLimit(AppSettings s, int configured) {
        if (s == null || "char".equalsIgnoreCase(s.chunkMode) || "full".equalsIgnoreCase(s.optimizationPreset)) return configured;
        double multiplier = "economy".equalsIgnoreCase(s.optimizationPreset) ? 2.0 : "balanced".equalsIgnoreCase(s.optimizationPreset) ? 1.5 : 1.0;
        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(s.provider, s.model);
        int context = model == null || model.contextLength <= 0 ? 32000 : model.contextLength;
        int safe = Math.max(configured, context - Math.max(1024, s.maxOutputTokens) - 3000);
        return Math.min(safe, Math.max(configured, (int)Math.round(configured * multiplier)));
    }

    private static boolean isCjk(int c) { return c >= 0x4E00 && c <= 0x9FFF; }
    private static boolean isKana(int c) { return (c >= 0x3040 && c <= 0x30FF) || (c >= 0xFF66 && c <= 0xFF9D); }
}
