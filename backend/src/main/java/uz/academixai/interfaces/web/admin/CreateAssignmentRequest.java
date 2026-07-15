package uz.academixai.interfaces.web.admin;

import java.util.UUID;

/** academix_tz.md §2.2 — { teacherId, classId, subjectId } */
public record CreateAssignmentRequest(UUID teacherId, UUID classId, UUID subjectId) {}
