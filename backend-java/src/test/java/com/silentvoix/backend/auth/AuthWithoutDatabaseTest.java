package com.silentvoix.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** No database configured: accounts cannot work, and the app is told so plainly (503). */
@SpringBootTest(properties = "silentvoix.database.url=")
@AutoConfigureMockMvc
class AuthWithoutDatabaseTest {

    @Autowired
    MockMvc mvc;

    @Test
    void signInAnswers503() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"lan@example.com\", \"password\": \"mat-khau-dai\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("database_unavailable"));
    }

    @Test
    void aSignedInCallAnswers503RatherThan401() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer some-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("database_unavailable"));
    }

    @Test
    void aMalformedBodyIsA400WithACode() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
