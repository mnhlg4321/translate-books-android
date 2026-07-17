package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FileUtil {

    public static class FileAccessException extends Exception {
        FileAccessException(String message) { super(message); }
        FileAccessException(String message, Throwable cause) { super(message, cause); }
    }

    public static class TextStats {
        public final int characters;
        public final int words;
        public final int lines;
        public final int approxTokens;

        TextStats(int characters, int words, int lines, int approxTokens) {
            this.characters = characters;
            this.words = words;
            this.lines = lines;
            this.approxTokens = approxTokens;
        }

        public String summary() {
            return fmt(words) + " words • " + fmt(characters) + " chars • " + fmt(lines) + " lines • ~" + fmt(approxTokens) + " tokens";
        }
    }

    public static class TreeEntry {
        public final String name;
        public final String mime;
        public final long size;
        public final long modified;
        public final Uri uri;
        TreeEntry(String name, String mime, long size, long modified, Uri uri) {
            this.name = name == null ? "" : name;
            this.mime = mime == null ? "" : mime;
            this.size = size;
            this.modified = modified;
            this.uri = uri;
        }
        public String detail() {
            String sizeTxt = size <= 0 ? "—" : humanSize(size);
            String dateTxt = modified <= 0 ? "" : " • " + new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(modified));
            return sizeTxt + dateTxt;
        }
    }

    public static class TextReadResult {
        public final String text;
        public final String encoding;
        public final int bytes;
        TextReadResult(String text, String encoding, int bytes) {
            this.text = text == null ? "" : text;
            this.encoding = encoding == null ? "unknown" : encoding;
            this.bytes = bytes;
        }
        public String detail() { return encoding + " • " + humanSize(bytes); }
    }

    public static String readText(Context c, Uri uri) throws Exception {
        return readTextDetailed(c, uri).text;
    }

    public static TextReadResult readTextDetailed(Context c, Uri uri) throws Exception {
        long declared=sizeOf(c,uri);
        if(declared>1024L*1024L)return readLargeText(c,uri,declared);
        if (uri == null) throw new FileAccessException("Input URI rỗng");
        try (InputStream is = c.getContentResolver().openInputStream(uri)) {
            if (is == null) throw new FileAccessException("Không mở được input URI. Hãy chọn lại file bằng Android file picker.");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) >= 0) bos.write(buf, 0, n);
            byte[] data = bos.toByteArray();
            return decodeText(data);
        } catch (SecurityException se) {
            throw new FileAccessException("Android không còn quyền đọc file này. Hãy chọn lại file input/config/glossary.", se);
        }
    }

    /** Incremental decoding avoids holding both the raw byte array and decoded book in memory. */
    private static TextReadResult readLargeText(Context c,Uri uri,long declared) throws Exception {
        try(InputStream raw=c.getContentResolver().openInputStream(uri)){
            if(raw==null)throw new FileAccessException("Cannot open large input URI");
            PushbackInputStream in=new PushbackInputStream(raw,3);byte[] bom=new byte[3];int n=in.read(bom);Charset charset=StandardCharsets.UTF_8;int skip=0;String label="UTF-8 streaming";
            if(n>=3&&(bom[0]&255)==0xEF&&(bom[1]&255)==0xBB&&(bom[2]&255)==0xBF){skip=3;label="UTF-8 BOM streaming";}
            else if(n>=2&&(bom[0]&255)==0xFF&&(bom[1]&255)==0xFE){skip=2;charset=Charset.forName("UTF-16LE");label="UTF-16LE streaming";}
            else if(n>=2&&(bom[0]&255)==0xFE&&(bom[1]&255)==0xFF){skip=2;charset=Charset.forName("UTF-16BE");label="UTF-16BE streaming";}
            if(n>skip)in.unread(bom,skip,n-skip);
            StringBuilder text=new StringBuilder((int)Math.min(Integer.MAX_VALUE-8,Math.max(16,declared)));char[] buffer=new char[16384];
            try(Reader reader=new InputStreamReader(in,charset)){int count;while((count=reader.read(buffer))>=0)text.append(buffer,0,count);}
            return new TextReadResult(text.toString(),label,(int)Math.min(Integer.MAX_VALUE,declared));
        }catch(SecurityException e){throw new FileAccessException("Android no longer has permission to read this file",e);}
    }

    public static long sizeOf(Context c,Uri uri){if(uri==null)return -1;try(Cursor cursor=c.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))return cursor.getLong(0);}catch(Exception ignored){}return -1;}

    public static String identityOf(Context c,Uri uri){if(uri==null)return "";long size=-1,modified=-1;String name="";String[] columns={OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE,Document.COLUMN_LAST_MODIFIED};try(Cursor cursor=c.getContentResolver().query(uri,columns,null,null,null)){if(cursor!=null&&cursor.moveToFirst()){if(!cursor.isNull(0))name=cursor.getString(0);if(!cursor.isNull(1))size=cursor.getLong(1);if(!cursor.isNull(2))modified=cursor.getLong(2);}}catch(Exception ignored){size=sizeOf(c,uri);name=displayName(c,uri);}return uri+":"+name+":"+size+":"+modified;}

    private static TextReadResult decodeText(byte[] data) throws Exception {
        if (data == null) data = new byte[0];
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return new TextReadResult(new String(data, 3, data.length - 3, StandardCharsets.UTF_8), "UTF-8 BOM", data.length);
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            return new TextReadResult(new String(data, 2, data.length - 2, Charset.forName("UTF-16LE")), "UTF-16LE BOM", data.length);
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            return new TextReadResult(new String(data, 2, data.length - 2, Charset.forName("UTF-16BE")), "UTF-16BE BOM", data.length);
        }
        try {
            return new TextReadResult(strictDecode(data, StandardCharsets.UTF_8), "UTF-8", data.length);
        } catch (CharacterCodingException ignored) {
            // Many Japanese TXT/CSV exports are still Shift-JIS/CP932. Keep this fallback
            // local to file import so translation output remains UTF-8.
            try {
                return new TextReadResult(strictDecode(data, Charset.forName("windows-31j")), "Windows-31J/Shift-JIS", data.length);
            } catch (Exception ignored2) {
                try {
                    return new TextReadResult(strictDecode(data, Charset.forName("Shift_JIS")), "Shift_JIS", data.length);
                } catch (Exception ignored3) {
                    return new TextReadResult(new String(data, StandardCharsets.UTF_8), "UTF-8 fallback", data.length);
                }
            }
        }
    }

    private static String strictDecode(byte[] data, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer decoded = decoder.decode(ByteBuffer.wrap(data));
        return decoded.toString();
    }

    public static void writeText(Context c, Uri uri, String text) throws Exception {
        if (uri == null) throw new FileAccessException("Output URI rỗng");
        try (OutputStream os = c.getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) throw new FileAccessException("Không mở được output URI để ghi. Hãy chọn lại output TXT/folder.");
            os.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (SecurityException se) {
            throw new FileAccessException("Android không còn quyền ghi output. Hãy chọn lại output TXT hoặc output folder.", se);
        }
    }

    /** Stages and verifies SAF output. The database remains authoritative if provider replacement is not atomic. */
    public static void writeTextVerified(Context c, Uri uri, String text) throws Exception {
        if (text == null || text.isEmpty()) throw new FileAccessException("Refusing to replace output with an empty file");
        java.io.File staged=java.io.File.createTempFile("tbl-output-",".pending",c.getCacheDir());
        try {
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(staged)){out.write(text.getBytes(StandardCharsets.UTF_8));out.flush();out.getFD().sync();}
            String expected=HashUtil.sha256(text);
            try(InputStream old=c.getContentResolver().openInputStream(uri)){
                if(old!=null){java.io.File backup=new java.io.File(c.getFilesDir(),"output-recovery-"+System.currentTimeMillis()+".txt");try(java.io.FileOutputStream copy=new java.io.FileOutputStream(backup)){byte[]buf=new byte[8192];int n;while((n=old.read(buf))>=0)copy.write(buf,0,n);copy.flush();copy.getFD().sync();}if(backup.length()>0)LogStore.append(c,"Output recovery copy: "+backup.getAbsolutePath());else backup.delete();}
            }catch(Exception ignored){LogStore.append(c,"No prior output available for recovery copy");}
            try(InputStream in=new java.io.FileInputStream(staged);OutputStream out=c.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new FileAccessException("Cannot open output URI");byte[]b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);out.flush();}
            if(!expected.equals(HashUtil.sha256(readText(c,uri))))throw new FileAccessException("Output verification failed; checkpoint remains authoritative");
        } finally { if(!staged.delete())staged.deleteOnExit(); }
    }

    public static Uri createOutputInTree(Context c, Uri treeUri, String displayName) throws Exception {
        if (treeUri == null) throw new FileAccessException("Output folder chưa được chọn");
        validateTreeWritableOrThrow(c, treeUri, "Output folder");
        String safeName = uniqueDisplayNameInTree(c, treeUri, sanitizeOutputName(displayName));
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
            Uri out = DocumentsContract.createDocument(c.getContentResolver(), parent, "text/plain", safeName);
            if (out == null) throw new FileAccessException("Không tạo được output document: " + safeName);
            return out;
        } catch (SecurityException se) {
            throw new FileAccessException("Android không còn quyền ghi output folder. Hãy chọn lại thư mục output.", se);
        }
    }

    public static List<TreeEntry> listFilesInTree(Context c, Uri treeUri, int max) {
        ArrayList<TreeEntry> out = new ArrayList<>();
        if (treeUri == null) return out;
        Cursor cursor = null;
        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);
            String[] projection = new String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED};
            cursor = c.getContentResolver().query(children, projection, null, null, null);
            if (cursor == null) return out;
            int idIdx = cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID);
            int nameIdx = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME);
            int mimeIdx = cursor.getColumnIndex(Document.COLUMN_MIME_TYPE);
            int sizeIdx = cursor.getColumnIndex(Document.COLUMN_SIZE);
            int modIdx = cursor.getColumnIndex(Document.COLUMN_LAST_MODIFIED);
            while (cursor.moveToNext()) {
                String id = idIdx >= 0 ? cursor.getString(idIdx) : null;
                String name = nameIdx >= 0 ? cursor.getString(nameIdx) : "";
                String mime = mimeIdx >= 0 ? cursor.getString(mimeIdx) : "";
                long size = sizeIdx >= 0 ? cursor.getLong(sizeIdx) : 0;
                long modified = modIdx >= 0 ? cursor.getLong(modIdx) : 0;
                if (id == null) continue;
                if (Document.MIME_TYPE_DIR.equals(mime)) continue;
                if (name != null && !name.toLowerCase(Locale.ROOT).endsWith(".txt") && !"text/plain".equals(mime)) continue;
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                out.add(new TreeEntry(name, mime, size, modified, docUri));
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        Collections.sort(out, new Comparator<TreeEntry>() {
            @Override public int compare(TreeEntry a, TreeEntry b) {
                return Long.compare(b.modified, a.modified);
            }
        });
        if (max > 0 && out.size() > max) return new ArrayList<>(out.subList(0, max));
        return out;
    }

    public static String displayName(Context c, Uri uri) {
        if (uri == null) return "";
        try (Cursor cursor = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = cursor.getString(idx);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        } catch (Exception ignored) {}
        String s = uri.getLastPathSegment();
        return s == null ? "file.txt" : s;
    }

    public static String treeName(Uri uri) {
        if (uri == null) return "";
        String s = uri.getLastPathSegment();
        return s == null ? "output folder" : s.replace("tree:", "");
    }

    public static boolean takePersistable(Context c, Uri uri, int grantFlags, boolean needRead, boolean needWrite) {
        if (c == null || uri == null) return false;
        int wanted = 0;
        if (needRead) wanted |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (needWrite) wanted |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        int available = grantFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if ((grantFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0 || (available & wanted) != wanted) {
            LogStore.append(c, "SAF grant is not persistable or is missing required flags for " + uri);
            return false;
        }
        try {
            c.getContentResolver().takePersistableUriPermission(uri, wanted);
            return (!needRead || hasPersistedRead(c, uri)) && (!needWrite || hasPersistedWrite(c, uri));
        } catch (Exception e) {
            LogStore.append(c, "Could not persist SAF grant for " + uri + ": " + safeMessage(e));
            return false;
        }
    }

    public static boolean hasPersistedRead(Context c, Uri uri) { return hasPersisted(c, uri, true); }
    public static boolean hasPersistedWrite(Context c, Uri uri) { return hasPersisted(c, uri, false); }

    private static boolean hasPersisted(Context c, Uri uri, boolean read) {
        if (c == null || uri == null) return false;
        String target = uri.toString();
        for (UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
            if (p == null || p.getUri() == null) continue;
            String granted = p.getUri().toString();
            if (target.equals(granted) || target.startsWith(granted)) {
                if (read && p.isReadPermission()) return true;
                if (!read && p.isWritePermission()) return true;
            }
        }
        return false;
    }

    public static String validateReadable(Context c, Uri uri, String label) {
        if (uri == null) return label + ": chưa chọn file";
        if (!hasPersistedRead(c, uri)) return label + ": chưa có quyền đọc lâu dài. Hãy chọn lại file bằng nút Browse/Import.";
        try (InputStream is = c.getContentResolver().openInputStream(uri)) {
            if (is == null) return label + ": không mở được file";
            return null;
        } catch (Exception e) {
            return label + ": không đọc được file hoặc đã mất quyền. Hãy chọn lại. Chi tiết: " + safeMessage(e);
        }
    }

    public static String validateWritable(Context c, Uri uri, String label) {
        if (uri != null && hasPersistedWrite(c, uri)) return verifyDocumentWritable(c, uri, label);
        if (uri == null) return label + ": chưa chọn output";
        if (!hasPersistedWrite(c, uri)) return label + ": chưa có quyền ghi lâu dài. Hãy chọn lại output bằng Create/choose output TXT.";
        return null;
    }

    public static String validateTreeWritable(Context c, Uri treeUri, String label) {
        if (treeUri != null && hasPersistedWrite(c, treeUri)) return verifyTreeWritable(c, treeUri, label);
        if (treeUri == null) return label + ": chưa chọn output folder";
        if (!hasPersistedWrite(c, treeUri)) return label + ": chưa có quyền ghi lâu dài. Hãy chọn lại output folder.";
        try {
            DocumentsContract.getTreeDocumentId(treeUri);
            return null;
        } catch (Exception e) {
            return label + ": URI folder không hợp lệ. Hãy chọn lại output folder.";
        }
    }

    public static void validateTreeWritableOrThrow(Context c, Uri treeUri, String label) throws FileAccessException {
        String err = validateTreeWritable(c, treeUri, label);
        if (err != null) throw new FileAccessException(err);
    }

    private static String verifyDocumentWritable(Context c, Uri uri, String label) {
        try (ParcelFileDescriptor descriptor = c.getContentResolver().openFileDescriptor(uri, "rw");
             OutputStream stream = c.getContentResolver().openOutputStream(uri, "wa")) {
            if (descriptor == null) return label + ": cannot open a writable file descriptor";
            if (stream == null) return label + ": cannot open a writable output stream";
            stream.write(new byte[0]);
            stream.flush();
            return null;
        } catch (Exception e) {
            return label + ": persisted write grant is no longer usable. Select the output again. Details: " + safeMessage(e);
        }
    }

    private static String verifyTreeWritable(Context c, Uri treeUri, String label) {
        Uri probe = null;
        try {
            String treeId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId);
            probe = DocumentsContract.createDocument(c.getContentResolver(), parent, "application/octet-stream",
                    ".translate-books-write-probe-" + UUID.randomUUID());
            if (probe == null) return label + ": cannot create a write probe in this folder";
            try (OutputStream stream = c.getContentResolver().openOutputStream(probe, "wt")) {
                if (stream == null) return label + ": cannot open the write probe";
                stream.write(0);
                stream.flush();
            }
            return null;
        } catch (Exception e) {
            return label + ": cannot create and write a file in this folder. Select the folder again. Details: " + safeMessage(e);
        } finally {
            if (probe != null) {
                try { DocumentsContract.deleteDocument(c.getContentResolver(), probe); }
                catch (Exception e) { LogStore.append(c, "Could not delete SAF write probe: " + safeMessage(e)); }
            }
        }
    }

    public static String accessBadge(Context c, Uri uri, boolean write) {
        if (uri == null) return "—";
        boolean ok = write ? hasPersistedWrite(c, uri) : hasPersistedRead(c, uri);
        return ok ? "persisted grant; verified at Start" : "permission missing or temporary";
    }

    public static String sanitizeOutputName(String raw) {
        String s = raw == null || raw.trim().isEmpty() ? "translated.txt" : raw.trim();
        s = s.replace('\u0000', '_')
                .replace('/', '_')
                .replace('\\', '_')
                .replace(':', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace('"', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('|', '_');
        while (s.contains("  ")) s = s.replace("  ", " ");
        s = s.trim();
        if (s.isEmpty()) s = "translated.txt";
        if (!s.toLowerCase(Locale.ROOT).endsWith(".txt")) s += ".txt";
        if (s.length() > 180) {
            int dot = s.toLowerCase(Locale.ROOT).lastIndexOf(".txt");
            s = s.substring(0, Math.min(176, dot > 0 ? dot : s.length())) + ".txt";
        }
        return s;
    }

    public static String uniqueDisplayNameInTree(Context c, Uri treeUri, String wanted) {
        String safe = sanitizeOutputName(wanted);
        Set<String> existing = namesInTree(c, treeUri);
        if (!existing.contains(safe.toLowerCase(Locale.ROOT))) return safe;
        int dot = safe.toLowerCase(Locale.ROOT).lastIndexOf(".txt");
        String base = dot > 0 ? safe.substring(0, dot) : safe;
        String ext = dot > 0 ? safe.substring(dot) : ".txt";
        for (int i = 2; i < 10000; i++) {
            String candidate = base + "_v" + i + ext;
            if (!existing.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
        }
        return base + " (" + System.currentTimeMillis() + ")" + ext;
    }

    private static Set<String> namesInTree(Context c, Uri treeUri) {
        HashSet<String> names = new HashSet<>();
        Cursor cursor = null;
        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);
            cursor = c.getContentResolver().query(children, new String[]{Document.COLUMN_DISPLAY_NAME}, null, null, null);
            if (cursor != null) {
                int idx = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    String name = idx >= 0 ? cursor.getString(idx) : null;
                    if (name != null) names.add(name.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return names;
    }

    private static String safeMessage(Throwable t) {
        String m = t == null ? "" : t.getMessage();
        return m == null || m.trim().isEmpty() ? t.getClass().getSimpleName() : m;
    }

    public static TextStats textStats(String text) {
        String raw = text == null ? "" : text;
        int chars = raw.length();
        int lines = raw.isEmpty() ? 0 : raw.split("\r?\n", -1).length;
        int words = countWords(raw);
        int tokens = Chunker.approxTokens(raw);
        return new TextStats(chars, words, lines, tokens);
    }

    public static String textStatsSummary(String text) {
        return textStats(text).summary();
    }

    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        int count = 0;
        boolean inLatinWord = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) && !isCjkOrKana(ch)) {
                if (!inLatinWord) {
                    count++;
                    inLatinWord = true;
                }
            } else {
                inLatinWord = false;
                if (isCjkOrKana(ch)) count++;
            }
        }
        return count;
    }

    private static boolean isCjkOrKana(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || b == Character.UnicodeBlock.HIRAGANA
                || b == Character.UnicodeBlock.KATAKANA
                || (b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS && Character.isLetterOrDigit(c))
                || b == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }

    private static String fmt(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }
}
