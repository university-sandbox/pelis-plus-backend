package com.example.template.screening;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScreeningRepository extends JpaRepository<Screening, UUID> {

    List<Screening> findByMovieIdAndStatus(Long movieId, String status);

    List<Screening> findByMovieId(Long movieId);

    @Query("SELECT s FROM Screening s WHERE s.movie.id = :movieId AND (:venueId IS NULL OR s.room.venue.id = :venueId) AND (:date IS NULL OR s.date = :date) AND (:format IS NULL OR s.format = :format) AND s.status = 'active'")
    Page<Screening> findFiltered(
        @Param("movieId") Long movieId,
        @Param("venueId") UUID venueId,
        @Param("date") java.time.LocalDate date,
        @Param("format") String format,
        Pageable pageable
    );

    @Query("SELECT s FROM Screening s WHERE (:status IS NULL OR s.status = :status) AND (:movieId IS NULL OR s.movie.id = :movieId)")
    Page<Screening> findByFilters(
        @Param("status") String status,
        @Param("movieId") Long movieId,
        Pageable pageable
    );
}
