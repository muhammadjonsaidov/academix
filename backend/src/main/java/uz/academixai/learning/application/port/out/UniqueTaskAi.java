package uz.academixai.learning.application.port.out;

/** Intelligence boundary for generation and independent solvability verification. */
public interface UniqueTaskAi {

  String generate(String subjectAndGrade, String standardDescription);

  boolean verifySolvable(String taskContent);
}
