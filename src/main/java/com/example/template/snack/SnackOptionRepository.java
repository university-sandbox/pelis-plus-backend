package com.example.template.snack;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnackOptionRepository extends JpaRepository<SnackOption, UUID> {

    List<SnackOption> findBySnackIdOrderByDisplayOrder(UUID snackId);
}
