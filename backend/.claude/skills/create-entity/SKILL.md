---
name: create-entity
description: Add or extend a domain object + JPA entity + repository in the AcademiX backend, in the right bounded context, with RLS where needed. Use whenever adding a new entity/table object or changing an existing one. Trigger for "add an entity", "new JPA entity", "create the X entity", "add a field to X".
---

# Add an AcademiX entity

1. **Contract**: `academix_tz.md` §1 has field lists for the existing entities — use them when they fit. For new or changed entities, design what the feature needs and update §1 (and the backend TDD schema) in the same change.
2. **Placement** (modular monolith):
   - Domain record in `<context>/domain/` — plain Java, no Spring/JPA (`DomainArchitectureTest`).
   - JPA entity + Spring Data repository in `<context>/infrastructure/persistence/`, mapped to/from the domain record; the use case reaches it through a `port/out` interface.
   - Many older entities still live in the legacy `domain/` + `infrastructure/persistence/`; extend them in place unless you're deliberately migrating that context.
3. **Migration**: pair it with a Flyway migration (`create-migration`). Keep column mapping and DDL identical.
4. **Tenant data?** If the table holds school data, it needs `school_id` + an RLS policy, or a justified entry in `TenantRlsCoverageTest`'s backlog — the build fails otherwise. Map `school_id` on the entity even when other entities in the same feature don't have it (RLS is per table).
5. JSONB columns: `@JdbcTypeCode(SqlTypes.JSON)` on every one (even always-null); free text is `TEXT`, not jsonb.
6. Repository queries on a non-unique column pair: `findFirst…OrderBy…Desc`, not `Optional findBy…`.
