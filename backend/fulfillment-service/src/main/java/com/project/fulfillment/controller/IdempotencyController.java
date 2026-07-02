package com.project.fulfillment.controller;

import com.project.fulfillment.entity.IdempotencyRecordEntity;
import com.project.fulfillment.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/idempotency")
@CrossOrigin(origins = "*")
public class IdempotencyController {

    @Autowired
    private IdempotencyService idempotencyService;

    @GetMapping
    public ResponseEntity<List<IdempotencyRecordEntity>> getAllIdempotencyRecords() {
        List<IdempotencyRecordEntity> records = idempotencyService.getAllRecords();
        return ResponseEntity.ok(records);
    }
}
