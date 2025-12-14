package com.analytics.config;

import com.analytics.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String EVENTS_PATH = "/api/v1/events";
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(EVENTS_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr();
        if (!rateLimiterService.allowRequest(key, Instant.now())) {
            log.warn("Rate limited request ip={} path={} body={}", key, request.getRequestURI(), readBody(request));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String readBody(HttpServletRequest request) {
        try {
            byte[] buffer = request.getInputStream().readAllBytes();
            Charset charset = request.getCharacterEncoding() != null
                    ? Charset.forName(request.getCharacterEncoding())
                    : StandardCharsets.UTF_8;
            return new String(buffer, charset);
        } catch (Exception ex) {
            return "<unreadable>";
        }
    }
}
