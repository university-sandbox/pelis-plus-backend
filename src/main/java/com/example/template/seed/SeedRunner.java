package com.example.template.seed;

import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * CLI seed command. Run without starting the HTTP server:
 *
 *   java -jar /app/app.jar --spring.main.web-application-type=NONE --seed=month
 *   java -jar /app/app.jar --spring.main.web-application-type=NONE --seed=week
 *   java -jar /app/app.jar --spring.main.web-application-type=NONE --seed=today
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Set<String> VALID_PERIODS = Set.of("today", "week", "month");

    private final SeedService seedService;

    public SeedRunner(SeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("seed")) return;

        List<String> values = args.getOptionValues("seed");
        if (values == null || values.isEmpty()) {
            System.err.println("[seed] Usage: --seed=today|week|month");
            System.exit(1);
        }

        String period = values.get(0).toLowerCase();
        if (!VALID_PERIODS.contains(period)) {
            System.err.println("[seed] Invalid period '" + period + "'. Use: today, week, month");
            System.exit(1);
        }

        System.out.println("[seed] Running seed-" + period + "...");
        seedService.seed(period);
        System.out.println("[seed] Done.");
        System.exit(0);
    }
}
