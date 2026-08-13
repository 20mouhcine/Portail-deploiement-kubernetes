package org.example.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.auth.dto.CsrfResponse;
import org.example.auth.dto.UserResponse;
import org.example.auth.security.SpaCsrfTokenRequestHandler;
import org.example.auth.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    @GetMapping("/csrf")
    public CsrfResponse csrf(
            HttpServletRequest request,
            HttpServletResponse response) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(
                SpaCsrfTokenRequestHandler.RESOLVED_TOKEN_ATTRIBUTE
        );
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.loadDeferredToken(request, response).get();
        }
        return new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        );
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return userService.getByUsername(authentication.getName());
    }
}
