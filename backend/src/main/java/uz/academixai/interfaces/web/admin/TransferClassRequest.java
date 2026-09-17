package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** academix_tz.md §2.2 — { newClassId: "uuid" } */
public record TransferClassRequest(@NotNull UUID newClassId) {}
