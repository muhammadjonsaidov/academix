/**
 * Shared-kernel AI vendor contract: the provider capabilities and the request/response shapes an
 * OpenAI-compatible endpoint speaks.
 *
 * <p>These are integration contracts, not domain model — which is why they belong in the shared
 * kernel while {@code User}, {@code Role} and the business entities deliberately do not. Same shape
 * as {@link uz.academixai.shared.realtime.RealtimePublisher}: an interface in the shared kernel,
 * implemented by an adapter ({@code uz.academixai.infrastructure.ai.OpenAiCompatibleClient}), so a
 * context can call the vendor without importing the legacy infrastructure tree.
 *
 * <p>Known and deliberate: {@link AiProvider} is a god interface — six unrelated capabilities
 * (grading, lesson plans, unique-task generation, solvability, tutor chat, psychology analysis) on
 * one type, because that is the shape of the vendor's HTTP surface. Every consumer already narrows
 * it to the one or two methods it needs through its own port ({@code intelligence...GradingAi},
 * {@code learning...UniqueTaskAi}, {@code wellbeing...BehaviorAnalyzer}), so the split is a rename
 * of those adapters away from this interface rather than a redesign. Left whole for now because
 * splitting it is what would let the legacy {@code infrastructure/ai} tree move into Intelligence,
 * and that is its own slice.
 */
package uz.academixai.shared.ai;
