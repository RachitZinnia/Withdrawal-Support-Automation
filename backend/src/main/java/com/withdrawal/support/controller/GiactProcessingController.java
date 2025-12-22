package com.withdrawal.support.controller;

import com.withdrawal.support.service.MRTProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Slf4j
public class GiactProcessingController {

    @Autowired
    MRTProcessingService mrtProcessingService;

    @PostMapping("process-mrt-case")
    public void processMRTCase() {

        try{
            mrtProcessingService.processMrtWaitingCases();
        } catch (Exception e) {

        }
    }
}

