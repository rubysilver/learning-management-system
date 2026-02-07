package com.krzelj.lms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LmsE2ETest {

    @LocalServerPort
    int port;

    private String base;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        base = "http://localhost:" + port;
        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private HttpResponse<String> loginAndGetDashboard(String username, String password) throws Exception {
        String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode()).isEqualTo(302);

        HttpRequest dashboardRequest = HttpRequest.newBuilder()
                .uri(URI.create(base + "/dashboard"))
                .GET()
                .build();

        return client.send(dashboardRequest, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void loginThenDashboardRedirectsToStudentDashboard() throws Exception {
        HttpResponse<String> response = loginAndGetDashboard("student", "student123");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElse("")).endsWith("/dashboard/student");
    }

    @Test
    void loginAsInstructorThenDashboardRedirectsToInstructorDashboard() throws Exception {
        HttpResponse<String> response = loginAndGetDashboard("instructor", "instructor123");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElse("")).endsWith("/dashboard/instructor");
    }

    @Test
    void loginAsAdminThenDashboardRedirectsToAdminDashboard() throws Exception {
        HttpResponse<String> response = loginAndGetDashboard("admin", "admin123");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElse("")).endsWith("/dashboard/admin");
    }
}
