package uz.academixai.learning.infrastructure.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.domain.ParentStudentLink;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.learning.application.port.out.LearningNotifier;
import uz.academixai.notification.application.NotificationService;
import uz.academixai.notification.domain.NotificationType;

/**
 * Delivers Learning's events through the notification inbox (and Telegram, when the user linked
 * it). Each recipient's notification preferences still apply — NotificationService checks them.
 */
@Component
public class NotificationLearningNotifier implements LearningNotifier {

  private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("dd.MM HH:mm");

  private final NotificationService notifications;
  private final StudentProfileRepository students;
  private final UserRepository users;
  private final ParentChildAccess parents;

  public NotificationLearningNotifier(
      NotificationService notifications,
      StudentProfileRepository students,
      UserRepository users,
      ParentChildAccess parents) {
    this.notifications = notifications;
    this.students = students;
    this.users = users;
    this.parents = parents;
  }

  @Override
  public void homeworkAssigned(
      UUID schoolId, UUID classId, UUID assignmentId, String title, LocalDateTime deadlineAt) {
    String body = "\"%s\" — topshirish muddati %s.".formatted(title, deadlineAt.format(DEADLINE));
    Map<String, String> data = Map.of("assignmentId", assignmentId.toString());
    for (StudentProfileEntity student : students.findByClassIdAndSchoolId(classId, schoolId)) {
      notifications.sendNotification(
          student.getUserId(), NotificationType.HOMEWORK_ASSIGNED, "Yangi uy vazifasi", body, data);
    }
  }

  @Override
  public void homeworkGraded(
      UUID studentId,
      UUID submissionId,
      String assignmentTitle,
      int score,
      int fivePointGrade,
      boolean regraded) {
    String title = regraded ? "Baho yangilandi" : "Ish baholandi";
    String result = "%d ball, baho: %d".formatted(score, fivePointGrade);
    Map<String, String> data = Map.of("submissionId", submissionId.toString());
    notifications.sendNotification(
        studentId,
        NotificationType.HOMEWORK_GRADED,
        title,
        "\"%s\" — %s.".formatted(assignmentTitle, result),
        data);

    String childName =
        users
            .findById(studentId)
            .map(user -> user.getFirstName() + " " + user.getLastName())
            .orElse("Farzandingiz");
    for (ParentStudentLink link : parents.parentsOf(studentId)) {
      notifications.sendNotification(
          link.parentUserId(),
          NotificationType.HOMEWORK_GRADED,
          title,
          "%s: \"%s\" — %s.".formatted(childName, assignmentTitle, result),
          Map.of("studentId", studentId.toString(), "submissionId", submissionId.toString()));
    }
  }

  @Override
  public void deadlineApproaching(
      UUID studentId, UUID assignmentId, String title, LocalDateTime deadlineAt) {
    notifications.sendNotification(
        studentId,
        NotificationType.DEADLINE_REMINDER,
        "Muddat yaqin",
        "\"%s\" — topshirish muddati %s. Ishingizni hali yubormagansiz."
            .formatted(title, deadlineAt.format(DEADLINE)),
        Map.of("assignmentId", assignmentId.toString()));
  }
}
