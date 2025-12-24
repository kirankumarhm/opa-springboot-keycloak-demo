package com.opa.demo.controller;

import com.opa.demo.service.OpaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    private final OpaService opaService;
    
    public PublicController(OpaService opaService) {
        this.opaService = opaService;
    }
    
    @PostMapping("/check-access")
    public ResponseEntity<AccessResponse> checkAccess(@RequestBody AccessRequest request) {
        boolean allowed = opaService.isAllowed(request.user(), request.action(), request.resource());
        return ResponseEntity.ok(new AccessResponse(allowed));
    }
    
    public record AccessRequest(String user, String action, String resource) {}
    public record AccessResponse(boolean allowed) {}
}
