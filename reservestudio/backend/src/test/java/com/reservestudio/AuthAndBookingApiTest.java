package com.reservestudio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservestudio.model.Slot;
import com.reservestudio.repository.SlotRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndBookingApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    SlotRepository slotRepository;

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String signup(String suffix, String password) throws Exception {
        String email = "user-" + suffix + "@test.com";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user" + suffix + "\",\"email\":\"" + email
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Kayıt + giriş; oturumu döner. */
    private MockHttpSession newLoggedInUser() throws Exception {
        String email = signup(uniqueSuffix(), "password123");
        return login(email, "password123");
    }

    /** GET /api/slots ile verilen gün/saatteki slotun id'sini bulur. */
    private long slotId(int daysAhead, int hour) throws Exception {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        String body = mockMvc.perform(get("/api/slots").param("date", date.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String expectedStart = date.atTime(hour, 0).toString();
        for (JsonNode node : objectMapper.readTree(body)) {
            if (node.get("start").asText().startsWith(expectedStart)) {
                return node.get("id").asLong();
            }
        }
        throw new AssertionError("Slot bulunamadı: " + expectedStart);
    }

    private String bookingJson(long slotId) {
        return "{\"slotId\":" + slotId + "}";
    }

    @Test
    void signupLoginAndMeFlow() throws Exception {
        String suffix = uniqueSuffix();
        String email = signup(suffix, "password123");
        MockHttpSession session = login(email, "password123");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value("user" + suffix))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void duplicateEmailAndUsernameReturn409() throws Exception {
        String suffix = uniqueSuffix();
        String email = signup(suffix, "password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"other" + suffix + "\",\"email\":\"" + email.toUpperCase()
                                + "\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user" + suffix + "\",\"email\":\"another-" + suffix
                                + "@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        String suffix = uniqueSuffix();
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user" + suffix + "\",\"email\":\"x-" + suffix
                                + "@test.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = signup(uniqueSuffix(), "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bookingWithoutLoginReturns401() throws Exception {
        long id = slotId(5, 10);
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(id)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void secondBookingOfSameSlotReturns409() throws Exception {
        MockHttpSession a = newLoggedInUser();
        MockHttpSession b = newLoggedInUser();
        long id = slotId(6, 11);

        mockMvc.perform(post("/api/bookings").session(a)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.slotId").value(id));

        mockMvc.perform(post("/api/bookings").session(b)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(id)))
                .andExpect(status().isConflict());
    }

    @Test
    void slotListShowsTwelveSlotsAndMarksBookedOneUnavailable() throws Exception {
        MockHttpSession session = newLoggedInUser();
        LocalDate date = LocalDate.now().plusDays(7);
        long id = slotId(7, 14);

        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(id)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/slots").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))           // 09:00 ... 20:00
                .andExpect(jsonPath("$[5].available").value(false))  // 14:00
                .andExpect(jsonPath("$[4].available").value(true));  // 13:00
    }

    @Test
    void unknownPastAndOutOfWindowRequests() throws Exception {
        MockHttpSession session = newLoggedInUser();

        // Olmayan slot -> 404
        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(999_999_999L)))
                .andExpect(status().isNotFound());

        // Geçmiş slot -> 400
        Slot past = slotRepository.save(new Slot(
                LocalDate.now().minusDays(2).atTime(10, 0), LocalDate.now().minusDays(2).atTime(11, 0)));
        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(past.getId())))
                .andExpect(status().isBadRequest());

        // slotId eksik -> 400
        mockMvc.perform(post("/api/bookings").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // Rezervasyon penceresi dışındaki gün -> 400
        mockMvc.perform(get("/api/slots").param("date", LocalDate.now().plusDays(365).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelFreesSlotAndOnlyOwnerCanCancel() throws Exception {
        MockHttpSession a = newLoggedInUser();
        MockHttpSession b = newLoggedInUser();
        long id = slotId(9, 15);

        MvcResult created = mockMvc.perform(post("/api/bookings").session(a)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(id)))
                .andExpect(status().isCreated())
                .andReturn();
        long bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertTrue(bookingId > 0);

        // Başkasının rezervasyonunu iptal edemez
        mockMvc.perform(delete("/api/bookings/" + bookingId).session(b))
                .andExpect(status().isForbidden());

        // Sahibi iptal eder
        mockMvc.perform(delete("/api/bookings/" + bookingId).session(a))
                .andExpect(status().isNoContent());

        // Slot tekrar alınabilir
        mockMvc.perform(post("/api/bookings").session(b)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(id)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/bookings/my").session(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/bookings/my").session(b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
