package com.project.validation.controller;

import com.project.validation.entity.ValidationEntity;
import com.project.validation.repository.ValidationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/validations")
@CrossOrigin(origins = "*")
public class ValidationController {

    @Autowired
    private ValidationRepository validationRepository;

    @GetMapping
    public ResponseEntity<List<ValidationEntity>> getAllValidations() {
        List<ValidationEntity> validations = validationRepository.findAll();
        return ResponseEntity.ok(validations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValidationEntity> getValidationById(@PathVariable Long id) {
        return validationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
