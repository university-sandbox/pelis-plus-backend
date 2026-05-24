package com.example.template.snack;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnackRepository extends JpaRepository<Snack, UUID> {

    List<Snack> findByStatus(String status);

    List<Snack> findByCategoryAndStatus(String category, String status);

    Page<Snack> findByStatus(String status, Pageable pageable);

    Page<Snack> findByCategoryAndStatus(String category, String status, Pageable pageable);
}
