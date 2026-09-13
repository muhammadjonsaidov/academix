package uz.academixai.wellbeing.adapter.in.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.academixai.wellbeing.application.port.in.BehaviorAnalysis;

/** Inbound scheduler adapter for the nightly behavior-analysis job. */
@Component
public class BehaviorAnalysisScheduler {

  private final BehaviorAnalysis analysis;

  public BehaviorAnalysisScheduler(BehaviorAnalysis analysis) {
    this.analysis = analysis;
  }

  /** Runs nightly at 23:00, matching the product specification. */
  @Scheduled(cron = "0 0 23 * * *")
  public void analyzeAllStudentsBehavior() {
    analysis.analyzeAllActiveStudents();
  }
}
