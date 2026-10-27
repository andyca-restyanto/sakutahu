// filepath: /backend/src/main/java/com/example/app/modules/subscription/controller/FeatureController.java
package com.example.app.modules.subscription.controller;

import com.example.app.modules.subscription.dto.FeatureAccessResponse;
import com.example.app.modules.subscription.service.FeatureAccessService;
import com.example.app.shared.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class FeatureController {

    private final FeatureAccessService featureAccessService;

    public FeatureController(FeatureAccessService featureAccessService) {
        this.featureAccessService = featureAccessService;
    }

    @GetMapping("/features")
    public FeatureAccessResponse features(@AuthenticationPrincipal UserPrincipal principal) {
        return featureAccessService.features(principal.id());
    }
}
