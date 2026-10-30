package com.reservestudio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndBookingApiTest {

    @Autowired
    MockMvc mockMvc;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String slotJson(int hour, int daysAhead) {
        LocalDateTime slot = LocalDate.now().plusDays(daysAhead).atTime(hour, 0);
        return "{\"slotStart\":\"" + slot + "\"}";
    }

    @Test
    void signupLoginAndMeFlow() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123");
        MockHttpSession session = login(email, "password123");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Other\",\"email\":\"" + email.toUpperCase() + "\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bookingWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson(10, 5)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void secondBookingOfSameSlotReturns409() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        signup(emailA, "password123");
        signup(emailB, "password123");
        MockHttpSession a = login(emailA, "password123");
        MockHttpSession b = login(emailB, "password123");

        mockMvc.perform(post("/api/bookings").session(a)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(11, 6)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings").session(b)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(11, 6)))
                .andExpect(status().isConflict());
    }

    @Test
    void slotShowsUnavailableAfterBooking() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123");
        MockHttpSession session = login(email, "password123");
        LocalDate date = LocalDate.now().plusDays(7);

        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(14, 7)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/slots").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[5].available").value(false))  // 14:00 -> 6. slot (09 + 5)
                .andExpect(jsonPath("$[4].available").value(true));  // 13:00
    }

    @Test
    void invalidSlotsReturn400() throws Exception {
        String email = uniqueEmail();
        signup(email, "password123");
        MockHttpSession session = login(email, "password123");

        // Saat başı değil
        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotStart\":\"" + LocalDate.now().plusDays(3).atTime(10, 30) + "\"}"))
                .andExpect(status().isBadRequest());
        // Çalışma saati dışı
        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(23, 3)))
                .andExpect(status().isBadRequest());
        // Geçmiş
        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(10, -1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelFreesSlotAndOnlyOwnerCanCancel() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        signup(emailA, "password123");
        signup(emailB, "password123");
        MockHttpSession a = login(emailA, "password123");
        MockHttpSession b = login(emailB, "password123");

        MvcResult created = mockMvc.perform(post("/api/bookings").session(a)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(15, 9)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        // Başkasının rezervasyonunu iptal edemez
        mockMvc.perform(delete("/api/bookings/" + id).session(b))
                .andExpect(status().isForbidden());

        // Sahibi iptal eder
        mockMvc.perform(delete("/api/bookings/" + id).session(a))
                .andExpect(status().isNoContent());

        // Slot tekrar alınabilir
        mockMvc.perform(post("/api/bookings").session(b)
                        .contentType(MediaType.APPLICATION_JSON).content(slotJson(15, 9)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/bookings/my").session(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
