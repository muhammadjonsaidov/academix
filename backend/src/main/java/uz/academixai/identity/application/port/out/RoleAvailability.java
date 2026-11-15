package uz.academixai.identity.application.port.out;

import uz.academixai.domain.Role;

/**
 * Whether accounts of a role may sign in. A role backed by an optional, switched-off module (the
 * psychologist add-on) has nothing to sign in to, so its accounts are turned away at login.
 */
public interface RoleAvailability {

  boolean isAvailable(Role role);
}
