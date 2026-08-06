package com.ml.tblandroidtxt;

/** Stable coordinator stage for audit and recovery projections. */
public enum EditorialRunContextCoordinatorStage {
    COMMAND_VALIDATION,
    CLOSURE_EVENT_RESOLUTION,
    CLOSED_CONTEXT,
    LINEAGE_VALIDATION,
    LINEAGE_PERSISTENCE,
    READBACK,
    STATUS
}
