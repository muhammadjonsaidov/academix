package uz.academixai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.Role;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.persistence.UserRepository;

class AdminDashboardServiceTest {

  @Test
  void dashboardIncludesTheCountsNeededForGuidedSchoolSetup() {
    UUID schoolId = UUID.randomUUID();
    UserRepository users = mock(UserRepository.class);
    StudentProfileRepository students = mock(StudentProfileRepository.class);
    SchoolClassRepository classes = mock(SchoolClassRepository.class);
    SubjectRepository subjects = mock(SubjectRepository.class);
    ClassSubjectTeacherRepository assignments = mock(ClassSubjectTeacherRepository.class);
    HomeworkSubmissionRepository submissions = mock(HomeworkSubmissionRepository.class);
    GradeRepository grades = mock(GradeRepository.class);
    PsychologicalSignalRepository signals = mock(PsychologicalSignalRepository.class);

    when(classes.countBySchoolIdAndIsActiveTrue(schoolId)).thenReturn(2L);
    when(students.countBySchoolIdAndIsActiveTrue(schoolId)).thenReturn(41);
    when(users.countByRoleAndSchoolId(Role.TEACHER, schoolId)).thenReturn(3);
    when(subjects.countBySchoolId(schoolId)).thenReturn(6L);
    when(assignments.countBySchoolId(schoolId)).thenReturn(5L);
    when(submissions.countDistinctStudentsSubmittedSince(
            org.mockito.ArgumentMatchers.eq(schoolId), org.mockito.ArgumentMatchers.any()))
        .thenReturn(0);
    when(grades.classProgress(
            org.mockito.ArgumentMatchers.eq(schoolId),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    when(grades.teacherRanking(schoolId)).thenReturn(List.of());
    when(signals.countUnresolvedBySchoolAndSeverity(schoolId, SignalSeverity.HIGH.name()))
        .thenReturn(0);
    when(signals.countUnresolvedBySchoolAndSeverity(schoolId, SignalSeverity.MEDIUM.name()))
        .thenReturn(0);

    AdminDashboardService.Dashboard dashboard =
        new AdminDashboardService(
                users, students, classes, subjects, assignments, submissions, grades, signals)
            .dashboard(schoolId);

    assertThat(dashboard.totalClasses()).isEqualTo(2);
    assertThat(dashboard.totalTeachers()).isEqualTo(3);
    assertThat(dashboard.totalSubjects()).isEqualTo(6);
    assertThat(dashboard.totalAssignments()).isEqualTo(5);
    assertThat(dashboard.totalStudents()).isEqualTo(41);
  }
}
