package com.opa.demo.controller;

import com.opa.demo.service.OpaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DemoController {
    
    private final OpaService opaService;
    
    public DemoController(OpaService opaService) {
        this.opaService = opaService;
    }
    
    @GetMapping("/users/{userId}/documents/{docId}")
    public ResponseEntity<String> getDocument(
            @PathVariable String userId,
            @PathVariable String docId,
            @RequestParam(defaultValue = "read") String action) {
        
        boolean allowed = opaService.isAllowed(userId, action, "document:" + docId);
        
        if (allowed) {
            return ResponseEntity.ok("Document " + docId + " content for user " + userId);
        } else {
            return ResponseEntity.status(403).body("Access denied");
        }
    }
    
    @PostMapping("/check-access")
    public ResponseEntity<AccessResponse> checkAccess(@RequestBody AccessRequest request) {
        boolean allowed = opaService.isAllowed(request.user(), request.action(), request.resource());
        return ResponseEntity.ok(new AccessResponse(allowed));
    }
    
    public record AccessRequest(String user, String action, String resource) {}
    public record AccessResponse(boolean allowed) {

    }
    }
