package com.example.template.movie.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class TmdbMovieSyncCommand implements ApplicationRunner {

    public static final String ARGUMENT_NAME = "sync-tmdb-movies";

    private static final Logger logger = LoggerFactory.getLogger(TmdbMovieSyncCommand.class);

    private final ApplicationContext applicationContext;
    private final TmdbMovieSyncService syncService;

    public TmdbMovieSyncCommand(ApplicationContext applicationContext, TmdbMovieSyncService syncService) {
        this.applicationContext = applicationContext;
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(ARGUMENT_NAME)) {
            return;
        }

        MovieSyncStats stats = syncService.syncMovies();
        logger.info(
            "Manual TMDB movie sync finished: fetched={}, upserted={}, deactivated={}, screeningsCreated={}, screeningsCancelled={}",
            stats.fetched(),
            stats.upserted(),
            stats.deactivated(),
            stats.screeningsCreated(),
            stats.screeningsCancelled()
        );

        SpringApplication.exit(applicationContext, () -> 0);
    }
}
