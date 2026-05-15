package com.jjs.patentapi.exception;

public class PatentNotFoundException extends RuntimeException {

    public PatentNotFoundException(String id) {
        super("Patent not found: " + id);
    }
}
