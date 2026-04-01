package com.zendev.Estapar.controller;

import com.zendev.Estapar.dto.WebhookRequest;
import com.zendev.Estapar.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody WebhookRequest request){
        webhookService.processEvent(request);
        return ResponseEntity.ok().build();
    }
}
