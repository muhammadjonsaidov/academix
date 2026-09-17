package uz.academixai.intelligence.application.port.out;

/** Tutor-specific text-generation provider boundary. */
public interface TutorAi {

  String respond(String subjectContext, String studentMessage);
}
