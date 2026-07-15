package uz.academixai.infrastructure.security;

import java.util.UUID;
import uz.academixai.domain.Role;

/** Authentication principal carrying what @PreAuthorize and the RLS interceptor need. */
public record AcademixPrincipal(UUID userId, Role role, UUID schoolId) {}
