package uz.academixai.school.application;

import java.util.UUID;

/** Read-model row exposed by School's teacher-access query use case. */
public record TeacherStudent(UUID id, String firstName, String lastName, String studentNumber) {}
