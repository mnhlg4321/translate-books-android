package com.ml.tblandroidtxt;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** DocumentIO over plain files. A ref is a file: URI string; text is read like the phone does and written as UTF-8 without BOM. */
public final class PathDocumentIO implements DocumentIO {
    private final Path outputDirectory;
    private final DesktopRawStore raws;

    /** @param outputDirectory where resolveOutput puts files for a ref without a name; may be null when refs are always full files */
    public PathDocumentIO(Path outputDirectory) {
        this(outputDirectory, null);
    }

    /** @param raws the PC RAW library that answers {@code tblraw://<id>} refs; may be null when no such ref is used */
    public PathDocumentIO(Path outputDirectory, DesktopRawStore raws) {
        this.outputDirectory = outputDirectory;
        this.raws = raws;
    }

    public static String refOf(Path file) { return file.toAbsolutePath().toUri().toString(); }

    public static Path pathOf(String ref) {
        if (ref == null || ref.isEmpty()) throw new IllegalArgumentException("Empty document reference");
        return ref.startsWith("file:") ? Paths.get(URI.create(ref)) : Paths.get(ref);
    }

    @Override public String readText(String ref) throws IOException {
        if (RawUri.isRaw(ref)) {
            if (raws == null) throw new IOException("Thư viện RAW không có sẵn");
            return raws.text(RawUri.idOf(ref));
        }
        try {
            return TextDecoding.decode(Files.readAllBytes(pathOf(ref))).text;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override public String displayName(String ref) {
        if (RawUri.isRaw(ref)) {
            DesktopRawStore.Item item = raws == null ? null : raws.find(RawUri.idOf(ref));
            return item == null ? RawUri.idOf(ref) : item.originalName;
        }
        return pathOf(ref).getFileName().toString();
    }

    @Override public void writeText(String ref, String text) throws IOException {
        Path file = pathOf(ref);
        if (file.toAbsolutePath().getParent() != null) Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, (text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
    }

    @Override public void writeTextVerified(String ref, String text) throws IOException {
        if (text == null || text.isEmpty()) throw new IOException("Refusing to replace output with an empty file");
        writeText(ref, text);
        String expected = HashUtil.sha256(text);
        if (!expected.equals(HashUtil.sha256(new String(Files.readAllBytes(pathOf(ref)), StandardCharsets.UTF_8)))) {
            throw new IOException("Output verification failed; checkpoint remains authoritative");
        }
    }

    /** The output chosen by the person is already a concrete file on the PC; a bare name goes into the output folder. */
    @Override public String resolveOutput(long jobId, String ref) {
        if (ref != null && ref.startsWith("file:")) return ref;
        if (outputDirectory == null) throw new IllegalArgumentException("Output destination is missing");
        return refOf(outputDirectory.resolve(OutputNames.sanitizeOutputName(ref)));
    }
}
