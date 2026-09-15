package uz.academixai.infrastructure.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Database roles the tenancy mechanism relies on.
 *
 * <p>{@code systemRole} is created by migration V45 with {@code BYPASSRLS} and no login: a process
 * cannot start as it, it can only {@code SET LOCAL ROLE} into it for one transaction. That is what
 * keeps {@link uz.academixai.shared.tenancy.TenantScope#runAsSystem} impossible to reach by
 * accident.
 */
@ConfigurationProperties(prefix = "academix.db")
public record TenantScopeProperties(String systemRole) {

  public TenantScopeProperties {
    systemRole = systemRole == null || systemRole.isBlank() ? "academix_system" : systemRole;
  }
}
