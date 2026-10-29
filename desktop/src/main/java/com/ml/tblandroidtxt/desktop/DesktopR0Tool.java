package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopRuntime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * R0 helper, headless, same DesktopRuntime code as the window. Prints counts and key names only: never text and never the key.
 *
 * <pre>
 *   export-library DATA_DIR ZIP        write the library ZIP of a data folder
 *   import DATA_DIR PROFILE.json LIB.zip   preview and apply the settings profile, then preview and apply the library ZIP
 *   counts DATA_DIR                    RAW / Glossary / Pronoun / set counts and the saved key's length and shape
 * </pre>
 */
public final class DesktopR0Tool {
    private DesktopR0Tool() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) { System.out.println("usage: export-library|import|counts ..."); System.exit(64); }
        try (DesktopRuntime rt = DesktopRuntime.open(Paths.get(args[1]), null)) {
            switch (args[0]) {
                case "export-library": {
                    int[] n = rt.exportLibrary(Paths.get(args[2]));
                    System.out.println("exported: glossary=" + n[0] + " pronoun=" + n[1] + " raw=" + n[2]);
                    break;
                }
                case "import": {
                    String json = new String(Files.readAllBytes(Paths.get(args[2])), StandardCharsets.UTF_8);
                    DesktopRuntime.ProfilePreview pp = rt.previewProfile(json);
                    StringBuilder keys = new StringBuilder();
                    for (String[] d : pp.differences) keys.append(keys.length() == 0 ? "" : ", ").append(d[0]);
                    System.out.println("profile differences (" + pp.differences.size() + "): " + keys);
                    if (!pp.differences.isEmpty()) rt.applyProfile(pp);
                    System.out.println("profile applied; undo available=" + rt.canUndoProfile());
                    DesktopRuntime.LibraryPreview lp = rt.previewLibrary(Paths.get(args[3]));
                    System.out.println("library zip: glossary=" + lp.glossariesInFile() + " pronoun=" + lp.pronounsInFile() + " raw=" + lp.rawsInFile()
                            + " | will add glossary=" + lp.addGlossaries() + " pronoun=" + lp.addPronouns() + " raw=" + lp.addRaws()
                            + " | existing=" + lp.existing() + " duplicate=" + lp.duplicate() + " renamed=" + lp.renamed() + " bad=" + lp.bad());
                    boolean ok = lp.addedCount() == 0 || rt.applyLibrary(lp);
                    if (lp.addedCount() == 0) System.out.println("active Glossary/Pronoun adopted from the ZIP: " + rt.adoptActive(lp));
                    System.out.println("library applied=" + ok);
                    counts(rt);
                    break;
                }
                case "counts": counts(rt); break;
                default: System.out.println("unknown command " + args[0]); System.exit(64);
            }
        }
    }

    private static void counts(DesktopRuntime rt) {
        System.out.println("counts: raw=" + rt.raws().list().size() + " glossary=" + rt.glossaries().size() + " pronoun=" + rt.pronouns().size()
                + " sets=" + rt.setCards().size());
        System.out.println("key: length=" + rt.secrets().savedKeyLength() + " openrouterShape=" + rt.secrets().savedKeyHasOpenRouterShape());
    }
}
