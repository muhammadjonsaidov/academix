---
name: create-entity
description: Create a JPA entity + repository for one of the 24 AcademiX domain objects defined in academix_tz.md §1 (User, School, HomeworkAssignment, HandwritingProfile, PsychologicalSignal, etc.), matching field names/types exactly and wiring RLS where required. Use whenever adding a new entity or extending an existing one. Trigger for "add an entity", "new JPA entity", "create the X entity/table object".
---

# Create an AcademiX entity

## Contract source

Grep `academix_tz.md` §1 for the entity — every one of the 24 core entities has a full Java field list with types and comments there (e.g. §1.13 `HandwritingProfile`, §1.14 `PsychologicalSignal`). Use it verbatim: field names, types, and any inline JSON sub-structures (`StepAnalysis`, `CriteriaScore`, `CriteriaItem`) are exact, not illustrative.

## Steps

1. Read the full entity definition in TZ §1, including any enum types declared alongside it and any narrative notes directly under the code block (several entities have correctness/business-rule notes right after the `@Entity` block — e.g. §1.9's unique-task verification flow, §1.14's severity→notify matrix — these aren't optional flavor text, they constrain how the entity's fields get used).
2. Check whether this table already has full DDL in `academix_backend_tdd.md` §4 — if so, the migration should already exist or be created via the `create-migration` skill; don't let the entity's column mapping drift from that DDL.
3. **RLS-scoped?** If this entity carries `schoolId` and is one of the 9 tables in the backend TDD's RLS list, don't skip the migration's RLS policy just because you're focused on the Java side — flag it if the corresponding migration doesn't have `ENABLE ROW LEVEL SECURITY` yet.
4. Repository layer: standard Spring Data repository unless the entity has a documented custom query need (e.g. `HandwritingProfile` needs a pgvector cosine-similarity query, not a standard JPA derived-query method).
5. Package layout is decided (CLAUDE.md "Repository layout" — DDD/Clean Architecture): the entity itself goes in `domain/` as a plain object with business rules, no framework annotations beyond what persistence strictly requires. JPA mapping (`@Entity`/`@Table`/repository) lives in `infrastructure/` — keep the domain model from leaking JPA-specific concerns where practical, per the dependency direction in CLAUDE.md (`infrastructure` depends on `domain`, never the reverse).

## After writing

If the entity is new (not in the spec at all), that's new surface — confirm with the user whether `academix_tz.md` should be updated to reflect it (a human edits the spec directly; this session won't touch it without confirmation, per the `spec-guard` hook).
