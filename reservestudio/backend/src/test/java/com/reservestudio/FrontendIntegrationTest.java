package com.reservestudio;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Frontend ile entegrasyon: CORS (cookie'li istekler), 404 davranışı ve arayüz dosyalarının sunulması. */
@SpringBootTest
@AutoConfigureMockMvc
class FrontendIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private void assertPreflightAllowed(String origin, String method) throws Exception {
        mockMvc.perform(options("/api/bookings")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", method)
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflightFromLocalDevServersIsAllowedWithCredentials() throws Exception {
        assertPreflightAllowed("http://localhost:5500", "POST");      // VS Code Live Server
        assertPreflightAllowed("http://localhost:8000", "POST");      // python3 -m http.server
        assertPreflightAllowed("http://127.0.0.1:5173", "DELETE");    // vite
        assertPreflightAllowed("http://localhost:3000", "POST");
    }

    @Test
    void preflightFromForeignOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/bookings")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownPathReturns404JsonInsteadOf500() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void frontendPagesAreServedFromBackend() throws Exception {
        // ../frontend klasörü yoksa (backend tek başına derlendiyse) bu test atlanır
        Assumptions.assumeTrue(getClass().getResource("/static/index.html") != null,
                "frontend klasörü bulunamadı, test atlandı");

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ReserveStudio")));
        for (String page : new String[]{"/signup.html", "/booking.html", "/dashboard.html",
                "/api.js", "/booking.js", "/style.css"}) {
            mockMvc.perform(get(page)).andExpect(status().isOk());
        }
    }
}
