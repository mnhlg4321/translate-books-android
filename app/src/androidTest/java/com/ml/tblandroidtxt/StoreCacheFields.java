package com.ml.tblandroidtxt;

import java.lang.reflect.Field;

/**
 * The Glossary and Pronoun caches live in GlossaryStoreCore and PronounStoreCore since the store logic moved to the shared core; the phone classes
 * are thin subclasses. Instrumented tests that reset those caches by reflection look for the field up the class chain.
 */
final class StoreCacheFields {
    private StoreCacheFields() { }

    static Field find(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException next) {
                // keep looking in the superclass
            }
        }
        throw new NoSuchFieldException(name + " in " + type.getName() + " or its superclasses");
    }
}
