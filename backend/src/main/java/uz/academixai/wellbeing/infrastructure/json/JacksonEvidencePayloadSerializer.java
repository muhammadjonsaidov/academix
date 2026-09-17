package uz.academixai.wellbeing.infrastructure.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import uz.academixai.wellbeing.application.port.out.EvidencePayloadSerializer;

/** Jackson implementation for the legacy raw-evidence JSON column. */
@Component
public class JacksonEvidencePayloadSerializer implements EvidencePayloadSerializer {

  private final ObjectMapper objectMapper;

  public JacksonEvidencePayloadSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String serialize(String evidence) {
    try {
      return objectMapper.writeValueAsString(Map.of("evidence", evidence == null ? "" : evidence));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize signal evidence", exception);
    }
  }
}
