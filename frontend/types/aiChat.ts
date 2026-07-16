// academix_tz.md §2.3/§3.4 "AI Tutor chat" — exact request/response shapes.
export type SubjectType =
  | "MATH"
  | "LANGUAGE_UZ"
  | "LANGUAGE_RU"
  | "LANGUAGE_EN"
  | "PHYSICS"
  | "CHEMISTRY"
  | "BIOLOGY"
  | "HISTORY"
  | "GEOGRAPHY"
  | "OTHER";

export type ChatBlockReason = "IRRELEVANT_QUESTION" | "POTENTIAL_ANSWER_LEAK" | "BUDGET_EXHAUSTED";

export interface AiChatRequest {
  subject: SubjectType | "GENERAL";
  message: string;
  context?: { assignmentId: string };
}

export interface AiChatResponse {
  response: string;
  isBlocked: boolean;
  blockReason: ChatBlockReason | null;
}

export interface AiChatHistoryItem {
  id: string;
  subject: SubjectType;
  message: string;
  response: string;
  isBlocked: boolean;
  createdAt: string;
}
