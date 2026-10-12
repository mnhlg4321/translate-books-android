package com.ml.tblandroidtxt;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EditorialV25MigrationSpecTest {
    private static String sql() {
        return String.join("\n", EditorialMigrationSpec.from24To25()).toLowerCase();
    }

    @Test public void v25AddsExactlyOneTableAndOneIndex() {
        String sql = sql();
        assertEquals(2, EditorialMigrationSpec.from24To25().size());
        assertEquals(1, sql.split("create table if not exists", -1).length - 1);
        assertEquals(1, sql.split("create index if not exists", -1).length - 1);
        assertEquals(0, sql.split("create unique index", -1).length - 1);
        assertEquals(0, sql.split("create trigger", -1).length - 1);
        assertTrue(sql.contains("create table if not exists editorial_phase_artifacts"));
        assertTrue(sql.contains("on editorial_phase_artifacts(chapter_key,phase,predecessor_identity)"));
    }

    @Test public void v25IsAdditiveOnly() {
        String sql = sql();
        assertFalse(sql.contains("alter table"));
        assertFalse(sql.contains("drop "));
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("insert or replace"));
        assertFalse(sql.contains("update "));
    }

    @Test public void v25PhaseAndStatusAreClosedSets() {
        String sql = sql();
        assertTrue(sql.contains("phase text not null check(phase in ('l2_edit','l3_final'))"));
        assertTrue(sql.contains("status text not null check(status in ('claimed','recovery_required','committed'))"));
        assertFalse(sql.contains("'l1_reconcile'"));
    }

    @Test public void v25CommittedRequiresBothBlobsAndOthersRequireNone() {
        String sql = sql();
        assertTrue(sql.contains("status='committed' and text_bytes is not null and evidence_bytes is not null"));
        assertTrue(sql.contains("length(text_sha256)=64 and length(evidence_sha256)=64"));
        assertTrue(sql.contains("status<>'committed' and text_bytes is null and evidence_bytes is null"));
    }

    @Test public void v25IdentitiesAreLowercaseHex64() {
        String sql = sql();
        for (String column : new String[]{"attempt_identity", "predecessor_identity", "bundle_identity"}) {
            assertTrue(column, sql.contains("check(length(" + column + ")=64 and " + column
                    + " not glob '*[^0-9a-f]*')"));
        }
    }

    @Test public void repositoryWiresV25IntoCreateAndUpgrade() throws Exception {
        Path source = Paths.get("src/main/java/com/ml/tblandroidtxt/TranslationRepository.java");
        if (!Files.exists(source)) source = Paths.get("app").resolve(source);
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        assertTrue(text.contains("EditorialMigrationSpec.from24To25()"));
        int onCreate = text.indexOf("onCreate(");
        int onUpgrade = text.indexOf("onUpgrade(");
        assertTrue(onCreate >= 0 && onUpgrade > onCreate);
        assertTrue(text.substring(onCreate, onUpgrade).contains("createEditorialPhaseArtifacts(db)"));
        assertTrue(text.substring(onUpgrade).contains("oldVersion < 25"));
    }
}
