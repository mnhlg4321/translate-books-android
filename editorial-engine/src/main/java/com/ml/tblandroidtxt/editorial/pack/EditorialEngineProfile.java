package com.ml.tblandroidtxt.editorial.pack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Trusted engine/contract/adapter facts supplied by the installed application. */
public final class EditorialEngineProfile {
    private final String engineVersion;
    private final Set<String> capabilities;
    private final Map<ContractKey, ContractSupport> contracts;
    private final Set<AdapterSupport> adapters;

    public EditorialEngineProfile(String engineVersion, Collection<String> capabilities,
                                  Collection<ContractSupport> contracts, Collection<AdapterSupport> adapters) {
        if (engineVersion == null || engineVersion.isBlank()) throw new IllegalArgumentException("engineVersion is required");
        this.engineVersion = engineVersion;
        this.capabilities = Collections.unmodifiableSet(new HashSet<>(capabilities == null ? Set.of() : capabilities));
        HashMap<ContractKey, ContractSupport> byKey = new HashMap<>();
        if (contracts != null) for (ContractSupport support : contracts) {
            if (byKey.put(new ContractKey(support.contractVersion(), support.schemaVersion()), support) != null) {
                throw new IllegalArgumentException("Duplicate trusted contract descriptor");
            }
        }
        this.contracts = Collections.unmodifiableMap(byKey);
        this.adapters = Collections.unmodifiableSet(new HashSet<>(adapters == null ? Set.of() : adapters));
    }

    public String engineVersion() { return engineVersion; }
    public Set<String> capabilities() { return capabilities; }
    public Map<ContractKey, ContractSupport> contracts() { return contracts; }
    public Set<AdapterSupport> adapters() { return adapters; }

    public record ContractKey(String contractVersion, String schemaVersion) {}

    public record ContractSupport(String contractVersion, String schemaVersion, String machineContractFingerprint,
                                  Set<String> supportedCapabilities) {
        public ContractSupport {
            if (contractVersion == null || schemaVersion == null || machineContractFingerprint == null) throw new IllegalArgumentException("Contract identity is required");
            supportedCapabilities = Collections.unmodifiableSet(new HashSet<>(supportedCapabilities == null ? Set.of() : supportedCapabilities));
        }
    }

    public record AdapterSupport(String adapterId, String sourceContractVersion, String sourceSchemaVersion,
                                 String sourceMachineContractFingerprint, Set<String> requiredCapabilities,
                                 boolean installed) {
        public AdapterSupport {
            if (adapterId == null || adapterId.isBlank() || sourceContractVersion == null || sourceSchemaVersion == null
                    || sourceMachineContractFingerprint == null) throw new IllegalArgumentException("Adapter identity is required");
            requiredCapabilities = Collections.unmodifiableSet(new HashSet<>(requiredCapabilities == null ? Set.of() : requiredCapabilities));
        }
    }
}
