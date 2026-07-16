package uz.academixai.interfaces.web.student;

import uz.academixai.application.ParentProgressService.SubjectProgress;

public record SubjectProgressResponse(
    String subject,
    double currentAvg,
    double previousMonthAvg,
    double growth,
    double submissionRate,
    String trend) {

  public static SubjectProgressResponse from(SubjectProgress domain) {
    return new SubjectProgressResponse(
        domain.subject(),
        domain.currentAvg(),
        domain.previousMonthAvg(),
        domain.growth(),
        domain.submissionRate(),
        domain.trend());
  }
}
