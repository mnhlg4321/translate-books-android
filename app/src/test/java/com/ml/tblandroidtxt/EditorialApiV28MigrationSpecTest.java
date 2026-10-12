package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** V28 preserves existing API-run rows while adding V5_CHAT and original source snapshots. */
public final class EditorialApiV28MigrationSpecTest {
    @Test public void v28RebuildCopiesEveryOldFieldAndAddsOnlyTheSourceSnapshot() {
        List<String> statements = EditorialApiV28MigrationSpec.from27To28();
        assertEquals(6, statements.size());
        String sql = String.join("\n", statements).toLowerCase(java.util.Locale.ROOT);
        assertTrue(sql.contains("alter table editorial_api_runs rename to editorial_api_runs_v27"));
        assertTrue(sql.contains("mode in ('quick','thorough','v5_chat')"));
        assertTrue(sql.contains("original_source_files_json text not null default '[]'"));
        assertTrue(sql.contains("select id,combo_id,contract_revision,quality_core_sha256,model,mode,state,raw_sha256,raw_text"));
        assertTrue(sql.contains("pronoun_sha256,pronoun_text,'[]',edited_text,final_text"));
        assertTrue(sql.contains("drop table editorial_api_runs_v27"));
        assertTrue(sql.contains("create index if not exists idx_editorial_api_runs_combo"));
    }
}
