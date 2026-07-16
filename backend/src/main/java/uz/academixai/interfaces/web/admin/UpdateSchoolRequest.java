package uz.academixai.interfaces.web.admin;

/** academix_tz.md §2.2 {@code PUT /admin/school} — exact body shape. */
public record UpdateSchoolRequest(
    String name, String address, String region, String district, String phone) {}
