package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** v4.23 J4: a fixed pixel height asked of a text button becomes wrap content; match/wrap/weight values pass through. */
public class V423WrapButtonTest {
    @Test public void fixedHeightsBecomeWrapContent() {
        for (int h : new int[]{36, 40, 44, 48, 96, 1}) assertEquals(-2, WrapButton.heightFor(h));
        assertEquals(-1, WrapButton.heightFor(-1));
        assertEquals(-2, WrapButton.heightFor(-2));
        assertEquals(0, WrapButton.heightFor(0));
        assertEquals(44, WrapButton.MIN_HEIGHT_DP);
    }
}
