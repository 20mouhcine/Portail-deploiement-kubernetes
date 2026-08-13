package org.example.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    public static final String RESOLVED_TOKEN_ATTRIBUTE =
            SpaCsrfTokenRequestHandler.class.getName() + ".RESOLVED_TOKEN";

    private final CsrfTokenRequestHandler plain =
            new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor =
            new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> deferredCsrfToken) {
        CsrfToken csrfToken = deferredCsrfToken.get();
        request.setAttribute(RESOLVED_TOKEN_ATTRIBUTE, csrfToken);
        xor.handle(request, response, () -> csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(
            HttpServletRequest request,
            CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        CsrfTokenRequestHandler delegate = StringUtils.hasText(headerValue)
                ? plain
                : xor;
        return delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
