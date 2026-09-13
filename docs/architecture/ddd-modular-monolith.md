# DDD modular-monolith architecture

## Decision

AcademiX remains one deployable Spring Boot application. It is not split into microservices yet:
the product is still evolving, its workflows share one PostgreSQL transaction boundary, and an
in-process module boundary is cheaper to change and test. The source code is organised by business
capability, not by global technical layer.

Each bounded context has this shape:

```
<context>/
  domain/                    aggregates, value objects and business rules
  application/               use cases and input/output ports
    port/in/                 public commands and queries (when a context needs an explicit API)
    port/out/                storage, messaging and external-service abstractions
  infrastructure/            outbound adapters for JPA, Redis, AMQP, HTTP and object storage
  adapter/in/web/            REST controllers and request/response DTOs
```

Controllers are inbound adapters. They only validate/map HTTP data and call an application use
case. An application service may depend on its own ports and on another context's public API, never
on another context's JPA entity or repository. An adapter implements a port and is the only place
that knows an external library or persistence mapping.

`Role` and stable identifiers are currently a shared kernel. They must remain small; business
entities and Spring Data repositories are never shared.

## Context map

| Context | Owns | Collaborates with |
| --- | --- | --- |
| Identity | accounts, credentials, sessions and access tokens | School for the school claim |
| School | schools, classes, subjects and memberships | Identity, Learning |
| Learning | homework, submissions, exams, criteria, grades and lesson plans | School, Progress, Intelligence |
| Progress | XP, badges, streaks and student progress | Learning |
| Intelligence | AI chat, OCR, AI grading, plagiarism and handwriting analysis | Learning, Wellbeing |
| Wellbeing | psychological signals, watchlists and reports | Learning, Intelligence, Notification |
| Notification | in-app preferences, Telegram links and delivery | all contexts through events |
| Reporting | read models and generated reports | School, Learning, Progress, Wellbeing |

## Package ownership map

The following is the target package ownership for every remaining global domain class. A class is
moved together with its JPA mapping/repository and its primary application use case; it is not moved
individually just to make the tree look cleaner.

| Target context | Domain types to move | Primary application area |
| --- | --- | --- |
| `school` | `School`, `SchoolClass`, `Subject`, `SubjectType`, `ClassSubjectTeacher`, `StudentProfile` | school administration, class/subject assignment and school context resolution |
| `learning` | `HomeworkAssignment`, `HomeworkSubmission`, `Exam`, `ExamSubmission`, `Grade`, `ExamGrade`, `SubmissionStatus`, `SubmissionType`, `AssignmentType`, `CriteriaItem`, `CriteriaScore`, `SubjectGradingCriteria`, `TeacherSyllabus`, `LessonPlan`, `LessonPlanContent`, `LessonActivity`, `StudentUniqueTask` | homework, exams, grading, syllabus, lesson plans and unique tasks |
| `intelligence` | `AIFeedback`, `ExamAIFeedback`, `AiChatMessage`, `HandwritingProfile`, `HandwritingCheckResult`, `HandwritingResetLog`, `PlagiarismType`, `StepAnalysis`, `ChatBlockReason` | OpenAI-compatible provider, chat, OCR, plagiarism and handwriting analysis |
| `wellbeing` | `PsychologicalSignal`, `SignalType`, `SignalSeverity`, `WatchlistEntry` | psychology signals, watchlists and intervention workflow |
| `family` | `ParentStudentLink`, `ParentRelation`, `DataDeletionRequest`, `DeletionRequestStatus` | parent-child links, consent and data-deletion requests |
| `identity` | `User`, `Role`, `PasswordResetLog`, `ResetReason` | credentials, account lifecycle and password recovery |
| `shared` | `FileType` | stable technical value type only; it must not gain business rules |

The migrated contexts currently use the exact target tree. `school` is migrated in compilable
slices because it has 152 source references: school/class/subject administration and
teacher-class-subject membership are complete vertical slices with outbound ports and JPA adapters.
`TeacherAccess` is the published School API used by Learning and teacher analytics;
`TeacherContextService` and `AssignmentService` no longer exist. Student profile and remaining
cross-context reads follow. This avoids a broken intermediate deployment.

`Learning` is the core domain. `Identity`, `Notification`, file storage, AMQP and AI providers are
supporting domains or technical adapters. A context references another context only through a
published command/query interface or an immutable event; it must not join its tables directly.

## Aggregates and consistency

An aggregate protects one business invariant and is saved through its root. Initial aggregate roots
are `Account`, `School`, `HomeworkAssignment`, `HomeworkSubmission`, `Exam`, `ExamSubmission`, and
`PsychologicalSignal`. Cross-aggregate effects are asynchronous events after the source transaction
commits. For example, a submitted homework can trigger AI analysis, XP evaluation and notification;
submission acceptance itself must not wait for those side effects.

RabbitMQ messages are transport adapters, not domain models. Submission acceptance and Telegram
delivery requests use the transactional outbox: the business row and durable event record commit
together, then a locked scheduled dispatcher publishes with retry/backoff. Delivery is at-least-once,
therefore consumers claim state transitions atomically or use their persisted delivery marker before
making external calls. New cross-context flows must use this path rather than `afterCommit`
publishing.

## Migration rules

1. Preserve public REST contracts and existing database tables during a context migration.
2. Move one complete use case at a time: domain model, application service, ports, adapters, then
   controller wiring.
3. Do not copy a legacy service into a new package. The new application service must depend on ports
   rather than `infrastructure.persistence.*`.
4. Keep temporary compatibility adapters explicit and delete them once their callers are migrated.
5. Add unit tests for domain invariants and integration tests at each module's public API before
   changing cross-context behaviour.

## Current migration state

`identity` is the first migrated context. `AuthenticationService` owns login, refresh, logout and
self-service profile operations. Its `AccountRepository` is an outbound port and
`JpaAccountRepository` adapts the existing `users` table. This preserves `schoolId` as part of the
identity aggregate so self-service edits cannot erase teacher-school ownership.

`notification` is the first fully vertical package migration:

```
notification/
  domain/                 Notification, NotificationPreference, NotificationType,
                          TelegramConnection
  application/            NotificationService, TelegramLinkService
  infrastructure/
    persistence/          notification and Telegram JPA entities/repositories
  adapter/in/web/         notification and Telegram REST controllers plus DTOs
```

`PsychologyService` currently calls `notification.application.NotificationService` directly. This
is an explicit transitional dependency; it will become a published `PsychologicalAlertRaised`
event when the Wellbeing context is migrated.

`reporting` is the second fully vertical migration:

```
reporting/
  domain/                 Report, ReportType
  application/            ReportService
  infrastructure/
    persistence/          report JPA entity and repository
    pdf/                  Jasper PDF generator and report-row model
  adapter/in/web/         report REST controller and DTOs
```

`ParentReportService` is still legacy application code and consumes Reporting's public
`ReportService` API. Its data ownership will move to the Reporting context when the Parent-facing
read model is migrated.

`progress` now owns the student achievement model:

```
progress/
  domain/                 Badge, BadgeCriteriaType, StudentBadge, XpHistoryEntry
  application/            XPService, StudentProgressService
  infrastructure/
    persistence/          badge, earned-badge and XP-history JPA entities/repositories
```

Learning, AI and legacy dashboard services currently consume the Progress application API or its
read repositories. These are migration seams; the Learning-to-Progress write path will be replaced
by a `SubmissionGraded` event when the Learning context is extracted.

The remaining global `application`, `domain`, `infrastructure` and `interfaces` packages are legacy
code and are not the target layout. They are migrated context by context; a big-bang package move is
not permitted.

`learning` owns the teacher homework-, exam-management, exam-submission and unique-task review
slices. Their controllers depend on `HomeworkManagement`, `ExamManagement`,
`ExamSubmissionWorkflow` and `UniqueTaskReview` input ports; the use cases depend on stores and
read-model ports plus School's published `TeacherAccess` API. Exam AI-budget lookup and the
underlying AI provider remain explicit compatibility adapters. Unique-task generation and review
are owned by Learning.

Student homework submission acceptance is also owned by Learning: the HTTP adapter maps a multipart
file to an immutable command payload, then the use case validates type/size, authorizes School
membership, applies quota and antivirus policies through ports, stores the image and writes the
transactional-outbox event. Student homework cards, detail, history, feedback and grade queries now
also use Learning's `StudentHomeworkQuery` API; dashboard and parent read models consume that same
published contract. Teacher submission listing/detail views use `TeacherSubmissionQuery`, while
`HomeworkGrading` owns final-grade writes, status changes and the Progress update. Both legacy
submission services have been deleted.

Exam-paper bulk upload, review, manual grading and approve-all are also owned by Learning's
`ExamSubmissionWorkflow`. The HTTP adapter converts multipart data to immutable payloads; the
workflow applies quota, file-safety and object-storage policies through ports. Each accepted paper
uses its own transaction to preserve partial success for a batch, while the submission record and
AI-processing event are committed together through the transactional outbox. The legacy
`ExamSubmissionService` and `ExamSubmissionWriter` have been deleted.

Student exam cards and result detail are exposed through Learning's `StudentExamQuery` port. It
authorizes the student's School membership through a published lookup and returns only domain read
models, so the web adapter never reaches JPA entities or persistence repositories.

`intelligence` now owns the monthly AI budget policy. Its `AiBudgetService` depends on a School
limit lookup and a Redis usage-counter port, while Learning and legacy callers consume that public
application API. Homework OCR, grading, plagiarism and handwriting orchestration are now owned by
`HomeworkAiAnalysisService`: its provider, object-storage, Learning-persistence, Progress and SSE
dependencies are all ports. Its queue contract and graceful `AI_SKIPPED` behavior are preserved.
`ExamAiAnalysisService` uses the same provider-neutral OCR/grading/handwriting boundaries while
keeping exam-only review flags and its separate budget. Tutor chat and psychology analysis remain
the next Intelligence slices.

## Ordered implementation plan

1. Complete Identity ports for token issuance, sessions, rate limiting and school-context lookup.
2. Complete School ownership and membership management (school/class administration is complete;
   subject, teacher membership and student profile remain).
3. Migrate Learning around submission and grading aggregates; publish submission events after commit.
4. Move AI, handwriting and OCR behind `Intelligence` ports and consume Learning events.
5. Migrate Wellbeing and Notification to event-driven consumers.
6. Add architectural dependency tests when Spring Modulith publishes a stable Spring Boot 4.1
   compatible release, or enforce the same package rules with ArchUnit if it is needed earlier.
