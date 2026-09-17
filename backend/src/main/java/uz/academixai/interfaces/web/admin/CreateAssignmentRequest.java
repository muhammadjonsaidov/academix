package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** academix_tz.md §2.2 — { teacherId, classId, subjectId } */
public record CreateAssignmentRequest(
    @NotNull(message = "majburiy maydon") UUID teacherId,
    @NotNull(message = "majburiy maydon") UUID classId,
    @NotNull(message = "majburiy maydon") UUID subjectId) {}
