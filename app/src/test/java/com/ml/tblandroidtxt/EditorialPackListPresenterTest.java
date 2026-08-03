package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackCompatibilityClass;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistry;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistryMetadata;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class EditorialPackListPresenterTest {
    @Test public void emptyRegistryIsExplicitEmptyState() {
        EditorialPackListPresenter.Result result = new EditorialPackListPresenter(registry(List.of())).load();
        assertEquals(EditorialPackListPresenter.State.EMPTY, result.state());
        assertTrue(result.packs().isEmpty());
    }

    @Test public void readyDataCompatibleStillSaysAwaitingCertification() {
        EditorialPackUiModel model = new EditorialPackListPresenter(registry(List.of(
                EditorialPackUiTestFixtures.pack("pack", "1.0.0", EditorialPackCompatibilityClass.DATA_COMPATIBLE,
                        EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION, Set.of(), "", EditorialPackRegistryMetadata.IntegrityState.VALID)))).load().packs().get(0);
        assertEquals("Đã lưu • chờ chứng nhận", model.storageLabel());
        assertEquals("Tương thích contract • chưa chứng nhận", model.compatibilityLabel());
        assertFalse(model.storageLabel().contains("READY"));
        assertTrue(model.readOnly()); assertTrue(model.mutationActions().isEmpty());
    }

    @Test public void blockedClassesAndMissingCapabilitiesAreVisible() {
        EditorialPackUiModel adapter = presenterFor(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, "missing adapter").packs().get(0);
        assertEquals("Cần adapter • không thể kích hoạt", adapter.compatibilityLabel());
        assertEquals("Đã lưu • bị khóa", adapter.storageLabel());
        assertEquals(Set.of("cap.adapter"), adapter.missingCapabilities());
        EditorialPackUiModel engine = presenterFor(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, "minimum engine").packs().get(0);
        assertEquals("Cần nâng engine/APK • không thể kích hoạt", engine.compatibilityLabel());
        EditorialPackUiModel invalid = presenterFor(EditorialPackCompatibilityClass.INVALID, "invalid").packs().get(0);
        assertEquals("Không hợp lệ", invalid.compatibilityLabel());
        assertTrue(invalid.blockedReason().contains("invalid"));
    }

    @Test public void versionsSortDescendingWithinPackAndHashBreaksTies() {
        EditorialPackManifest one = EditorialPackUiTestFixtures.pack("same", "1.0.0", EditorialPackCompatibilityClass.DATA_COMPATIBLE, EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION, Set.of(), "", EditorialPackRegistryMetadata.IntegrityState.VALID);
        EditorialPackManifest two = EditorialPackUiTestFixtures.pack("same", "2.0.0", EditorialPackCompatibilityClass.DATA_COMPATIBLE, EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION, Set.of(), "", EditorialPackRegistryMetadata.IntegrityState.VALID);
        EditorialPackManifest other = EditorialPackUiTestFixtures.pack("other", "9.0.0", EditorialPackCompatibilityClass.DATA_COMPATIBLE, EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION, Set.of(), "", EditorialPackRegistryMetadata.IntegrityState.VALID);
        List<EditorialPackUiModel> packs = new EditorialPackListPresenter(registry(List.of(one, two, other))).load().packs();
        assertEquals("other", packs.get(0).packId());
        assertEquals("2.0.0", packs.get(1).version());
        assertEquals("1.0.0", packs.get(2).version());
    }

    @Test public void hashShorteningAndFilesAreExactAndCandidateNotInRegistry() {
        EditorialPackManifest manifest = EditorialPackUiTestFixtures.pack("pack", "1.0.0", EditorialPackCompatibilityClass.DATA_COMPATIBLE, EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION, Set.of(), "", EditorialPackRegistryMetadata.IntegrityState.VALID);
        EditorialPackUiModel model = new EditorialPackListPresenter(registry(List.of(manifest))).load().packs().get(0);
        assertEquals(manifest.canonicalPackHash().substring(0, 12) + "…", model.shortHash());
        assertEquals(3, model.files().size());
        assertFalse(model.canonicalHash().equals("dbe214d842d98fd76afd2e700747fcc2cf3d5b6038134b38ea8f220fed3bf273"));
    }

    @Test public void registryFailureBecomesErrorWithoutCrash() {
        EditorialPackListPresenter.Result result = new EditorialPackListPresenter(new EditorialPackRegistry() {
            public Optional<EditorialPackManifest> findByHash(String hash) { throw new IllegalStateException("query failed"); }
            public Optional<EditorialPackManifest> findByIdentity(String id, String version) { throw new IllegalStateException("query failed"); }
            public List<EditorialPackManifest> list() { throw new IllegalStateException("query failed"); }
        }).load();
        assertEquals(EditorialPackListPresenter.State.ERROR, result.state());
        assertTrue(result.errorMessage().contains("query failed"));
    }

    @Test public void findQueriesRemainReadOnlyAndUseIdentityOrHash() {
        EditorialPackManifest manifest = EditorialPackUiTestFixtures.pack("pack", "1.0.0", EditorialPackCompatibilityClass.DATA_COMPATIBLE, EditorialPackRegistryMetadata.StorageState.STORED_READY_FOR_CERTIFICATION, Set.of(), "", EditorialPackRegistryMetadata.IntegrityState.VALID);
        EditorialPackListPresenter presenter = new EditorialPackListPresenter(registry(List.of(manifest)));
        assertTrue(presenter.findByHash(manifest.canonicalPackHash()).isPresent());
        assertTrue(presenter.findByIdentity("pack", "1.0.0").isPresent());
    }

    private EditorialPackListPresenter.Result presenterFor(EditorialPackCompatibilityClass classification, String reason) {
        return new EditorialPackListPresenter(registry(List.of(EditorialPackUiTestFixtures.pack("pack", "1.0.0", classification,
                EditorialPackRegistryMetadata.StorageState.STORED_BLOCKED, Set.of("cap.adapter"), reason, EditorialPackRegistryMetadata.IntegrityState.INVALID)))).load();
    }

    private static EditorialPackRegistry registry(List<EditorialPackManifest> manifests) {
        return new EditorialPackRegistry() {
            public Optional<EditorialPackManifest> findByHash(String hash) { return manifests.stream().filter(m -> m.canonicalPackHash().equals(hash)).findFirst(); }
            public Optional<EditorialPackManifest> findByIdentity(String id, String version) { return manifests.stream().filter(m -> m.packId().equals(id) && m.version().equals(version)).findFirst(); }
            public List<EditorialPackManifest> list() { return manifests; }
        };
    }
}
