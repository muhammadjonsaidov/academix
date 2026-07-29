package uz.academixai.interfaces.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;


@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
    return ResponseEntity.status(e.getStatus())
        .body(
            new ApiErrorResponse(
                e.getStatus().value(), e.getCode(), e.getMessage(), e.getMitigation()));
  }

  // Without this, Spring Security's AccessDeniedException (thrown by @PreAuthorize denials, e.g.
  // a STUDENT token hitting a hasRole('TEACHER') endpoint) falls through to the generic
  // Exception.class handler below and comes back as a 500 — confirmed by a real request. A wrong
  // role should read as "forbidden," not "server broke."
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            new ApiErrorResponse(
                403,
                "ERR_ACCESS_DENIED",
                "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                "Ruxsat chegarasini tekshiring."));
  }

  // Without this, a multipart upload over spring.servlet.multipart.max-file-size/
  // max-request-size never reaches any controller — it's rejected at the DispatcherServlet
  // level and, before this handler existed, fell through to the generic Exception.class
  // handler below as a raw 500 ERR_INTERNAL (confirmed by a real 11.5MB photo upload
  // against the default 10MB request cap). A too-large file is a client mistake, not a
  // server failure — same ERR_INVALID_FILE code already used for other upload validation
  // (SyllabusService, StudentSubmissionService, ExamSubmissionService).
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ApiErrorResponse(
                400,
                "ERR_INVALID_FILE",
                "Fayl hajmi juda katta.",
                "Faylni siqib yoki kichikroq o'lchamda qayta yuklang (maksimal 20MB)."));
  }

  // Without this, a GET typed into a browser's address bar against a POST-only endpoint (e.g.
  // /api/v1/auth/login) fell through to the generic handler below as a logged-with-stack-trace
  // 500 ERR_INTERNAL — confirmed by a real production log. Wrong HTTP method is a client
  // mistake, not a server failure; same category as the AccessDeniedException handler above.
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(
            new ApiErrorResponse(
                405,
                "ERR_METHOD_NOT_ALLOWED",
                "Bu manzil uchun so'rov turi noto'g'ri.",
                "API hujjatlaridagi to'g'ri HTTP metodini ishlating."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e) {
    // This handler was swallowing the real stack trace for every unexpected 500 — confirmed by a
    // real request where the only way to find the actual cause was to add this log line.
    log.error("Unexpected exception", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ApiErrorResponse(
                500,
                "ERR_INTERNAL",
                "Kutilmagan xatolik yuz berdi.",
                "Iltimos qayta urinib ko'ring yoki qo'llab-quvvatlash xizmatiga murojaat qiling."));
  }
}
