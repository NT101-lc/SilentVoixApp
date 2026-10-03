package com.silentvoix.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/auth}: register and login are open; me and logout need
 * {@code Authorization: Bearer <token>}. Errors are {@code {"error": "<code>"}}.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    /** Extra fields (a "role", say) are ignored: everyone who registers is an ordinary user. */
    record RegisterRequest(String email, String password, String displayName, String platform, String appVersion) {
    }

    record LoginRequest(String email, String password, String platform, String appVersion) {
    }

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthService.Session register(@RequestBody RegisterRequest request) {
        return auth.register(request.email(), request.password(), request.displayName(), request.platform(),
                request.appVersion());
    }

    @PostMapping("/login")
    AuthService.Session login(@RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password(), request.platform(), request.appVersion());
    }

    @GetMapping("/me")
    Account me(CurrentUser user) {
        return user.account();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(CurrentUser user) {
        auth.logout(user);
    }
}
