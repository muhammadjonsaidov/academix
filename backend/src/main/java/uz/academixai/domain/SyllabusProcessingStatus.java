package uz.academixai.domain;

/** Lifecycle of the asynchronous syllabus-to-knowledge-base pipeline. */
public enum SyllabusProcessingStatus {
  PENDING,
  PROCESSING,
  READY,
  FAILED
}
