package uz.academixai.infrastructure.ai;

import uz.academixai.domain.SubjectType;

/**
 * academix_tz.md §1.9 step 1 — an optional, deterministic, per-{@link SubjectType} pre-check that
 * runs before the independent AI verification call. Cheap and exact: catches obviously-wrong
 * generated tasks without spending a Qwen call. Subjects with no registered validator skip this
 * step entirely (see {@link UniqueTaskValidatorRegistry}).
 */
public interface UniqueTaskValidator {

  SubjectType subjectType();

  boolean isSolvable(String taskContent);
}
