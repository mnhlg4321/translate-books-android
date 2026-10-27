package com.ml.tblandroidtxt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** RAW chapter text as {@code <dir>/<id>.txt}, written through a temporary file and renamed so a crash never leaves half a chapter. */
final class RawFileStore {
    private final File dir;

    RawFileStore(File dir) { this.dir = dir; }

    File fileFor(String id) throws IOException {
        if (id == null || !id.matches("[A-Za-z0-9-]{1,64}")) throw new IOException("RAW id không hợp lệ");
        return new File(dir, id + ".txt");
    }

    void write(String id, String text) throws IOException {
        if (text == null || text.isEmpty()) throw new IOException("Từ chối ghi RAW rỗng");
        AtomicFileCommit.write(fileFor(id).toPath(), text);
        // The recovery copy AtomicFileCommit keeps next to an overwritten file is not needed for RAW.
        File recovery = new File(dir, id + ".txt.recovery");
        if (recovery.exists() && !recovery.delete()) recovery.deleteOnExit();
    }

    boolean exists(String id) {
        try { return fileFor(id).isFile(); } catch (IOException e) { return false; }
    }

    byte[] readBytes(String id) throws IOException {
        return Files.readAllBytes(fileFor(id).toPath());
    }

    String read(String id) throws IOException {
        return new String(readBytes(id), StandardCharsets.UTF_8);
    }

    boolean delete(String id) {
        try {
            File f = fileFor(id);
            return !f.exists() || f.delete();
        } catch (IOException e) {
            return false;
        }
    }

    /** Removes {@code *.pending} leftovers and files whose id is not in {@code keepIds}; returns the number removed. */
    int purgeExcept(java.util.Set<String> keepIds) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int removed = 0;
        for (File f : files) {
            String name = f.getName();
            boolean orphan;
            if (name.endsWith(".txt")) orphan = !keepIds.contains(name.substring(0, name.length() - 4));
            else orphan = name.endsWith(".pending") || name.endsWith(".recovery") || name.contains(".txt");
            if (orphan && f.delete()) removed++;
        }
        return removed;
    }
}
