package com.example.template.snack;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnackChoiceRepository extends JpaRepository<SnackChoice, UUID> {

    List<SnackChoice> findByOptionIdOrderByDisplayOrder(UUID optionId);
}
