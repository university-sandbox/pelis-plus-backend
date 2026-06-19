package com.example.template.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedService {

    private final JdbcTemplate jdbc;

    public SeedService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void seed(String period) {
        String sql = loadScript("db/seed/seed-" + period + ".sql");
        jdbc.execute(sql);
    }

    private String loadScript(String classpath) {
        try {
            return new ClassPathResource(classpath)
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Seed script not found: " + classpath, e);
        }
    }
}
