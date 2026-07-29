package uz.academixai.interfaces.web.admin;

/** {@code type} must be a {@link uz.academixai.domain.SubjectType} name; icon is optional. */
public record CreateSubjectRequest(String name, String type, String icon) {}
