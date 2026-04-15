package com.example.template.admin;

import com.example.template.screening.CreateScreeningRequest;
import com.example.template.screening.ScreeningDto;
import com.example.template.screening.ScreeningService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
@RequestMapping("/admin/screenings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScreeningController {

    private final ScreeningService screeningService;

    public AdminScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @GetMapping
    public ResponseEntity<Page<ScreeningDto>> listScreenings(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long movieId,
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(screeningService.getAllScreenings(status, movieId, page));
    }

    @PostMapping
    public ResponseEntity<ScreeningDto> createScreening(@RequestBody CreateScreeningRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(screeningService.createScreening(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScreeningDto> updateScreening(
        @PathVariable UUID id,
        @RequestBody CreateScreeningRequest request
    ) {
        return ResponseEntity.ok(screeningService.updateScreening(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ScreeningDto> cancelScreening(@PathVariable UUID id) {
        return ResponseEntity.ok(screeningService.cancelScreening(id));
    }
}
