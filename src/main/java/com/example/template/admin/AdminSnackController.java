package com.example.template.admin;

import com.example.template.snack.Snack;
import com.example.template.snack.SnackDto;
import com.example.template.snack.SnackRepository;
import com.example.template.snack.SnackService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/snacks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSnackController {

    private final SnackRepository snackRepository;
    private final SnackService snackService;

    public AdminSnackController(SnackRepository snackRepository, SnackService snackService) {
        this.snackRepository = snackRepository;
        this.snackService = snackService;
    }

    @GetMapping
    public ResponseEntity<Page<SnackDto>> listSnacks(
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(snackRepository.findAll(
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("category").ascending().and(Sort.by("name").ascending()))
        ).map(snackService::toDto));
    }

    @PostMapping
    public ResponseEntity<SnackDto> createSnack(@RequestBody SnackRequest request) {
        Snack snack = new Snack();
        applyRequest(snack, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(snackService.toDto(snackRepository.save(snack)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SnackDto> updateSnack(@PathVariable UUID id, @RequestBody SnackRequest request) {
        Snack snack = snackRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Snack not found: " + id));
        applyRequest(snack, request);
        return ResponseEntity.ok(snackService.toDto(snackRepository.save(snack)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SnackDto> toggleStatus(@PathVariable UUID id) {
        Snack snack = snackRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Snack not found: " + id));
        snack.setStatus("active".equals(snack.getStatus()) ? "inactive" : "active");
        return ResponseEntity.ok(snackService.toDto(snackRepository.save(snack)));
    }

    private void applyRequest(Snack snack, SnackRequest request) {
        if (request.name() != null) snack.setName(request.name());
        if (request.description() != null) snack.setDescription(request.description());
        if (request.category() != null) snack.setCategory(request.category());
        if (request.price() != null) snack.setPrice(BigDecimal.valueOf(request.price()));
        if (request.image() != null) snack.setImage(request.image());
        if (request.status() != null) snack.setStatus(request.status());
    }

    public record SnackRequest(String name, String description, String category, Double price, String image, String status) {}
}
