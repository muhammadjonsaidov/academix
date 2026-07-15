package uz.academixai.interfaces.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
    return ResponseEntity.status(e.getStatus())
        .body(
            new ApiErrorResponse(
                e.getStatus().value(), e.getCode(), e.getMessage(), e.getMitigation()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ApiErrorResponse(
                500,
                "ERR_INTERNAL",
                "Kutilmagan xatolik yuz berdi.",
                "Iltimos qayta urinib ko'ring yoki qo'llab-quvvatlash xizmatiga murojaat qiling."));
  }
}
