package com.abhishri.escape.dto;

import com.abhishri.escape.exception.ApiErrorCode;

public class ErrorResponseDTO {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private ApiErrorCode errorCode;

    public ErrorResponseDTO() {}

    public ErrorResponseDTO(String timestamp, int status, String error,
                            String message, String path, ApiErrorCode errorCode) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errorCode = errorCode;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public ApiErrorCode getErrorCode() { return errorCode; }
    public void setErrorCode(ApiErrorCode errorCode) { this.errorCode = errorCode; }
}
