package com.opa.demo.filter;

import com.opa.demo.service.OpaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(100)
public class OpaAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(OpaAuthorizationFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    
    private final OpaService opaService;

    public OpaAuthorizationFilter(OpaService opaService) {
        this.opaService = opaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Add request ID for tracing
        String requestId = getOrGenerateRequestId(request);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        
        try {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            
            logger.debug("Processing request: {} {}", method, uri);
            
            // Skip authorization for public endpoints and health checks
            if (shouldSkipAuthorization(uri)) {
                logger.debug("Skipping authorization for URI: {}", uri);
                filterChain.doFilter(request, response);
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.debug("Authentication object: {}", auth);
            
            if (auth == null) {
                logger.warn("No authentication found for protected endpoint: {}", uri);
                filterChain.doFilter(request, response); // Let Spring Security handle this
                return;
            }
            
            if (!(auth.getPrincipal() instanceof Jwt jwt)) {
                logger.warn("Authentication principal is not a JWT for URI: {}, principal type: {}", 
                    uri, auth.getPrincipal().getClass().getSimpleName());
                filterChain.doFilter(request, response); // Let Spring Security handle this
                return;
            }

            String username = extractUsername(jwt);
            if (username == null) {
                logger.warn("Unable to extract username from JWT for URI: {}", uri);
                sendUnauthorizedResponse(response, "Invalid token");
                return;
            }
            
            String action = determineAction(method);
            String resource = extractResource(uri);
            
            logger.info("Authorizing user: {} for action: {} on resource: {}", username, action, resource);
            
            boolean allowed = opaService.isAllowed(username, action, resource);
            
            if (!allowed) {
                logger.warn("Access denied for user: {} on resource: {}", username, resource);
                sendForbiddenResponse(response, "Access denied by policy");
                return;
            }
            
            logger.debug("Authorization successful for user: {}", username);
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error during authorization check", e);
            sendInternalErrorResponse(response, "Authorization check failed");
        } finally {
            MDC.clear();
        }
    }
    
    private boolean shouldSkipAuthorization(String uri) {
        return uri.startsWith("/api/public/") || 
               uri.startsWith("/actuator/") ||
               uri.startsWith("/health") ||
               uri.equals("/api/check-access") ||
               uri.equals("/");
    }
    
    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
    
    private String extractUsername(Jwt jwt) {
        try {
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null || username.isEmpty()) {
                username = jwt.getClaimAsString("sub");
            }
            if (username == null || username.isEmpty()) {
                username = jwt.getClaimAsString("name");
            }
            return username;
        } catch (Exception e) {
            logger.warn("Failed to extract username from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    private String determineAction(String method) {
        return switch (method.toUpperCase()) {
            case "GET", "HEAD", "OPTIONS" -> "read";
            case "POST", "PUT", "PATCH", "DELETE" -> "write";
            default -> "unknown";
        };
    }

    private String extractResource(String uri) {
        try {
            // Handle document access pattern: /api/users/{userId}/documents/{docId}
            if (uri.matches("/api/users/.+/documents/.+")) {
                String[] parts = uri.split("/");
                if (parts.length >= 6) {
                    return "document:" + parts[5];
                }
            }
            
            // Handle other patterns
            if (uri.startsWith("/api/users/")) {
                return "user-api";
            }
            
            return uri;
        } catch (Exception e) {
            logger.warn("Failed to extract resource from URI: {}", uri, e);
            return uri;
        }
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\",\"status\":401}", message));
    }
    
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\",\"status\":403}", message));
    }
    
    private void sendInternalErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\",\"status\":500}", message));
    }
}
