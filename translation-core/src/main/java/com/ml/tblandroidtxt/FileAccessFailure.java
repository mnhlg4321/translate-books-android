package com.ml.tblandroidtxt;

/** Failure to read or write a user-selected file; FileUtil.FileAccessException extends it so core code can recognise it. */
public class FileAccessFailure extends Exception {
    FileAccessFailure(String message) { super(message); }
    FileAccessFailure(String message, Throwable cause) { super(message, cause); }
}
