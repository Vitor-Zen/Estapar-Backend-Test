package com.zendev.Estapar.controller;

import com.zendev.Estapar.dto.RevenueRequest;
import com.zendev.Estapar.dto.RevenueResponse;
import com.zendev.Estapar.service.RevenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/revenue")
public class RevenueController {

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping
    public ResponseEntity<RevenueResponse> getRevenue(@RequestBody RevenueRequest request){
        return ResponseEntity.ok(revenueService.getRevenue(request));
    }
}
