package com.abhishri.escape.exception;

import com.abhishri.escape.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleGameNotFound(
            GameNotFoundException ex, HttpServletRequest req) {
        log.warn("4xx GAME_NOT_FOUND path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ex.getMessage(), req, ApiErrorCode.GAME_NOT_FOUND));
    }

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidMove(
            InvalidMoveException ex, HttpServletRequest req) {
        log.warn("4xx INVALID_MOVE path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ex.getMessage(), req, ApiErrorCode.INVALID_MOVE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        log.warn("400 INVALID_REQUEST path={} msg={}", req.getRequestURI(), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, msg, req, ApiErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponseDTO> handleEverythingElse(
            Throwable ex, HttpServletRequest req) {
        log.error("500 INTERNAL_ERROR path={}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                        req, ApiErrorCode.INTERNAL_ERROR));
    }

    private ErrorResponseDTO error(HttpStatus status, String message,
                                   HttpServletRequest req, ApiErrorCode code) {
        return new ErrorResponseDTO(
                Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(),
                status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), code);
    }
}
