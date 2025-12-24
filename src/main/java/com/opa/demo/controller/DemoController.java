package com.opa.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Document API", description = "Document access and authorization endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DemoController {
    
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    
    @Operation(summary = "Get document by ID", description = "Retrieve a document by user and document ID")
    @ApiResponse(responseCode = "200", description = "Document retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @GetMapping("/users/{userId}/documents/{docId}")
    @PreAuthorize("#userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "User ID", required = true)
            @PathVariable @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String userId,
            @Parameter(description = "Document ID", required = true)
            @PathVariable @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String docId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = extractUsername(jwt);
        logger.info("User {} accessing document {} for user {}", username, docId, userId);
        
        // Simulate document retrieval
        DocumentResponse response = new DocumentResponse(
            docId,
            "Document " + docId + " content",
            username,
            "application/pdf",
            Instant.now().toEpochMilli(),
            "v1.0",
            calculateDocumentSize(docId)
        );
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Check access permissions", description = "Check if user has access to perform action on resource")
    @ApiResponse(responseCode = "200", description = "Access check completed")
    @PostMapping("/check-access")
    public ResponseEntity<AccessCheckResponse> checkAccess(
            @Valid @RequestBody AccessCheckRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String username = extractUsername(jwt);
        logger.info("Access check for user: {} - action: {}, resource: {}", 
                   username, request.action(), request.resource());
        
        return ResponseEntity.ok(new AccessCheckResponse(
            true,
            "Access granted for user: " + username,
            Instant.now().toEpochMilli(),
            username,
            request.action(),
            request.resource()
        ));
    }
    
    private String extractUsername(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isEmpty()) {
            username = jwt.getClaimAsString("sub");
        }
        if (username == null || username.isEmpty()) {
            username = jwt.getClaimAsString("name");
        }
        logger.debug("Extracted username: {} from JWT claims: {}", username, jwt.getClaims().keySet());
        return username;
    }
    
    private long calculateDocumentSize(String docId) {
        // Simulate document size calculation
        return docId.hashCode() % 10000 + 1000;
    }
    
    public record AccessCheckRequest(
        @NotBlank(message = "Action is required")
        @Pattern(regexp = "^(read|write|delete|admin)$", message = "Action must be one of: read, write, delete, admin")
        String action,
        
        @NotBlank(message = "Resource is required")
        @Pattern(regexp = "^[a-zA-Z0-9:/_-]+$", message = "Resource format is invalid")
        String resource
    ) {}
    
    public record AccessCheckResponse(
        boolean allowed,
        String message,
        long timestamp,
        String user,
        String action,
        String resource
    ) {}
    
    public record DocumentResponse(
        String documentId,
        String content,
        String accessedBy,
        String contentType,
        long timestamp,
        String version,
        long sizeBytes
    ) {}
}
