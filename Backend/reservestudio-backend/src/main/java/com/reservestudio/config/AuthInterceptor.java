package com.reservestudio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reservestudio.dto.Dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/** Oturumu olmayan istekleri 401 ile reddeder; geçerliyse userId'yi request'e koyar. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_ID = "USER_ID";
    public static final String REQUEST_USER_ID = "userId";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true; // CORS preflight
        }
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(SESSION_USER_ID);

        if (userId == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                    401, "Unauthorized", "Bu işlem için giriş yapmalısınız", LocalDateTime.now()));
            return false;
        }
        request.setAttribute(REQUEST_USER_ID, userId);
        return true;
    }
}
