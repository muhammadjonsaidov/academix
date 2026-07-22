package uz.academixai.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetLogRepository extends JpaRepository<PasswordResetLogEntity, UUID> {}
