package uz.academixai.interfaces.web.teacher;

import java.time.LocalDate;
import java.util.UUID;

public record CreateExamRequest(
    UUID classId, UUID subjectId, String title, LocalDate examDate, int maxScore) {}
