package com.shopflow.starter.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reprend le header {@code X-Correlation-Id} (posé par la gateway) ou en crée un, le place dans le MDC
 * pendant le traitement de la requête et le renvoie dans la réponse.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = Correlation.sanitizeOrNew(request.getHeader(Correlation.HTTP_HEADER));
        Correlation.set(correlationId);
        response.setHeader(Correlation.HTTP_HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            Correlation.clear();
        }
    }
}
