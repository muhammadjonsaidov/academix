# AcademiX AI — MVP production roadmap

## Qaror

Mavjud barcha feature saqlanadi. Platforma mikroservislarga bo'linmaydi. U **modular monolith**
bo'lib qoladi: bitta Spring Boot deployable, bitta PostgreSQL transactional boundary va alohida
Telegram delivery worker. Bu hozirgi product uchun eng past operatsion murakkablikni, lekin aniq
modul chegaralarini beradi.

Maqsad — feature'ni qisqartirish emas, balki har bir feature'ni production-safe, testlanadigan va
keyinchalik mustaqil servicega ajratish mumkin bo'lgan holatga olib chiqish.

## Target arxitektura

```text
                    Next.js web application
                             |
                       REST / SSE gateway
                             |
                  +------------------------+
                  |   AcademiX backend      |
                  |   modular monolith     |
                  +------------------------+
   Identity | School | Learning | Intelligence | Progress | Wellbeing | Family
                  |       |          |              |           |
                    Notification | Reporting
                             |
                     transactional outbox
                             |
                         RabbitMQ broker
                    /                         \
      backend asynchronous consumers       telegram-bot worker
                             |
 Postgres + Redis + S3/SeaweedFS + AI/OCR providers + SMTP + Telegram API
```

Har business context quyidagi vertikal slice ko'rinishida bo'ladi:

```text
<context>/
  domain/             Aggregate, value object, business invariant, domain event
  application/        Command/query use case va port/in, port/out
  infrastructure/     JPA, Redis, AMQP, S3, HTTP adapterlari
  adapter/in/web/     Controller, request/response DTO, HTTP mapping
```

Qatlam qoidalari:

1. `domain` Spring, JPA, HTTP, RabbitMQ va boshqa frameworklarni import qilmaydi.
2. `application` faqat o'z domain'i, o'z portlari va boshqa contextning **published API**siga
   bog'lanadi; JPA entity/repository yoki boshqa context jadvaliga emas.
3. `infrastructure` portlarni implement qiladi. JPA entity va Spring Data repository shu qatlamda.
4. Controller faqat auth contextni oladi, requestni validatsiya qiladi, command/query'ga map qiladi
   va response qaytaradi. Unda business rule bo'lmaydi.
5. Contextlar orasidagi write side effectlar domain/application event orqali asinxron bajariladi.
   Bir context ikkinchisining repositorysini chaqirmaydi.
6. API va database migration backward-compatible bo'ladi; har deploy rollback qilinadigan bo'lishi
   kerak.

## Context ownership

| Context | Aggregate / data owner | Public capability |
|---|---|---|
| Identity | Account, credential, session, password recovery | authenticate, authorize, account lifecycle |
| School | School, class, subject, teacher/class membership, student profile | school membership va academic structure |
| Learning | Assignment, submission, exam, grade, criteria, syllabus, lesson plan, unique task | homework/exam lifecycle va final grading |
| Intelligence | OCR, AI feedback, plagiarism, handwriting profile/check, tutor chat | AI/OCR orchestration; AI hech qachon final grade owner emas |
| Progress | XP ledger, badge, streak | graded workdan achievement hisoblash |
| Wellbeing | psychological signal, watchlist, intervention | signal detection va human-review workflow |
| Family | parent-child link, consent, deletion request | parental access, consent va data rights |
| Notification | notification, preference, delivery record, Telegram connection | eventdan delivery yaratish |
| Reporting | report definition, generated report | immutable reporting/read model |

`Role`, `TenantId/SchoolId`, `UserId`, `CorrelationId`, vaqt va pagination kabi kichik immutable
tiplar `shared-kernel`da bo'ladi. Business entity, JPA repository va service hech qachon shared
kernelga o'tmaydi.

## Patternlar: qayerda va nima uchun

| Pattern | Qo'llanadigan joy | Aniq qoida |
|---|---|---|
| Modular monolith / bounded context | Butun backend | Global `application/domain/infrastructure/interfaces` legacy tree'ni use-case bo'yicha contextlarga ko'chirish |
| Hexagonal architecture (ports/adapters) | Har bir context | Application service faqat portga bog'lanadi; JPA, S3, AI, Redis, RabbitMQ adapter bo'ladi |
| Aggregate + rich domain model | Learning, Progress, Family, Wellbeing | `HomeworkSubmission` status transition, `ExamSubmission` approval, consent va signal resolution domain invariant bo'ladi |
| CQRS-lite | Dashboard, analytics, reporting | Command side normalized aggregate'ni o'zgartiradi; query side read repository/projection qaytaradi. Alohida event-sourcing kerak emas |
| Transactional outbox | Submission, grade, notification, deletion, AI workflows | DB write va event bir transactionda `outbox_events`ga yoziladi; publisher RabbitMQga yetkazadi |
| Idempotent consumer / inbox | AI grading, XP, Telegram, report generation | Consumer `event_id`ni inboxda saqlaydi; qayta delivery duplicate grade/xp/message yaratmaydi |
| Saga / process manager | Homework/exam AI workflow, deletion workflow | Orchestration state machine bo'ladi: accepted → OCR → AI → teacher review → graded → XP/notification. Distributed transaction ishlatilmaydi |
| State machine | Submission, exam submission, deletion request, signal | Valid transitionlar enum/service if'lari emas, domain policyda markazlashadi; illegal state 409 beradi |
| Strategy | AI provider, grading policy, plagiarism/handwriting scoring, notification channel | `AiProvider`, `OcrProvider`, `NotificationChannel`, `GradingPolicy` portlari; provider switch config orqali |
| Adapter / anti-corruption layer | OpenAI-compatible API, Google Vision, Telegram, Jasper, S3 | Vendor DTO yoki exception domain/application ichiga o'tmaydi |
| Specification / query object | Student list, submission filters, analytics | Filterlash SQL/JPA query adapterda; service `findAll().stream()` qilmaydi |
| Repository | Aggregate persistence | Repository aggregate root uchun; dashboard projectionlari alohida read repository bo'ladi |
| Unit of work | Command transaction | Har command use case bitta transaction. Cross-context side effect event/outbox orqali |
| Policy object | Authorization, retention, consent, AI budget | Role check controllerga sochilmaydi; `AccessPolicy`, `ConsentPolicy`, `AiBudgetPolicy`ga yig'iladi |
| Result/error taxonomy | HTTP va async processing | Typed business errorlar (`Validation`, `Conflict`, `Forbidden`, `TransientExternal`) standart error responsega map qilinadi |
| BFF-lite | Next.js frontend | Browser faqat same-origin `/api` bilan gaplashadi; Next rewrite/proxy backendga yo'naltiradi. Backend authorizationning yagona manbai bo'lib qoladi |

Qo'llanmaydigan patternlar: hozir microservice, event sourcing, CQRS database'larini ajratish,
service mesh va distributed saga orchestration kerak emas. Ular MVP'ni tezlashtirmaydi.

## Majburiy cross-cutting qoidalar

- Har requestda `correlationId`, authenticated `userId`, `schoolId`, route va audit action loglanadi.
- Har tenant-scoped jadvalda `school_id NOT NULL`, composite tenant index va RLS policy bo'ladi.
  Owner bypass qilmasligi uchun app database roli restricted bo'ladi; RLS uchun integration test majburiy.
- Har external call timeout, bounded retry, circuit breaker va typed failure bilan ishlaydi.
- API request DTO `@Valid` bilan tekshiriladi; domain invariant command/use case darajasida yana
  tekshiriladi.
- File upload allowlist, magic-byte check, size limit, antivirus, private bucket va presigned
  download URL bilan ishlaydi.
- AI natijasi audit qilinadi: provider/model/prompt version/input hash/cost/latency/status.
  Teacher final grade owner bo'lib qoladi.
- Psychological yoki biometric feature faqat explicit policy/consent va human-review bilan natija
  chiqaradi; AI avtomatik jazolash, ota-onaga avtomatik sensitive notification yubormaydi.

## Roadmap va deliverablelar

### Phase 0 — release blockerlarni yopish (1-3 kun)

**Infra**

- `seed` servisni `demo` profile'ga o'tkazish. Production compose demo SQL, demo credential va
  demo bucket data'ni umuman ishlatmasin.
- Production compose'ni alohida `compose.production.yml`ga chiqarish: faqat nginx `80/443`
  publish qiladi. Postgres, Redis, RabbitMQ, SeaweedFS management va backend portlari private
  networkda qoladi.
- Redis auth, strong database/app/RabbitMQ/S3 secretlari startup validation bilan majburiy bo'lsin.
- `latest` image'larni immutable version/digestga pin qilish. Next.js va lockfile dependency audit
  criticallarini patch qilish.
- Backup encryption, offsite destination va restore drill pipeline qo'shish.

**Backend/security**

- Refresh tokenni login JSON response va Zustand'dan olib tashlash; faqat HttpOnly Secure cookie.
  Refresh token rotation, token family/reuse detection va session revoke qo'shish.
- SSE query parameter JWT'ni one-time short-lived stream ticketga almashtirish.
- Barcha request DTO'ga bean validation; global validation error shape va API limitlar.
- Production profile fail-fast: default JWT secret, default DB password, enabled demo profile,
  public Swagger, no antivirus kabi noto'g'ri config bilan boot qilmasin.

**Definition of done:** internetda demo user yo'q; ichki portlar ochiq emas; dependency audit critical
0; production config default credential bilan start qilmaydi; refresh token browser JS'iga chiqmaydi.

### Phase 1 — reliability foundation (1 hafta)

**Backend**

- `outbox_events` va `processed_events` migrationlarini yaratish. Eventda `id`, `type`, `payload`,
  `aggregate_type/id`, `school_id`, `occurred_at`, `attempt`, `status`, `correlation_id` bo'ladi.
- Outbox publisher: polling + `FOR UPDATE SKIP LOCKED`, publisher confirm, exponential retry,
  alertable failed state. DLQ replay endpoint faqat admin/ops uchun.
- Avval uch oqimni migratsiya qilish: homework submit → AI analysis; exam submit → AI analysis;
  notification created → Telegram delivery.
- Consumerlarni event idempotent qilish. XP, feedback, notification va delivery unique constraint
  bilan duplicatega chidamli bo'ladi.
- Storage startup dependency'ni application liveness'dan ajratish: health/readiness component,
  degraded state va upload endpointning clear xatosi.
- Actuatorga liveness/readiness, queue depth, outbox lag, AI failure rate, storage health metriclar.

**Telegram bot**

- Backend-dagi notification wire DTO'ni shared *contract module* yoki JSON schema/OpenAPI AsyncAPI
  artifact orqali versionlash; copy-paste Java record emas.
- Delivery attempts, Telegram message id, permanent/transient error va opt-out holatini saqlash.
- Polling offset'ini durable saqlash, graceful shutdown va idempotent command handling qo'shish.

**Definition of done:** RabbitMQ 10 minut o'chib tursa ham submission/notification yo'qolmaydi;
consumer retry qilinsa duplicate XP/grade/Telegram xabari chiqmaydi; DLQ replay testlangan.

### Phase 2 — architecture guardrails va tenancy (1 hafta)

**Architecture**

- `shared-kernel`ni minimal yaratish: `SchoolId`, `UserId`, `CorrelationId`, pagination va audit
  abstractions. `User`, `Role` va business entity'larni u yerga ko'chirmaslik.
- ArchUnit testlar qo'shish: domain framework-free; application infrastructure/import qilmasin;
  controller repository chaqirmasin; contextlar JPA entity/repository bilan kesishmasin.
- CI'ga architecture test va dependency graph report qo'shish.
- Tenant schema audit: har table owner/context, `school_id`, FK, RLS, index va deletion behavior
  inventorysi. Missing school scope'li sezgir tablelarni migration bilan tuzatish.
- `SchoolContext`ni auth tokenning yagona source'i qilish; cross-tenant ID lookup'lar ham
  `schoolId` predicate bilan ishlashi shart.

**Definition of done:** architecture qoidasi buzilgan PR compile/testda yiqiladi; tenant isolation
role × context × table integration matrix bilan testlangan.

### Phase 3 — School va Learning core migration (2 hafta)

Bu eng muhim refactor. Big-bang ko'chirish taqiqlanadi.

1. `school` context: school, class, subject, class-subject-teacher, student profile va membership
   use case'larini vertikal packagega ko'chirish. Public query portlar: `SchoolMembershipQuery`,
   `ClassCatalogQuery`, `StudentProfileQuery`.
2. `learning` contextga homework assignment/submission va gradingni bir use case'dan ko'chirish.
   Birinchi slice: create/update/publish assignment. Ikkinchi slice: submit homework.
   Uchinchi slice: teacher review/final grade. To'rtinchi: exam flow.
3. `HomeworkSubmission` va `ExamSubmission` uchun explicit state machine chiqarish:

```text
ACCEPTED -> PENDING_ANALYSIS -> OCR_DONE -> AI_DONE -> AWAITING_REVIEW -> GRADED
                       \-> ANALYSIS_FAILED -> RETRY_SCHEDULED
```

4. Dashboardlar aggregate repository emas, read projection/query portdan o'qiydi. Pagination,
   sorting, filter va aggregate SQL/database tomoniga suriladi.
5. `findAll().stream()` analytics va N+1 joylari query/projectionga almashtiriladi.

**Definition of done:** homework/examning har bir oldingi API contracti ishlaydi; state transition
unit testlangan; submit-to-grade E2E flow outbox bilan testlangan; dashboard querylari bounded/paged.

### Phase 4 — Intelligence va Progress migration (2 hafta)

**Intelligence**

- AI/OCR/vision/handwriting/chat uchun `AiProvider`, `OcrProvider`, `ObjectStorage` portlari.
  OpenAI-compatible, Google Vision, SeaweedFS adapter bo'ladi.
- `SubmissionAccepted` eventidan Intelligence process manager boshlanadi. U faqat AI feedback va
  analysis status yaratadi; Learning final grade ownership'ni saqlaydi.
- Prompt template/version registry, provider model selection, token/cost ledger va per-school
  budget policy.
- Handwriting: vector profile, reset, anomaly lifecycle va manual review queue. Biometric consent
  policy talab qilinadi.

**Progress**

- `SubmissionGraded` eventidan XP/streak/badge hisoblash. `xp_history` immutable ledger bo'ladi;
  event id uniqueness duplicate awardni to'xtatadi.

**Definition of done:** AI provider down bo'lsa submission saqlanadi va retry qilinadi; AI final
grade'ni o'zgartirmaydi; XP exactly-once business effectga ega.

### Phase 5 — Wellbeing, Family, Notification, Reporting (2 hafta)

- Wellbeing signal lifecycle: detected → triaged → assigned → resolved/dismissed. Signal policy
  score/evidence'ni human-review'dan ajratadi. Psychologist/teacher access policy markazlashadi.
- Family context: parent link va consent tarixini immutable audit bilan yuritadi. Consent revoke
  biometric/AI workflows'ni to'xtatadi.
- Deletion saga: requested → approved → export/hold check → database deletion → S3 deletion →
  backup retention expiry → completed. Har qadam auditli va retryable.
- Notification barcha contextdan direct service call emas, typed event iste'molchisi bo'ladi.
  Channel strategy: in-app, Telegram, email. Preference/consent har delivery oldidan tekshiriladi.
- Reporting: read-model/projectiondan PDF yaratadi; Jasper async job, storage URL va status orqali
  ishlaydi; request thread'da og'ir PDF generation qilinmaydi.

**Definition of done:** sensitive signal avtomatik final decision qilmaydi; consent revoke testlangan;
deletion har storage qatlamidan kuzatiladi; report generation requestni bloklamaydi.

### Phase 6 — frontend platform hardening (parallel, 2-3 hafta)

- Feature-sliced frontend: `features/<context>/{api,model,ui}`. Katta page'ni route composition
  komponentiga, form, table, dialog va data hook'ga ajratish.
- OpenAPI backend contractdan TypeScript client/type generation. Hand-written duplicated DTO'larni
  bosqichma-bosqich olib tashlash.
- Server state uchun cache/query layer (TanStack Query yoki minimal in-house wrapper), Zustand faqat
  auth/UI ephemeral state uchun. API cache invalidation eventlariga mos bo'ladi.
- Form validation schema'lari backend constraints bilan bir manbadan yoki contractdan keladi.
- Single-origin `/api/v1` BFF-lite route. Browserda backend URL/credential branching kamayadi.
- Error boundary, loading/skeleton, empty state, retry UX, accessible toast/form error/focus
  management. Dashboard oldindan static data cache qilmasin.
- SSE reconnect/backoff, event ordering va optimistic update policy aniq yoziladi.
- Playwright role-based E2E suite: login/refresh/logout, admin CRUD, teacher assignment, student
  upload, grading, parent read-only, psychologist workflow, Telegram connect, error/retry.

**Definition of done:** 5 rolning critical business journey'lari E2E'da; frontend-backend contract
drift CI'da aniqlanadi; tokenlar browser storage'da emas.

### Phase 7 — delivery, observability va operational readiness (1 hafta)

- Separate `dev`, `demo`, `staging`, `production` compose/Helm environment configlari.
- CI gates: formatting, unit/integration/architecture/API contract/E2E, migration-from-empty va
  migration-from-previous-schema test, dependency/container scan, coverage threshold.
- Stagingda productionga yaqin integration stack va synthetic journey monitoring.
- Structured JSON logs, PII redaction, Sentry/OpenTelemetry tracing, alert policy.
- Runbooklar: rollback, DLQ replay, AI provider incident, storage outage, compromised token,
  data deletion, backup restore.
- Blue/green yoki rolling deploy. Flyway expand-contract migration qoidasi; destructive migration
  alohida release bilan.

**Definition of done:** deploy smoke/E2E checkdan o'tadi; rollback testlangan; restore drill
o'tgan; critical queue/AI/storage/auth alertlari ishlaydi.

## Execution order va parallel tracks

```text
Week 1       Phase 0
Week 2       Phase 1 ----------------------- Frontend platform foundation
Week 3       Phase 2 ----------------------- Contract generation / E2E base
Week 4-5     Phase 3 ----------------------- Learning UI refactor
Week 6-7     Phase 4 ----------------------- AI/progress UI and observability
Week 8-9     Phase 5 ----------------------- Sensitive-flow UX
Week 10      Phase 7 + staging hardening
```

Har phase kichik, independently deployable PR'larga bo'linadi. Bir PR bitta use case, uning testlari,
migrationi va observability'sini olib yuradi. Katta package rename yoki barcha contextni birdan
ko'chirish qilinmaydi.

## Birinchi implementatsiya backlog'i

1. Production compose'dan `seed`ni chiqarish va demo compose profile yaratish.
2. Next.js/security dependency patch + automated audit CI gate.
3. Private network/ports/secrets production hardening.
4. Refresh-token cookie-only + rotation migration.
5. Outbox/inbox schema, publisher va homework submission oqimi.
6. ArchUnit guardrail + tenant/RLS matrix.
7. `school` contextning class/subject slice'i.
8. `learning` assignment create/publish slice'i.
9. `learning` homework submission state machine + async analysis slice'i.
10. OpenAPI generated frontend client va critical role-flow E2E.

Bu o'nta item tugamaguncha yangi feature yoki katta UI redesign ochilmaydi; mavjud feature bug-fixlari
va security hotfixlar istisno.
