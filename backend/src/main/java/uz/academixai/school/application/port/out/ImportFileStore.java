package uz.academixai.school.application.port.out;

import java.util.Optional;

/**
 * Outbound port for the short-lived store holding an uploaded file between the analyze and commit
 * steps.
 *
 * <p>The token is the only handle the client gets back, so expiry is part of the contract: {@link
 * #get} returning empty is what turns a stale wizard into a clear retry rather than a wrong import.
 */
public interface ImportFileStore {

  String save(byte[] fileBytes);

  Optional<byte[]> get(String token);
}
