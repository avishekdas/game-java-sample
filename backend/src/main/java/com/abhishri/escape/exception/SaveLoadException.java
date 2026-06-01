package com.abhishri.escape.exception;

public class SaveLoadException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public SaveLoadException(String message, ApiErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SaveLoadException(String message, ApiErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() { return errorCode; }
}
