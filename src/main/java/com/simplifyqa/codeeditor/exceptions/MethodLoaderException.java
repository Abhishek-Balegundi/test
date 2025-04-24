package com.simplifyqa.codeeditor.exceptions;

public class MethodLoaderException extends RuntimeException{
    public MethodLoaderException() {
    }

    public MethodLoaderException(String message) {
        super(message);
    }

    public MethodLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodLoaderException(Throwable cause) {
        super(cause);
    }
}
