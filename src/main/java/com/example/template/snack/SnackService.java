package com.example.template.snack;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SnackService {

    private static final int PAGE_SIZE = 20;

    private final SnackRepository snackRepository;
    private final SnackOptionRepository snackOptionRepository;
    private final SnackChoiceRepository snackChoiceRepository;

    public SnackService(
        SnackRepository snackRepository,
        SnackOptionRepository snackOptionRepository,
        SnackChoiceRepository snackChoiceRepository
    ) {
        this.snackRepository = snackRepository;
        this.snackOptionRepository = snackOptionRepository;
        this.snackChoiceRepository = snackChoiceRepository;
    }

    public Page<SnackDto> getSnacks(String category, int page) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), PAGE_SIZE, Sort.by("category").ascending().and(Sort.by("name").ascending()));
        if (category != null && !category.isBlank()) {
            return snackRepository.findByCategoryAndStatus(category, "active", pageRequest).map(this::toDto);
        }
        return snackRepository.findByStatus("active", pageRequest).map(this::toDto);
    }

    public List<String> getCategories() {
        return snackRepository.findByStatus("active").stream()
            .map(Snack::getCategory)
            .distinct()
            .sorted()
            .toList();
    }

    public SnackDto toDto(Snack snack) {
        List<SnackOption> options = snackOptionRepository.findBySnackIdOrderByDisplayOrder(snack.getId());
        List<SnackOptionDto> optionDtos = options.stream().map(opt -> {
            List<String> choices = snackChoiceRepository.findByOptionIdOrderByDisplayOrder(opt.getId())
                .stream().map(SnackChoice::getChoice).toList();
            return new SnackOptionDto(opt.getLabel(), choices);
        }).toList();

        return new SnackDto(
            snack.getId().toString(),
            snack.getName(),
            snack.getDescription(),
            snack.getCategory(),
            snack.getPrice() != null ? snack.getPrice().doubleValue() : 0.0,
            snack.getImage(),
            snack.getStatus(),
            optionDtos
        );
    }
}
