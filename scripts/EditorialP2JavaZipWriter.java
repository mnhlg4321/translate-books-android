import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Test/reference-only ZIP writer for P2. It intentionally leaves DEFLATED
 * entry sizes unknown so java.util.zip emits data descriptors.
 */
public final class EditorialP2JavaZipWriter {
    private static final long FIXED_TIMESTAMP_MILLIS = 1_788_368_400_000L;
    private static final String[] ENTRY_NAMES = {
            "editorial-pack.json", "project.txt", "prompt.txt", "workflow.txt"
    };

    private EditorialP2JavaZipWriter() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException("expected manifest, project, prompt, workflow and output paths");
        }
        byte[][] payloads = new byte[4][];
        for (int i = 0; i < 4; i++) payloads[i] = Files.readAllBytes(Path.of(args[i]));
        Path output = Path.of(args[4]);
        if (Files.exists(output)) throw new IllegalStateException("refusing to overwrite " + output);
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (OutputStream raw = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.setLevel(Deflater.DEFAULT_COMPRESSION);
            for (int i = 0; i < ENTRY_NAMES.length; i++) {
                ZipEntry entry = new ZipEntry(ENTRY_NAMES[i]);
                entry.setTime(FIXED_TIMESTAMP_MILLIS);
                zip.putNextEntry(entry);
                zip.write(payloads[i]);
                zip.closeEntry();
            }
        }
    }
}
