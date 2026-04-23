package com.tabularhub.api;

import com.tabularhub.engine.TvcsException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(TvcsException.class)
  public ResponseEntity<Map<String, Object>> tvcs(TvcsException ex) {
    String msg = ex.getMessage() == null ? "" : ex.getMessage();
    String detail = ex.getStderr() == null ? "" : ex.getStderr();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "tvcs_error");
    body.put("message", msg);
    body.put("exitCode", ex.getExitCode());
    body.put("detail", detail);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
  }
}
