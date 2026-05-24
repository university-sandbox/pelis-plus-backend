package com.example.template.movie.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TmdbMovieSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TmdbMovieSyncScheduler.class);

    private final TmdbMovieSyncService syncService;

    public TmdbMovieSyncScheduler(TmdbMovieSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${app.tmdb.sync-cron:0 0 23 * * SUN}", zone = "${app.tmdb.time-zone:America/Lima}")
    public void syncMoviesWeekly() {
        try {
            MovieSyncStats stats = syncService.syncMovies();
            logger.info(
                "TMDB movie sync finished: fetched={}, upserted={}, deactivated={}",
                stats.fetched(),
                stats.upserted(),
                stats.deactivated()
            );
        } catch (RuntimeException ex) {
            logger.error("TMDB movie sync failed", ex);
        }
    }
}
