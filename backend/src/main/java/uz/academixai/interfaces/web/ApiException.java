package uz.academixai.interfaces.web;

import org.springframework.http.HttpStatus;

/**
 * Custom status/internal-code/message/mitigation error shape (academix_backend_tdd.md) — not RFC
 * 7807 Problem Detail.
 */
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String code;
  private final String mitigation;

  public ApiException(HttpStatus status, String code, String message, String mitigation) {
    super(message);
    this.status = status;
    this.code = code;
    this.mitigation = mitigation;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMitigation() {
    return mitigation;
  }
}
