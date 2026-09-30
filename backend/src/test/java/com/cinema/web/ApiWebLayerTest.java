package com.cinema.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.auth.JsonSecurityErrorHandler;
import com.cinema.auth.JwtService;
import com.cinema.auth.SecurityConfig;
import com.cinema.movie.AdminMovieController;
import com.cinema.movie.MovieService;
import com.cinema.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = AdminMovieController.class, properties = {
        "app.jwt.secret=web-layer-test-secret-Zr4Kx8Qm2Nv6Tp9L-web",
        "app.rate-limit.enabled=false",
        "app.cors.allowed-origins=http://localhost:3000"
})
@Import({ SecurityConfig.class, JwtService.class, JsonSecurityErrorHandler.class, ApiWebLayerTest.ProbeController.class })
@ExtendWith(OutputCaptureExtension.class)
class ApiWebLayerTest {
    private static final String SECRET = "web-layer-test-secret-Zr4Kx8Qm2Nv6Tp9L-web";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private StringRedisTemplate redis;

    @Test
    void missingTokenReturnsJson401() throws Exception {
        mvc.perform(get("/admin/movies"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void expiredTokenReturns401SoTheClientRefreshes() throws Exception {
        JwtService expiredIssuer = new JwtService(objectMapper, SECRET, -1);
        String expired = expiredIssuer.issue(user(UserRole.ADMIN));

        mvc.perform(get("/admin/movies").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(jsonPath("$.message").value("The access token is invalid or has expired."));
    }

    @Test
    void garbageTokenReturns401() throws Exception {
        mvc.perform(get("/notifications").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void customerOnAdminRouteReturnsJson403() throws Exception {
        mvc.perform(get("/admin/movies").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have access to this resource."));
    }

    @Test
    void adminCanReachAdminRoute() throws Exception {
        when(movieService.listAll()).thenReturn(List.of());

        mvc.perform(get("/admin/movies").header("Authorization", bearer(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/admin/movies").header("Authorization", bearer(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON."));
    }

    @Test
    void wrongJsonTypeNamesTheField() throws Exception {
        mvc.perform(post("/probe/echo").header("Authorization", bearer(UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"count\":\"many\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field 'count' has an invalid value."));
    }

    @Test
    void beanValidationFailureReturns400() throws Exception {
        mvc.perform(post("/probe/echo").header("Authorization", bearer(UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"count\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("count")));
    }

    @Test
    void badUuidReturns400() throws Exception {
        mvc.perform(put("/admin/movies/not-a-uuid").header("Authorization", bearer(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be a valid UUID."));
    }

    @Test
    void missingHeaderAndParameterReturn400() throws Exception {
        mvc.perform(get("/probe/header").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required header 'X-Thing' is missing."));
        mvc.perform(get("/probe/page").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'page' is missing."));
        mvc.perform(get("/probe/page").param("page", "abc").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'page' has an invalid value."));
    }

    @Test
    void methodParameterConstraintReturns400() throws Exception {
        mvc.perform(get("/probe/positive/0").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        mvc.perform(get("/movies/a/b/c"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/no-such-endpoint").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint matches this path."));
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mvc.perform(delete("/admin/movies").header("Authorization", bearer(UserRole.ADMIN)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mvc.perform(post("/admin/movies").header("Authorization", bearer(UserRole.ADMIN))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void dataIntegrityViolationReturns409WithoutLeakingSql() throws Exception {
        mvc.perform(post("/probe/conflict").header("Authorization", bearer(UserRole.CUSTOMER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data."))
                .andExpect(content().string(not(containsString("uq_internal_secret"))));
    }

    @Test
    void unexpectedErrorIsLoggedWithStackTraceAndRequestId(CapturedOutput output) throws Exception {
        mvc.perform(get("/probe/boom").header("Authorization", bearer(UserRole.CUSTOMER)).header("X-Request-Id", "trace-boom-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error."))
                .andExpect(jsonPath("$.requestId").value("trace-boom-1"))
                .andExpect(content().string(not(containsString("internal detail"))));

        assertThat(output.getOut())
                .contains("Unhandled exception for GET /probe/boom")
                .contains("java.lang.IllegalStateException: internal detail 42")
                .contains("[trace-boom-1]");
    }

    @Test
    void echoesValidInboundRequestId() throws Exception {
        MvcResult result = mvc.perform(get("/admin/movies").header("X-Request-Id", "client-req.42"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "client-req.42"))
                .andExpect(jsonPath("$.requestId").value("client-req.42"))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo("client-req.42");
    }

    @Test
    void replacesUnsafeInboundRequestId() throws Exception {
        MvcResult result = mvc.perform(get("/admin/movies").header("X-Request-Id", "bad id<script>"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String generated = result.getResponse().getHeader("X-Request-Id");

        assertThat(generated).isNotEqualTo("bad id<script>");
        assertThat(UUID.fromString(generated)).isNotNull();
        assertThat(result.getResponse().getContentAsString()).contains(generated);

        String tooLong = "a".repeat(129);
        assertThat(mvc.perform(get("/admin/movies").header("X-Request-Id", tooLong)).andReturn()
                .getResponse().getHeader("X-Request-Id")).isNotEqualTo(tooLong);
    }

    @Test
    void generatesRequestIdOnSuccessfulResponses() throws Exception {
        when(movieService.listAll()).thenReturn(List.of());

        mvc.perform(get("/admin/movies").header("Authorization", bearer(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void corsExposesRequestIdHeader() throws Exception {
        when(movieService.listAll()).thenReturn(List.of());

        mvc.perform(get("/admin/movies").header("Authorization", bearer(UserRole.ADMIN)).header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Request-Id")));
    }

    @Test
    void paymentWebhookIsReachableWithoutTokenButMockCallbackIsNot() throws Exception {
        mvc.perform(post("/payments/webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/payments/mock-callback").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorIsNotPubliclyExposedBeyondHealth() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env").header("Authorization", bearer(UserRole.ADMIN)))
                .andExpect(status().isForbidden());
    }

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issue(user(role));
    }

    private AuthUser user(UserRole role) {
        return new AuthUser(UUID.randomUUID(), "Web Test", role.name().toLowerCase() + "@example.com", role);
    }

    record EchoRequest(@NotBlank String name, @Min(1) int count) {
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {
        @PostMapping("/echo")
        EchoRequest echo(@Valid @RequestBody EchoRequest request) {
            return request;
        }

        @GetMapping("/header")
        String header(@RequestHeader("X-Thing") String thing) {
            return thing;
        }

        @GetMapping("/page")
        int page(@RequestParam int page) {
            return page;
        }

        @GetMapping("/positive/{value}")
        int positive(@PathVariable @Min(1) int value) {
            return value;
        }

        @PostMapping("/conflict")
        void conflict() {
            throw new DataIntegrityViolationException(
                    "could not execute statement [ERROR: duplicate key value violates unique constraint \"uq_internal_secret\"]");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("internal detail 42");
        }
    }
}
