package uz.academixai.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics;
import uz.academixai.reporting.application.port.out.LearningActivityCounts;
import uz.academixai.reporting.application.port.out.SchoolDirectoryCounts;
import uz.academixai.reporting.application.port.out.WellbeingAlertCounts;

class AdminDashboardServiceTest {

  @Test
  void dashboardIncludesTheCountsNeededForGuidedSchoolSetup() {
    UUID schoolId = UUID.randomUUID();
    SchoolDirectoryCounts schoolCounts = mock(SchoolDirectoryCounts.class);
    LearningActivityCounts activity = mock(LearningActivityCounts.class);
    WellbeingAlertCounts alerts = mock(WellbeingAlertCounts.class);
    AnalyticsStatistics statistics = mock(AnalyticsStatistics.class);

    when(schoolCounts.of(schoolId)).thenReturn(new SchoolDirectoryCounts.Counts(2, 41, 3, 6));
    when(activity.assignments(schoolId)).thenReturn(5L);
    when(activity.distinctSubmittersSince(eq(schoolId), any())).thenReturn(0);
    when(statistics.classComparison(eq(schoolId), any(), any())).thenReturn(List.of());
    when(statistics.teacherRanking(schoolId)).thenReturn(List.of());
    when(alerts.unresolved(eq(schoolId), any())).thenReturn(0);

    AdminDashboardService.Dashboard dashboard =
        new AdminDashboardService(schoolCounts, activity, alerts, statistics).dashboard(schoolId);

    assertThat(dashboard.totalClasses()).isEqualTo(2);
    assertThat(dashboard.totalTeachers()).isEqualTo(3);
    assertThat(dashboard.totalSubjects()).isEqualTo(6);
    assertThat(dashboard.totalAssignments()).isEqualTo(5);
    assertThat(dashboard.totalStudents()).isEqualTo(41);
  }
}
