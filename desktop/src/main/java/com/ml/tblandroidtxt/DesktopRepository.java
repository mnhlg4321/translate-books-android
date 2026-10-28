package com.ml.tblandroidtxt;

/** The shared repository over the PC database (the core constructor is protected, hence this subclass in the core package). */
public final class DesktopRepository extends TranslationRepositoryCore {
    public DesktopRepository(SqlDb db) { super(db); }
}
