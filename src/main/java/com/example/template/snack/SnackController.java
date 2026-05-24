package com.example.template.snack;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/snacks")
public class SnackController {

    private final SnackService snackService;

    public SnackController(SnackService snackService) {
        this.snackService = snackService;
    }

    @GetMapping
    public ResponseEntity<Page<SnackDto>> getSnacks(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(snackService.getSnacks(category, page));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(snackService.getCategories());
    }
}
