package uz.academixai.interfaces.web.psychologist;

/** academix_tz.md §2.6 — PUT /psychologist/signals/{signalId}/resolve body, exact shape. */
public record ResolveSignalRequest(String notes, String actionTaken) {}
