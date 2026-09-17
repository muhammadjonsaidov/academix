package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** academix_tz.md §2.2 — { teacherId, classId, subjectId } */
public record CreateAssignmentRequest(
    @NotNull UUID teacherId, @NotNull UUID classId, @NotNull UUID subjectId) {}
