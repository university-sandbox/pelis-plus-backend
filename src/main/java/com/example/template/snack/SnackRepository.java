package com.example.template.snack;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnackRepository extends JpaRepository<Snack, UUID> {

    List<Snack> findByStatus(String status);

    List<Snack> findByCategoryAndStatus(String category, String status);
}
