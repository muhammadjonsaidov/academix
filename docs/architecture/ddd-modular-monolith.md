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

`Learning` is the core domain. `Identity`, `Notification`, file storage, AMQP and AI providers are
supporting domains or technical adapters. A context references another context only through a
published command/query interface or an immutable event; it must not join its tables directly.

## Aggregates and consistency

An aggregate protects one business invariant and is saved through its root. Initial aggregate roots
are `Account`, `School`, `HomeworkAssignment`, `HomeworkSubmission`, `Exam`, `ExamSubmission`, and
`PsychologicalSignal`. Cross-aggregate effects are asynchronous events after the source transaction
commits. For example, a submitted homework can trigger AI analysis, XP evaluation and notification;
submission acceptance itself must not wait for those side effects.

The existing RabbitMQ messages are transport adapters, not domain models. New flows publish an
application event first, then an adapter reliably externalizes it. Introducing an outbox is required
before scaling the backend beyond one instance.

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

The remaining global `application`, `domain`, `infrastructure` and `interfaces` packages are legacy
code and are not the target layout. They are migrated context by context; a big-bang package move is
not permitted.

## Ordered implementation plan

1. Complete Identity ports for token issuance, sessions, rate limiting and school-context lookup.
2. Migrate School ownership and membership management.
3. Migrate Learning around submission and grading aggregates; publish submission events after commit.
4. Move AI, handwriting and OCR behind `Intelligence` ports and consume Learning events.
5. Migrate Wellbeing and Notification to event-driven consumers.
6. Add architectural dependency tests when Spring Modulith publishes a stable Spring Boot 4.1
   compatible release, or enforce the same package rules with ArchUnit if it is needed earlier.
