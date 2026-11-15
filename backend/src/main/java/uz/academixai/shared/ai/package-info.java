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
 * <p>{@link AiProvider} carries three capabilities (grading, tutor chat, psychology analysis)
 * because that is the shape of the vendor's HTTP surface. Every consumer narrows it to the method
 * it needs through its own port ({@code intelligence...GradingAi}, {@code
 * wellbeing...BehaviorAnalyzer}).
 */
package uz.academixai.shared.ai;
