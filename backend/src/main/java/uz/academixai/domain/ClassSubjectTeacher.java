package uz.academixai.domain;

import java.util.UUID;

/**
 * academix_tz.md §1.5 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.ClassSubjectTeacherEntity}.
 */
public record ClassSubjectTeacher(
    UUID id, UUID schoolId, UUID classId, UUID subjectId, UUID teacherId, String academicYear) {}
