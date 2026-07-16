package uz.academixai.interfaces.web.student;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.AiChatService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.3/§3.4 "AI Tutor chat" — exact contract, don't drift path/shape. */
@RestController
@RequestMapping("/api/v1/student/ai-chat")
@PreAuthorize("hasRole('STUDENT')")
public class AiChatController {

  private final AiChatService chatService;

  public AiChatController(AiChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping
  public AiChatResponse chat(
      @AuthenticationPrincipal AcademixPrincipal principal, @RequestBody AiChatRequest request) {
    return AiChatResponse.from(
        chatService.chat(
            principal.schoolId(),
            principal.userId(),
            request.subject(),
            request.message(),
            request.assignmentId()));
  }

  @GetMapping("/history")
  public List<AiChatHistoryItemResponse> history(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) String subject,
      @RequestParam(required = false, defaultValue = "50") int limit) {
    return chatService.history(principal.userId(), subject, limit).stream()
        .map(AiChatHistoryItemResponse::from)
        .toList();
  }
}
