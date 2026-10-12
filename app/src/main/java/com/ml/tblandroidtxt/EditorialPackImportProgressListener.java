package com.ml.tblandroidtxt;

/**
 * Optional progress seam for the headless pack importer.  Implementations must
 * not mutate importer state; failures from a listener are ignored by the
 * importer so observability can never turn a valid import into a partial one.
 */
@FunctionalInterface
public interface EditorialPackImportProgressListener {
    void onState(EditorialPackImportState state);
}
