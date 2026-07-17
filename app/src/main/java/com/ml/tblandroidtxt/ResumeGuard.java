package com.ml.tblandroidtxt;
public final class ResumeGuard {public static String verifyInput(String expectedHash,String currentText){if(expectedHash==null||expectedHash.isEmpty())return "UNKNOWN_OR_MIGRATED_INPUT_HASH";return expectedHash.equals(HashUtil.sha256(currentText))?"OK":"INPUT_HASH_MISMATCH";}private ResumeGuard(){}}
