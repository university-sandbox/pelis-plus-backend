package com.example.template.movie;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findByActiveTrue(Pageable pageable);

    Page<Movie> findByActiveTrueAndStatus(String status, Pageable pageable);

    Page<Movie> findByActiveTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Movie m JOIN m.genres g WHERE m.active = true AND g.id = :genreId")
    Page<Movie> findByActiveTrueAndGenreId(@Param("genreId") Long genreId, Pageable pageable);

    @Query("SELECT m FROM Movie m")
    Page<Movie> findAllMovies(Pageable pageable);

    @Modifying
    @Query("UPDATE Movie m SET m.active = false WHERE m.active = true AND m.id NOT IN :activeIds")
    int deactivateMoviesNotIn(@Param("activeIds") List<Long> activeIds);

    @Modifying
    @Query("UPDATE Movie m SET m.active = true, m.status = :status WHERE m.id IN :activeIds")
    int activateMoviesIn(@Param("activeIds") List<Long> activeIds, @Param("status") String status);
}
