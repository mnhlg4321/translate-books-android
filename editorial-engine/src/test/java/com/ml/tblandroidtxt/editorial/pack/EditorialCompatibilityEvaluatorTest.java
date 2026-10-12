package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorialCompatibilityEvaluatorTest {
    private final EditorialCompatibilityEvaluator evaluator = new EditorialCompatibilityEvaluator();

    @Test public void exactContractAndCapabilitiesAreDataCompatible() {
        EditorialPackManifest manifest = EditorialPackFixtures.valid().manifest();
        EditorialEngineProfile profile = exactProfile(manifest, Set.of("context.test.v1", "ledger.test.v1"), List.of());
        EditorialCompatibilityResult result = evaluator.evaluate(manifest, profile);
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.classification());
        assertFalse(result.blocked());
        assertTrue(result.usableWithoutApk());
    }

    @Test public void missingCapabilityBlocksWithoutGuessing() {
        EditorialPackManifest manifest = EditorialPackFixtures.valid().manifest();
        EditorialCompatibilityResult result = evaluator.evaluate(manifest, exactProfile(manifest, Set.of("context.test.v1"), List.of()));
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, result.classification());
        assertEquals(EditorialPackCompatibilityClass.DATA_COMPATIBLE, result.requiredClass());
        assertEquals(EditorialPackValidationCode.MISSING_CAPABILITY, result.primaryCode());
        assertTrue(result.missingCapabilities().contains("ledger.test.v1"));
    }

    @Test public void adapterRequiredIsUsableOnlyWhenTrustedAdapterIsInstalled() {
        EditorialPackFixtures.Fixture base = EditorialPackFixtures.valid();
        EditorialPackManifest changed = EditorialPackFixtures.phaseChanged("ADAPTER_REQUIRED").manifest();
        EditorialEngineProfile.AdapterSupport adapter = new EditorialEngineProfile.AdapterSupport(
                "adapter.test.v2", changed.contractVersion(), changed.schemaVersion(), changed.machineContractFingerprint(), Set.of(), true);
        EditorialCompatibilityResult ready = evaluator.evaluate(changed, profileFor(changed, base.manifest().machineContractFingerprint(), adapter, true));
        assertEquals(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, ready.classification());
        assertFalse(ready.blocked());

        EditorialCompatibilityResult blocked = evaluator.evaluate(changed, profileFor(changed, base.manifest().machineContractFingerprint(), adapter, false));
        assertEquals(EditorialPackCompatibilityClass.ADAPTER_REQUIRED, blocked.classification());
        assertTrue(blocked.blocked());
        assertEquals(EditorialPackValidationCode.ADAPTER_REQUIRED, blocked.primaryCode());
    }

    @Test public void unsupportedMachineContractRequiresEngineUpgrade() {
        EditorialPackManifest changed = EditorialPackFixtures.phaseChanged("ENGINE_UPGRADE_REQUIRED").manifest();
        EditorialEngineProfile profile = new EditorialEngineProfile("1.0.0", Set.of("context.test.v1", "ledger.test.v1"),
                List.of(new EditorialEngineProfile.ContractSupport(changed.contractVersion(), changed.schemaVersion(), "different-fingerprint", Set.of())), List.of());
        EditorialCompatibilityResult result = evaluator.evaluate(changed, profile);
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, result.classification());
        assertTrue(result.blocked());
        assertEquals(EditorialPackValidationCode.ENGINE_UPGRADE_REQUIRED, result.primaryCode());
    }

    @Test public void unknownContractAndTooNewEngineAreBlockedFailClosed() {
        EditorialPackManifest manifest = EditorialPackFixtures.valid().manifest();
        EditorialEngineProfile unknown = new EditorialEngineProfile("1.0.0", Set.of(), List.of(), List.of());
        assertEquals(EditorialPackCompatibilityClass.BLOCKED, evaluator.evaluate(manifest, unknown).classification());

        EditorialEngineProfile old = new EditorialEngineProfile("0.9.0", Set.of("context.test.v1", "ledger.test.v1"),
                List.of(new EditorialEngineProfile.ContractSupport(manifest.contractVersion(), manifest.schemaVersion(), manifest.machineContractFingerprint(), Set.of())), List.of());
        EditorialCompatibilityResult tooNew = evaluator.evaluate(manifest, old);
        assertEquals(EditorialPackCompatibilityClass.ENGINE_UPGRADE_REQUIRED, tooNew.classification());
        assertEquals(EditorialPackValidationCode.MINIMUM_ENGINE_TOO_NEW, tooNew.primaryCode());
    }

    @Test public void dishonestDeclaredClassIsInvalid() {
        EditorialPackManifest manifest = EditorialPackFixtures.create("com.example.editorial.safe4", "5.0.4", "prompt\n", "ENGINE_UPGRADE_REQUIRED").manifest();
        EditorialCompatibilityResult result = evaluator.evaluate(manifest, exactProfile(manifest, Set.of("context.test.v1", "ledger.test.v1"), List.of()));
        assertEquals(EditorialPackCompatibilityClass.INVALID, result.classification());
        assertEquals(EditorialPackValidationCode.DECLARED_CLASS_MISMATCH, result.primaryCode());
    }

    private static EditorialEngineProfile exactProfile(EditorialPackManifest manifest, Set<String> capabilities, List<EditorialEngineProfile.AdapterSupport> adapters) {
        return new EditorialEngineProfile("1.0.0", capabilities,
                List.of(new EditorialEngineProfile.ContractSupport(manifest.contractVersion(), manifest.schemaVersion(), manifest.machineContractFingerprint(), Set.of())), adapters);
    }

    private static EditorialEngineProfile profileFor(EditorialPackManifest manifest, String ignored, EditorialEngineProfile.AdapterSupport adapter, boolean installed) {
        EditorialEngineProfile.AdapterSupport actual = new EditorialEngineProfile.AdapterSupport(adapter.adapterId(), adapter.sourceContractVersion(), adapter.sourceSchemaVersion(), adapter.sourceMachineContractFingerprint(), adapter.requiredCapabilities(), installed);
        return new EditorialEngineProfile("1.0.0", Set.of("context.test.v1", "ledger.test.v1"),
                List.of(new EditorialEngineProfile.ContractSupport(manifest.contractVersion(), manifest.schemaVersion(), "base-fingerprint", Set.of())), List.of(actual));
    }
}
