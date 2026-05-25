package com.iris.back.framework.web;

import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.ApiResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, WebRequest request) {
    log.warn("API exception code={} method={} path={} message={}",
        ex.getCode(), method(request), path(request), ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      WebRequest request
  ) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining("; "));
    log.warn("API exception code={} method={} path={} message={}",
        "VALIDATION_ERROR", method(request), path(request), message);
    return ResponseEntity.badRequest().body(ApiResponse.failure("VALIDATION_ERROR", message));
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResponse<Void>> handleBind(BindException ex, WebRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining("; "));
    log.warn("API exception code={} method={} path={} message={}",
        "BIND_ERROR", method(request), path(request), message);
    return ResponseEntity.badRequest().body(ApiResponse.failure("BIND_ERROR", message));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex, WebRequest request) {
    log.warn("API exception code={} method={} path={} message={}",
        "UNAUTHORIZED", method(request), path(request), ex.getMessage());
    return ResponseEntity.status(401).body(ApiResponse.failure("UNAUTHORIZED", ex.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    log.warn("API exception code={} method={} path={} message={}",
        "FORBIDDEN", method(request), path(request), ex.getMessage());
    return ResponseEntity.status(403).body(ApiResponse.failure("FORBIDDEN", ex.getMessage()));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      WebRequest request
  ) {
    log.warn("API exception code={} method={} path={} message={}",
        "METHOD_NOT_ALLOWED", method(request), path(request), ex.getMessage());
    return ResponseEntity.status(405).body(ApiResponse.failure("METHOD_NOT_ALLOWED", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, WebRequest request) {
    log.error("API exception code={} method={} path={} message={}",
        "INTERNAL_ERROR", method(request), path(request), ex.getMessage(), ex);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.failure("INTERNAL_ERROR", ex.getMessage()));
  }

  private String method(WebRequest request) {
    return request instanceof ServletWebRequest servletWebRequest
        ? servletWebRequest.getRequest().getMethod()
        : "-";
  }

  private String path(WebRequest request) {
    return request instanceof ServletWebRequest servletWebRequest
        ? servletWebRequest.getRequest().getRequestURI()
        : "-";
  }
}
