package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.family.domain.ParentRelation;

/** academix_tz.md — POST /admin/parents/link body, exact shape. */
public record LinkParentRequest(String parentPhone, UUID studentId, ParentRelation relation) {}
