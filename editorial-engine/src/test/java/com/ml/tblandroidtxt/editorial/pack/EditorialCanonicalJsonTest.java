package com.ml.tblandroidtxt.editorial.pack;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EditorialCanonicalJsonTest {
    @Test public void fieldOrderDoesNotChangeCanonicalRepresentation() {
        LinkedHashMap<String, Object> first = new LinkedHashMap<>();
        first.put("z", BigDecimal.valueOf(1)); first.put("a", List.of("x", true));
        LinkedHashMap<String, Object> second = new LinkedHashMap<>();
        second.put("a", List.of("x", true)); second.put("z", BigDecimal.ONE);
        assertEquals(EditorialCanonicalJson.canonicalize(first), EditorialCanonicalJson.canonicalize(second));
    }

    @Test public void duplicateKeysAndBomAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> EditorialCanonicalJson.parse("{\"a\":1,\"a\":2}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> EditorialCanonicalJson.parse(new byte[]{(byte)0xef, (byte)0xbb, (byte)0xbf, '{', '}'}));
    }
}
