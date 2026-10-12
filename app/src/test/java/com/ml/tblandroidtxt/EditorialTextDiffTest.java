package com.ml.tblandroidtxt;import org.junit.Test;import static org.junit.Assert.*;
public class EditorialTextDiffTest {@Test public void showsChangedAndAddedLines(){String d=EditorialTextDiff.render("a\nb","a\nc\nd",20);assertTrue(d.contains("- b"));assertTrue(d.contains("+ c"));assertTrue(d.contains("+ d"));assertTrue(d.contains("Tổng dòng thay đổi: 2"));}}
