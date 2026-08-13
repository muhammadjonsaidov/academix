/**
 * Shared client-side password policy — mirrors what the backend SHOULD enforce
 * (pentest F-16: the API accepts "12345678" as-is). This is defense-in-depth
 * only: it improves UX and stops the weakest passwords at the UI, but the real
 * gate must live server-side in AuthService.changePassword /
 * PasswordResetService.resetPassword.
 */
export interface PasswordPolicyResult {
  valid: boolean;
  /** Human-readable Uzbek reason when invalid, empty when valid. */
  error: string;
}

const MIN_LENGTH = 8;

/** At least one lowercase, one uppercase and one digit — the "12345678"-class
 *  passwords fail here, which is exactly the gap the pentest found. */
export function validatePassword(password: string): PasswordPolicyResult {
  if (password.length < MIN_LENGTH) {
    return {
      valid: false,
      error: `Parol kamida ${MIN_LENGTH} belgidan iborat bo'lishi kerak.`,
    };
  }
  if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password)) {
    return {
      valid: false,
      error: "Parolda kamida bitta katta harf, bitta kichik harf va bitta raqam bo'lishi kerak.",
    };
  }
  return { valid: true, error: "" };
}
