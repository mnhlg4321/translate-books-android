package com.ml.tblandroidtxt;

import org.junit.Test;

import static org.junit.Assert.*;

public class EditorialPersistenceSpecTest {
    @Test public void additiveMigrationCreatesEveryEditorialStoreWithoutDestruction() {
        String sql = String.join("\n", EditorialMigrationSpec.from10To11()).toLowerCase();
        assertTrue(sql.contains("editorial_projects"));
        assertTrue(sql.contains("editorial_chapters"));
        assertTrue(sql.contains("editorial_assets"));
        assertTrue(sql.contains("editorial_runs"));
        assertTrue(sql.contains("editorial_scenes"));
        assertTrue(sql.contains("editorial_gates"));
        assertTrue(sql.contains("editorial_evidence"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
    }

    @Test public void version12AddsProjectOwnedEditorialReferencesWithoutDestruction() {
        String sql=String.join("\n",EditorialMigrationSpec.from11To12()).toLowerCase();
        assertTrue(sql.contains("editorial_project_assets"));
        assertTrue(sql.contains("project_id"));
        assertTrue(sql.contains("unique index"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("delete from"));
    }
}
