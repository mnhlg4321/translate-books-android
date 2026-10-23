package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class LibraryRawOwnershipModelTest {
    @Test public void sharedRawIsKeptWhenOneSetRemains() {
        Map<String, List<String>> sets = new LinkedHashMap<>();
        sets.put("old", Arrays.asList("raw-a", "raw-shared"));
        sets.put("keep", Arrays.asList("raw-shared", "raw-b"));
        assertEquals(Arrays.asList("raw-a"), LibraryRawOwnershipModel.rawOnlyIds(sets, Arrays.asList("old"), Arrays.asList("raw-a", "raw-shared", "raw-b")));
    }

    @Test public void rawBecomesOnlyWhenEveryOwningSetIsDeleted() {
        Map<String, List<String>> sets = new LinkedHashMap<>();
        sets.put("one", Arrays.asList("raw-a"));
        sets.put("two", Arrays.asList("raw-a", "raw-b"));
        assertEquals(Arrays.asList("raw-a", "raw-b"), LibraryRawOwnershipModel.rawOnlyIds(sets, Arrays.asList("one", "two"), Arrays.asList("raw-a", "raw-b")));
    }
}
