package uz.academixai.wellbeing.infrastructure.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.domain.ParentStudentLink;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.notification.application.NotificationService;
import uz.academixai.notification.domain.NotificationType;
import uz.academixai.wellbeing.application.port.out.PsychologicalAlertNotifier;

/**
 * Resolves recipients and delegates actual delivery to Notification's published application API.
 */
@Component
public class NotificationPsychologicalAlertNotifier implements PsychologicalAlertNotifier {

  private static final Logger log =
      LoggerFactory.getLogger(NotificationPsychologicalAlertNotifier.class);

  private final ParentChildAccess parentLinks;
  private final StudentProfileRepository students;
  private final SchoolClassRepository classes;
  private final UserRepository users;
  private final NotificationService notifications;

  public NotificationPsychologicalAlertNotifier(
      ParentChildAccess parentLinks,
      StudentProfileRepository students,
      SchoolClassRepository classes,
      UserRepository users,
      NotificationService notifications) {
    this.parentLinks = parentLinks;
    this.students = students;
    this.classes = classes;
    this.users = users;
    this.notifications = notifications;
  }

  @Override
  public boolean notifyParents(UUID studentId, SignalType type, SignalSeverity severity) {
    List<ParentStudentLink> links = parentLinks.parentsOf(studentId);
    for (ParentStudentLink link : links) {
      send(
          link.parentUserId(),
          "Diqqat talab qiluvchi holat",
          "Farzandingizda e'tibor talab qiluvchi holat aniqlandi. Batafsil ma'lumot uchun"
              + " maktab psixologi bilan bog'laning.",
          studentId,
          type,
          severity);
    }
    return !links.isEmpty();
  }

  @Override
  public void notifyTeacherAndPsychologists(
      UUID studentId, SignalType type, SignalSeverity severity) {
    studentSchool(studentId)
        .ifPresentOrElse(
            schoolId -> {
              notifyClassTeacher(studentId, type, severity);
              List<UserEntity> psychologists =
                  users.findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(
                      Role.PSYCHOLOGIST, schoolId);
              if (psychologists.isEmpty()) {
                log.warn(
                    "Psychological signal for student {} has no psychologists to notify.",
                    studentId);
              }
              psychologists.forEach(
                  psychologist ->
                      send(
                          psychologist.getId(),
                          "Psixologik signal",
                          alertText(type, severity),
                          studentId,
                          type,
                          severity));
            },
            () ->
                log.warn(
                    "Psychological signal for student {} has no school to resolve recipients for.",
                    studentId));
  }

  private void notifyClassTeacher(UUID studentId, SignalType type, SignalSeverity severity) {
    students
        .findByUserId(studentId)
        .map(StudentProfileEntity::getClassId)
        .flatMap(classId -> classes.findById(classId).map(SchoolClassEntity::getClassTeacherId))
        .ifPresentOrElse(
            teacherId ->
                send(
                    teacherId,
                    "Psixologik signal",
                    alertText(type, severity),
                    studentId,
                    type,
                    severity),
            () ->
                log.warn(
                    "Psychological signal for student {} has no class teacher to notify"
                        + " (missing classId or classTeacherId).",
                    studentId));
  }

  private Optional<UUID> studentSchool(UUID studentId) {
    return students.findByUserId(studentId).map(StudentProfileEntity::getSchoolId);
  }

  private void send(
      UUID recipientId,
      String title,
      String body,
      UUID studentId,
      SignalType type,
      SignalSeverity severity) {
    notifications.sendNotification(
        recipientId,
        NotificationType.PSYCHOLOGICAL_ALERT,
        title,
        body,
        Map.of(
            "studentId", studentId.toString(), "type", type.name(), "severity", severity.name()));
  }

  private static String alertText(SignalType type, SignalSeverity severity) {
    return "O'quvchida %s (%s) darajali signal aniqlandi.".formatted(type, severity);
  }
}
