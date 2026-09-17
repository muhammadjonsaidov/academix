package uz.academixai.interfaces.web.psychologist;

import jakarta.validation.constraints.Size;

/** academix_tz.md §2.6 — PUT /psychologist/signals/{signalId}/resolve body, exact shape. */
public record ResolveSignalRequest(
    @Size(max = 500) String notes, @Size(max = 500) String actionTaken) {}
