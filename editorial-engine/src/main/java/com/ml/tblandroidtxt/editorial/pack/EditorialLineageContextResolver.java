package com.ml.tblandroidtxt.editorial.pack;

/** Read-only port from caller selections to authoritative lineage facts. */
@FunctionalInterface
public interface EditorialLineageContextResolver {
    EditorialLineageAuthoritativeContext resolve(EditorialLineageCallerSelection selection);
}
