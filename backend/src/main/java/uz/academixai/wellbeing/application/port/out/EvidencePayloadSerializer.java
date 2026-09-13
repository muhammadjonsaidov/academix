package uz.academixai.wellbeing.application.port.out;

/** Serializes raw evidence without binding the use case to a JSON implementation. */
public interface EvidencePayloadSerializer {

  String serialize(String evidence);
}
