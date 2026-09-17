package uz.academixai.shared.error;

/** academix_backend_tdd.md error-code table shape: status/code/message/mitigation. */
public record ApiErrorResponse(int status, String code, String message, String mitigation) {}
