package com.bloodnetwork.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Server-side authorization interceptor enforcing strict role-based access control,
 * session validity checks, no-store cache headers for protected portals,
 * and automatic dashboard redirection for already authenticated users.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        String role = (session != null) ? (String) session.getAttribute("role") : null;

        // 1. Redirection for already logged-in users accessing /login or /register
        if ((uri.equals("/login") || uri.equals("/register")) && role != null) {
            if ("ADMIN".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return false;
            } else if ("REQUESTER".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + "/hospital/dashboard");
                return false;
            } else if ("DONOR".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + "/donor/dashboard");
                return false;
            }
        }

        // 2. Protected Admin Routes (/admin/**)
        if (uri.startsWith("/admin")) {
            if (role == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthorized");
                return false;
            }
            if (!"ADMIN".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + "/login?error=forbidden");
                return false;
            }
            applyNoCacheHeaders(response);
        }

        // 3. Protected Hospital Requester Routes (/hospital/**)
        if (uri.startsWith("/hospital")) {
            if (role == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthorized");
                return false;
            }
            if (!"REQUESTER".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + "/login?error=forbidden");
                return false;
            }
            applyNoCacheHeaders(response);
        }

        // 4. Protected Donor Routes (/donor/**)
        if (uri.startsWith("/donor")) {
            if (role == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthorized");
                return false;
            }
            if (!"DONOR".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + "/login?error=forbidden");
                return false;
            }
            applyNoCacheHeaders(response);
        }

        // 5. Protected Account Routes (/account/**)
        if (uri.startsWith("/account")) {
            if (role == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthorized");
                return false;
            }
            applyNoCacheHeaders(response);
        }

        return true;
    }

    private void applyNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}
