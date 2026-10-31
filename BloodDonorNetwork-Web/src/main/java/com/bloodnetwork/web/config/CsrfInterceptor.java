package com.bloodnetwork.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor providing Anti-Cross-Site Request Forgery (CSRF) protection
 * for all state-changing HTTP operations (POST, PUT, DELETE, PATCH).
 * Generates and verifies session-bound cryptographic tokens.
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    public static final String CSRF_SESSION_KEY = "CSRF_TOKEN";
    public static final String CSRF_PARAM_NAME = "_csrf";
    public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(true);

        // 1. Ensure token exists in session
        String sessionToken = (String) session.getAttribute(CSRF_SESSION_KEY);
        if (sessionToken == null) {
            sessionToken = UUID.randomUUID().toString();
            session.setAttribute(CSRF_SESSION_KEY, sessionToken);
        }

        // Expose token to request attributes for Thymeleaf templates
        request.setAttribute(CSRF_PARAM_NAME, sessionToken);

        // 2. Validate token on state-changing requests
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) 
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {

            // Permit login submission if token is empty only on first access, but still prefer token
            String requestToken = request.getParameter(CSRF_PARAM_NAME);
            if (requestToken == null || requestToken.isEmpty()) {
                requestToken = request.getHeader(CSRF_HEADER_NAME);
            }

            if (requestToken == null || !requestToken.equals(sessionToken)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token.");
                return false;
            }
        }

        return true;
    }
}
