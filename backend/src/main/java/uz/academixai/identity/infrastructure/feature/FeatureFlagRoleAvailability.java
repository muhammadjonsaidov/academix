package uz.academixai.identity.infrastructure.feature;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.identity.application.port.out.RoleAvailability;
import uz.academixai.shared.feature.PsychologyModule;

/** Maps the {@code academix.features.*} module flags onto the roles they gate. */
@Component
public class FeatureFlagRoleAvailability implements RoleAvailability {

  private final boolean psychologyEnabled;

  public FeatureFlagRoleAvailability(
      @Value(PsychologyModule.ENABLED_VALUE) boolean psychologyEnabled) {
    this.psychologyEnabled = psychologyEnabled;
  }

  @Override
  public boolean isAvailable(Role role) {
    return role != Role.PSYCHOLOGIST || psychologyEnabled;
  }
}
