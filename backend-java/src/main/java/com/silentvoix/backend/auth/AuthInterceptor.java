package com.silentvoix.backend.auth;

import com.silentvoix.backend.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards every {@code /api/v1} call except health, register and login: it needs a live session
 * ({@code 401} otherwise), and {@code /api/v1/admin/**} also needs the admin role ({@code 403}).
 * The role is read from the database on each call, never trusted from the app.
 */
class AuthInterceptor implements HandlerInterceptor {

    static final String CURRENT_USER = CurrentUser.class.getName();
    private static final String BEARER = "Bearer ";
    private static final String ADMIN_PATHS = "/api/v1/admin/";

    private final AuthService auth;

    AuthInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw ApiException.unauthorized();
        }
        String token = header.substring(BEARER.length()).trim();
        CurrentUser user = token.isEmpty() ? null : auth.authenticate(token);
        if (user == null) {
            throw ApiException.unauthorized();
        }
        if (request.getRequestURI().startsWith(ADMIN_PATHS) && !user.isAdmin()) {
            throw ApiException.forbidden();
        }
        request.setAttribute(CURRENT_USER, user);
        return true;
    }
}
