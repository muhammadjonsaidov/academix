package uz.academixai.interfaces.web.admin;

import java.time.LocalDate;
import java.util.UUID;

/** academix_tz.md §2.2 — { firstName, lastName, phone, classId, studentNumber, birthDate } */
public record CreateStudentRequest(
    String firstName,
    String lastName,
    String phone,
    UUID classId,
    String studentNumber,
    LocalDate birthDate) {}
