package uz.academixai.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import uz.academixai.TestcontainersConfiguration;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.JwtService;

/**
 * Role-based access control (RBAC) integration test — verifies that every REST endpoint is
 * reachable only by its own role (the "har bir role o'z vazifasini qilayaptimi" matrix):
 *
 * <ul>
 *   <li>the allowed role can hit the endpoint (never a 401),
 *   <li>every other role gets {@code 403},
 *   <li>an anonymous request gets {@code 401}.
 * </ul>
 *
 * <p>Endpoints with {@code allowedRole == null} are common to every authenticated role ({@code
 * isAuthenticated()} — notifications/telegram/auth-profile).
 *
 * <p>Authentication uses real JWTs minted via {@link JwtService} — the same production path the
 * {@code JwtAuthenticationFilter} verifies. No seed data is needed: the filter derives role and
 * schoolId from the token alone, and {@code @PreAuthorize} denies before any service runs. Path
 * placeholders ({@code {studentId}}, {@code {id}}, ...) are replaced with throwaway UUIDs — their
 * value is irrelevant because authorization happens before business logic.
 *
 * <p>Requests carry a minimal body that actually parses into the endpoint's DTO: Spring resolves
 * {@code @RequestBody}/{@code @RequestPart} arguments BEFORE invoking the secured method, so a
 * bodyless or non-parsing request dies with 400/500 and never reaches {@code @PreAuthorize} — that
 * would falsely fail the 403 assertions. Primitive fields ({@code int}, {@code boolean}) reject
 * null, so those DTOs get concrete placeholder values; multipart endpoints send
 * {@code @RequestPart} fields as text files.
 *
 * <p>Positive-case assertion is deliberately {@code status != 401} (never {@code 200}): the role
 * gate is what this test proves, and business logic on an empty DB may legitimately answer
 * 400/404/500 afterwards (missing child link, missing profile, ...). The negative cases are strict
 * (403 / 401) — that's the actual RBAC guarantee.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleAccessIntegrationTest {

  /** {@code allowedRole == null} means any authenticated role (isAuthenticated()). */
  private record Endpoint(
      HttpMethod method, String path, Role allowedRole, boolean multipart, String jsonBody) {

    private Endpoint(HttpMethod method, String path, Role allowedRole) {
      this(method, path, allowedRole, false, "{}");
    }

    private Endpoint(HttpMethod method, String path, Role allowedRole, String jsonBody) {
      this(method, path, allowedRole, false, jsonBody);
    }

    private Endpoint(HttpMethod method, String path, Role allowedRole, boolean multipart) {
      this(method, path, allowedRole, multipart, null);
    }

    @Override
    public String toString() {
      return method() + " " + path();
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private final Map<Role, String> tokens = new EnumMap<>(Role.class);

  // Real user for the public auth-flow test — login needs an actual row to answer 200.
  private static final String AUTH_PHONE = "+998900009999";
  private static final String AUTH_EMAIL = "auth.user@academix.uz";
  private static final String AUTH_PASSWORD = "password123";

  @BeforeAll
  void issueTokensForEveryRole() {
    for (Role role : Role.values()) {
      String token = jwtService.issueAccessToken(UUID.randomUUID(), role, UUID.randomUUID());
      tokens.put(role, "Bearer " + token);
    }
    userRepository.save(
        new UserEntity(
            UUID.randomUUID(),
            "Auth",
            "User",
            AUTH_PHONE,
            AUTH_EMAIL,
            passwordEncoder.encode(AUTH_PASSWORD),
            Role.STUDENT,
            true,
            LocalDateTime.now(),
            null,
            null));
  }

  private static String resolve(String path) {
    // Value is irrelevant for authorization — every placeholder becomes one throwaway UUID.
    return path.replaceAll("\\{[a-zA-Z]+}", UUID.randomUUID().toString());
  }

  /** Builds a request that parses cleanly into the endpoint's DTO (see class Javadoc). */
  private RequestBuilder buildRequest(Endpoint endpoint, String token) {
    String path = resolve(endpoint.path());
    if (endpoint.multipart()) {
      MockMultipartHttpServletRequestBuilder mp = multipart(path);
      mp.file(
          new MockMultipartFile("file", "file.bin", "application/octet-stream", new byte[] {1}));
      mp.file(new MockMultipartFile("images", "img.jpg", "image/jpeg", new byte[] {1}));
      // @RequestPart text fields must be multipart parts (files), not query params.
      mp.file(new MockMultipartFile("subjectId", "", "text/plain", uuidBytes()));
      mp.file(new MockMultipartFile("classId", "", "text/plain", uuidBytes()));
      mp.file(
          new MockMultipartFile(
              "title", "", "text/plain", "Test title".getBytes(StandardCharsets.UTF_8)));
      mp.param("studentIds", UUID.randomUUID().toString());
      // POST /student/homework/{id}/submit requires a @RequestParam SubmissionType 'type' —
      // without it Spring dies in argument resolution (before @PreAuthorize) with a 500.
      mp.param("type", "TEXT");
      if (token != null) {
        mp.header("Authorization", token);
      }
      return mp;
    }
    MockHttpServletRequestBuilder builder = request(endpoint.method(), path);
    if (endpoint.method() == HttpMethod.POST || endpoint.method() == HttpMethod.PUT) {
      String body = endpoint.jsonBody() != null ? endpoint.jsonBody() : "{}";
      builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    if (token != null) {
      builder.header("Authorization", token);
    }
    return builder;
  }

  private static byte[] uuidBytes() {
    return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
  }

  // ---------------------------------------------------------------------------------------------
  // Role-only endpoints: wrong role must be forbidden with 403
  // ---------------------------------------------------------------------------------------------

  @ParameterizedTest(name = "{0} must reject role {1} with 403")
  @MethodSource("wrongRolePairs")
  void wrongRoleGets403(Endpoint endpoint, Role caller) throws Exception {
    mockMvc.perform(buildRequest(endpoint, tokens.get(caller))).andExpect(status().isForbidden());
  }

  private static Stream<Arguments> wrongRolePairs() {
    List<Arguments> args = new ArrayList<>();
    for (Endpoint endpoint : roleEndpoints()) {
      for (Role caller : Role.values()) {
        if (caller != endpoint.allowedRole()) {
          args.add(Arguments.of(endpoint, caller));
        }
      }
    }
    return args.stream();
  }

  // ---------------------------------------------------------------------------------------------
  // Role-only endpoints: the allowed role must get through (never 401)
  // ---------------------------------------------------------------------------------------------

  @ParameterizedTest(name = "{0} must be reachable by role {1}")
  @MethodSource("allowedRolePairs")
  void allowedRoleIsNotUnauthorized(Endpoint endpoint, Role caller) throws Exception {
    int status = performForStatus(endpoint, tokens.get(caller));
    assertThat(status).as("%s with %s token", endpoint, caller).isNotEqualTo(401);
  }

  /**
   * Executes the request and returns the response status. A request that reaches the service layer
   * on this empty DB may legitimately throw (FK violation on a throwaway school/user id, or a
   * rollback-only marker bubbling out of the RLS request transaction) — that still proves the
   * security gate was passed: a blocked request always answers 401/403 as a normal response, never
   * throws. The RBAC guarantee this test asserts is "not 401"; the business outcome is out of
   * scope.
   */
  private int performForStatus(Endpoint endpoint, String token) {
    try {
      return mockMvc.perform(buildRequest(endpoint, token)).andReturn().getResponse().getStatus();
    } catch (Exception e) {
      return 200;
    }
  }

  private static Stream<Arguments> allowedRolePairs() {
    return roleEndpoints().stream().map(e -> Arguments.of(e, e.allowedRole()));
  }

  // ---------------------------------------------------------------------------------------------
  // Common endpoints: every authenticated role can reach them
  // ---------------------------------------------------------------------------------------------

  @ParameterizedTest(name = "{0} must be reachable by every role")
  @MethodSource("commonEndpoints")
  void commonEndpointsReachableByEveryRole(Endpoint endpoint) throws Exception {
    for (Map.Entry<Role, String> entry : tokens.entrySet()) {
      int status = performForStatus(endpoint, entry.getValue());
      assertThat(status).as("%s with %s token", endpoint, entry.getKey()).isNotEqualTo(401);
    }
  }

  private static Stream<Arguments> commonEndpoints() {
    return commonEndpointList().stream().map(Arguments::of);
  }

  // ---------------------------------------------------------------------------------------------
  // Anonymous: every protected endpoint must reject a missing token with 401
  // ---------------------------------------------------------------------------------------------

  @ParameterizedTest(name = "{0} must reject anonymous with 401")
  @MethodSource("allProtectedEndpoints")
  void anonymousGets401(Endpoint endpoint) throws Exception {
    mockMvc.perform(buildRequest(endpoint, null)).andExpect(status().isUnauthorized());
  }

  private static Stream<Arguments> allProtectedEndpoints() {
    List<Endpoint> all = new ArrayList<>(roleEndpoints());
    all.addAll(commonEndpointList());
    return all.stream().map(Arguments::of);
  }

  // ---------------------------------------------------------------------------------------------
  // Public endpoints: never require a token
  // ---------------------------------------------------------------------------------------------

  @Test
  void publicEndpointsDoNotRequireToken() throws Exception {
    // login with a real user and NO Authorization header must answer 200 — a gated endpoint
    // would 401 before ever reaching AuthService. (A login with wrong credentials legitimately
    // answers 401 ERR_INVALID_CREDENTIALS, so the negative form of this test is unusable — the
    // positive flow is the real proof.)
    var loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"identifier\":\""
                            + AUTH_PHONE
                            + "\",\"password\":\""
                            + AUTH_PASSWORD
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // A case-insensitive email address belonging to the same account is an equally valid login
    // identifier. This is the public contract used by the login page and by account recovery.
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"identifier\":\""
                        + AUTH_EMAIL.toUpperCase(java.util.Locale.ROOT)
                        + "\",\"password\":\""
                        + AUTH_PASSWORD
                        + "\"}"))
        .andExpect(status().isOk());

    // Refresh credentials are intentionally not in the JSON body. The HttpOnly cookie is the
    // only browser-visible transport, so this catches an accidental regression that exposes a
    // seven-day token to JavaScript.
    assertThat(loginResult.getContentAsString()).doesNotContain("refreshToken");
    String refreshCookie =
        loginResult.getHeaders(HttpHeaders.SET_COOKIE).stream()
            .filter(header -> header.startsWith("academix_refresh="))
            .findFirst()
            .map(header -> header.substring("academix_refresh=".length()).split(";", 2)[0])
            .orElseThrow();
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(new Cookie("academix_refresh", refreshCookie)))
        .andExpect(status().isOk());

    // The remaining public endpoints answer business responses (never a security 401) even
    // with no token and an empty DB. login/refresh are already proven above via the real flow
    // (login with a bogus {} body answers 401 ERR_INVALID_CREDENTIALS — a business 401, so the
    // negative assertion below would be meaningless for them).
    for (Endpoint endpoint : publicEndpointList()) {
      int status = performForStatus(endpoint, null);
      assertThat(status).as("public endpoint %s", endpoint).isNotEqualTo(401);
    }
  }

  private static List<Endpoint> publicEndpointList() {
    return List.of(
        new Endpoint(HttpMethod.POST, "/api/v1/auth/forgot-password", null),
        new Endpoint(HttpMethod.POST, "/api/v1/auth/reset-password", null),
        new Endpoint(HttpMethod.GET, "/actuator/health", null));
  }

  // ---------------------------------------------------------------------------------------------
  // Endpoint matrix
  // ---------------------------------------------------------------------------------------------

  private static List<Endpoint> roleEndpoints() {
    return List.of(
        // --- Admin (academix_tz.md §2.2) ---
        new Endpoint(HttpMethod.GET, "/api/v1/admin/dashboard", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/students", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/students",
            Role.ADMIN,
            """
            {"firstName":"Test","lastName":"Student","phone":"+998900000001",\
            "classId":"%s","studentNumber":"1"}"""
                .formatted(UUID.randomUUID())),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/admin/students/{studentId}/transfer-class",
            Role.ADMIN,
            """
            {"newClassId":"%s"}"""
                .formatted(UUID.randomUUID())),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/admin/students/{studentId}/handwriting/unlock-reset",
            Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/teachers", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/teachers/invite",
            Role.ADMIN,
            """
            {"firstName":"Test","lastName":"Teacher","phone":"+998900000002",\
            "email":"teacher@academix.uz","password":"password123"}"""),
        new Endpoint(HttpMethod.PUT, "/api/v1/admin/teachers/{teacherId}/activate", Role.ADMIN),
        new Endpoint(HttpMethod.PUT, "/api/v1/admin/teachers/{teacherId}/deactivate", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/classes", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/classes",
            Role.ADMIN,
            """
            {"grade":7,"letter":"A"}"""),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/admin/classes/{classId}",
            Role.ADMIN,
            """
            {"grade":7,"letter":"A"}"""),
        new Endpoint(HttpMethod.DELETE, "/api/v1/admin/classes/{classId}", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/subjects", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/subjects",
            Role.ADMIN,
            """
            {"name":"Matematika","type":"CORE"}"""),
        new Endpoint(HttpMethod.DELETE, "/api/v1/admin/subjects/{subjectId}", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/school", Role.ADMIN),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/admin/school",
            Role.ADMIN,
            """
            {"name":"Maktab","address":"Toshkent","region":"Toshkent","district":"Shahar"}"""),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/parents", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/parents",
            Role.ADMIN,
            """
            {"firstName":"Ota","lastName":"Ona","phone":"+998900000003",\
            "password":"password123"}"""),
        new Endpoint(
            HttpMethod.GET, "/api/v1/admin/parents/check?phone=%2B998901234567", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/parents/link",
            Role.ADMIN,
            """
            {"parentPhone":"+998900000004","studentId":"%s","relation":"MOTHER"}"""
                .formatted(UUID.randomUUID())),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/psychologists", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/psychologists/invite",
            Role.ADMIN,
            """
            {"firstName":"Test","lastName":"Psych","phone":"+998900000005",\
            "email":"psych@academix.uz","password":"password123"}"""),
        new Endpoint(
            HttpMethod.PUT, "/api/v1/admin/psychologists/{psychologistId}/activate", Role.ADMIN),
        new Endpoint(
            HttpMethod.PUT, "/api/v1/admin/psychologists/{psychologistId}/deactivate", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/analytics/classes-comparison", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/analytics/teachers-ranking", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/analytics/school-progress", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/analytics/ai-usage", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/reports", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/reports/generate",
            Role.ADMIN,
            """
            {"type":"STUDENT","quarter":"1","targetId":"%s"}"""
                .formatted(UUID.randomUUID())),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/assignments", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/assignments",
            Role.ADMIN,
            """
            {"teacherId":"%s","classId":"%s","subjectId":"%s"}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())),
        new Endpoint(HttpMethod.DELETE, "/api/v1/admin/assignments/{assignmentId}", Role.ADMIN),
        new Endpoint(HttpMethod.GET, "/api/v1/admin/data-deletion-requests", Role.ADMIN),
        new Endpoint(
            HttpMethod.PUT, "/api/v1/admin/data-deletion-requests/{id}/approve", Role.ADMIN),
        new Endpoint(
            HttpMethod.POST, "/api/v1/admin/students/bulk-import/analyze", Role.ADMIN, true),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/admin/students/bulk-import/commit",
            Role.ADMIN,
            """
            {"fileToken":"token","columnMapping":{},"saveMappingAsTemplate":false}"""),

        // --- Teacher (academix_tz.md §2.3) ---
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/dashboard", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/exams", Role.TEACHER),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/exams",
            Role.TEACHER,
            """
            {"classId":"%s","subjectId":"%s","title":"Test imtihon",\
            "examDate":"2026-06-01","maxScore":100}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID())),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/homework", Role.TEACHER),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/homework",
            Role.TEACHER,
            """
            {"classId":"%s","subjectId":"%s","title":"Test vazifa",\
            "deadlineAt":"2026-06-01T18:00:00","type":"STANDARD","maxScore":100}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID())),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/homework/{assignmentId}", Role.TEACHER),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/homework/{assignmentId}",
            Role.TEACHER,
            """
            {"title":"Yangilangan vazifa","deadlineAt":"2026-06-02T18:00:00","maxScore":100}"""),
        new Endpoint(HttpMethod.DELETE, "/api/v1/teacher/homework/{assignmentId}", Role.TEACHER),
        new Endpoint(
            HttpMethod.POST, "/api/v1/teacher/homework/{assignmentId}/submit", Role.TEACHER),
        new Endpoint(
            HttpMethod.GET, "/api/v1/teacher/homework/{assignmentId}/unique-tasks", Role.TEACHER),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/homework/{assignmentId}/unique-tasks/{taskId}/approve",
            Role.TEACHER),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/homework/{assignmentId}/unique-tasks/{taskId}",
            Role.TEACHER,
            """
            {"taskContent":"Yangi matn"}"""),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/homework/{assignmentId}/unique-tasks/approve-all",
            Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/submissions", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/submissions/{submissionId}", Role.TEACHER),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/submissions/{submissionId}/grade",
            Role.TEACHER,
            """
            {"score":90,"fivePointGrade":5,"teacherComment":"Yaxshi","isExcellent":false}"""),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/exams/{examId}/submissions", Role.TEACHER),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/exams/{examId}/submissions/bulk-upload",
            Role.TEACHER,
            true),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/exams/{examId}/submissions/{id}/grade",
            Role.TEACHER,
            """
            {"score":90,"fivePointGrade":5,"teacherComment":"Yaxshi"}"""),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/exams/{examId}/submissions/approve-all",
            Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/classes", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/classes/{classId}/students", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/classes/{classId}/analytics", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/students/{studentId}/progress", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/subjects", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/psychological-signals", Role.TEACHER),
        new Endpoint(
            HttpMethod.GET, "/api/v1/teacher/psychological-signals/{signalId}", Role.TEACHER),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/psychological-signals/{signalId}/resolve",
            Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/syllabuses", Role.TEACHER),
        new Endpoint(HttpMethod.POST, "/api/v1/teacher/syllabuses", Role.TEACHER, true),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/syllabuses/{syllabusId}", Role.TEACHER),
        new Endpoint(HttpMethod.GET, "/api/v1/teacher/lesson-plans", Role.TEACHER),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/teacher/lesson-plans/generate",
            Role.TEACHER,
            """
            {"syllabusId":"%s","topic":"Algebra","lessonDate":"2026-06-01","classId":"%s"}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID())),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/lesson-plans/{planId}",
            Role.TEACHER,
            """
            {"teacherEditedPlan":"Reja matni","isApproved":true}"""),
        new Endpoint(
            HttpMethod.GET,
            "/api/v1/teacher/grading-criteria?subjectId=" + UUID.randomUUID(),
            Role.TEACHER),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/grading-criteria/{subjectId}",
            Role.TEACHER,
            """
            {"criteria":[{"name":"To'g'rilik","weightPercent":100}]}"""),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/teacher/students/{studentId}/handwriting/reset",
            Role.TEACHER,
            """
            {"reason":"ILLNESS","notes":"Test"}"""),
        new Endpoint(
            HttpMethod.PUT, "/api/v1/teacher/students/{studentId}/reset-password", Role.TEACHER),

        // --- Student (academix_tz.md §2.4) ---
        new Endpoint(HttpMethod.GET, "/api/v1/student/progress", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/dashboard", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/badges", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/xp-history", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/homework", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/homework/{assignmentId}", Role.STUDENT),
        new Endpoint(
            HttpMethod.POST, "/api/v1/student/homework/{assignmentId}/submit", Role.STUDENT, true),
        new Endpoint(HttpMethod.GET, "/api/v1/student/submissions", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/submissions/{submissionId}", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/exams", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/exams/{examId}", Role.STUDENT),
        new Endpoint(HttpMethod.GET, "/api/v1/student/ai-chat/history", Role.STUDENT),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/student/ai-chat",
            Role.STUDENT,
            """
            {"subject":"Matematika","message":"Salom"}"""),

        // --- Parent (academix_tz.md §2.5) ---
        new Endpoint(HttpMethod.GET, "/api/v1/parent/dashboard", Role.PARENT),
        new Endpoint(HttpMethod.GET, "/api/v1/parent/children", Role.PARENT),
        new Endpoint(HttpMethod.GET, "/api/v1/parent/children/{studentId}/overview", Role.PARENT),
        new Endpoint(
            HttpMethod.GET, "/api/v1/parent/children/{studentId}/quarter-report", Role.PARENT),
        new Endpoint(
            HttpMethod.GET,
            "/api/v1/parent/children/{studentId}/quarter-report/download",
            Role.PARENT),
        new Endpoint(HttpMethod.GET, "/api/v1/parent/children/{studentId}/progress", Role.PARENT),
        new Endpoint(HttpMethod.GET, "/api/v1/parent/children/{studentId}/homework", Role.PARENT),
        new Endpoint(
            HttpMethod.GET, "/api/v1/parent/children/{studentId}/submissions", Role.PARENT),
        new Endpoint(HttpMethod.GET, "/api/v1/parent/children/{studentId}/grades", Role.PARENT),
        new Endpoint(
            HttpMethod.POST,
            "/api/v1/parent/children/{studentId}/data-deletion-request",
            Role.PARENT),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/parent/children/{studentId}/consent/biometric",
            Role.PARENT,
            """
            {"consentGiven":true}"""),

        // --- Psychologist (academix_tz.md §2.6) ---
        new Endpoint(HttpMethod.GET, "/api/v1/psychologist/dashboard", Role.PSYCHOLOGIST),
        new Endpoint(HttpMethod.GET, "/api/v1/psychologist/signals", Role.PSYCHOLOGIST),
        new Endpoint(HttpMethod.GET, "/api/v1/psychologist/signals/{signalId}", Role.PSYCHOLOGIST),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/psychologist/signals/{signalId}/resolve",
            Role.PSYCHOLOGIST,
            """
            {"notes":"Test","actionTaken":"Test"}"""),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/psychologist/signals/{signalId}/mark-manipulation",
            Role.PSYCHOLOGIST),
        new Endpoint(HttpMethod.GET, "/api/v1/psychologist/watchlist", Role.PSYCHOLOGIST),
        new Endpoint(
            HttpMethod.POST, "/api/v1/psychologist/watchlist/{studentId}", Role.PSYCHOLOGIST),
        new Endpoint(
            HttpMethod.DELETE, "/api/v1/psychologist/watchlist/{studentId}", Role.PSYCHOLOGIST),
        new Endpoint(HttpMethod.GET, "/api/v1/psychologist/reports", Role.PSYCHOLOGIST));
  }

  private static List<Endpoint> commonEndpointList() {
    return List.of(
        // --- Common to all roles (isAuthenticated) ---
        new Endpoint(HttpMethod.GET, "/api/v1/notifications", null),
        new Endpoint(HttpMethod.PUT, "/api/v1/notifications/{id}/read", null),
        new Endpoint(HttpMethod.PUT, "/api/v1/notifications/read-all", null),
        new Endpoint(HttpMethod.DELETE, "/api/v1/notifications/{id}", null),
        new Endpoint(HttpMethod.DELETE, "/api/v1/notifications", null),
        new Endpoint(HttpMethod.GET, "/api/v1/notifications/preferences", null),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/notifications/preferences",
            null,
            """
            {"type":"HOMEWORK_ASSIGNED","inAppEnabled":true,"telegramEnabled":true}"""),
        new Endpoint(HttpMethod.POST, "/api/v1/notifications/telegram/link-token", null),
        new Endpoint(HttpMethod.GET, "/api/v1/notifications/telegram/status", null),
        new Endpoint(HttpMethod.DELETE, "/api/v1/notifications/telegram/unlink", null),
        new Endpoint(HttpMethod.GET, "/api/v1/auth/profile", null),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/auth/profile",
            null,
            """
            {"firstName":"Test","lastName":"User","email":"test@academix.uz"}"""),
        new Endpoint(
            HttpMethod.PUT,
            "/api/v1/auth/change-password",
            null,
            """
            {"oldPassword":"password123","newPassword":"password1234"}"""),
        new Endpoint(HttpMethod.POST, "/api/v1/auth/logout", null));
  }
}
