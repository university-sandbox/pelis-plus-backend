package com.example.template.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.template.order.Order;

@Repository
public interface AnalyticsRepository extends JpaRepository<Order, java.util.UUID> {

    // --- Revenue KPIs ---

    @Query(value = """
        SELECT COALESCE(SUM(o.total), 0)
        FROM orders o
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal sumRevenue(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COUNT(ot.id)
        FROM order_tickets ot
        JOIN orders o ON ot.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    long countTicketsSold(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COALESCE(AVG(o.total), 0)
        FROM orders o
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal avgOrderValue(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COUNT(u.id)
        FROM app_users u
        WHERE u.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    long countNewUsers(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COUNT(am.id)
        FROM active_memberships am
        WHERE am.expires_at >= CURRENT_DATE
        """, nativeQuery = true)
    long countActiveMemberships();

    @Query(value = """
        SELECT COALESCE(AVG(CAST(tc.sold AS FLOAT) / NULLIF(r.capacity, 0)), 0)
        FROM screenings s
        JOIN rooms r ON s.room_id = r.id
        JOIN (
            SELECT ot.screening_id, COUNT(ot.id) AS sold
            FROM order_tickets ot
            JOIN orders o ON ot.order_id = o.id
            WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
            GROUP BY ot.screening_id
        ) tc ON s.id = tc.screening_id
        WHERE s.date BETWEEN :from AND :to
        """, nativeQuery = true)
    double avgOccupancyRate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // --- Revenue Series ---

    @Query(value = """
        SELECT TO_CHAR(DATE_TRUNC(:bucket, o.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS label,
               COALESCE(SUM(o.total), 0) AS revenue
        FROM orders o
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        GROUP BY DATE_TRUNC(:bucket, o.created_at AT TIME ZONE 'UTC')
        ORDER BY DATE_TRUNC(:bucket, o.created_at AT TIME ZONE 'UTC')
        """, nativeQuery = true)
    List<Object[]> revenueSeriesRaw(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("bucket") String bucket
    );

    // --- Revenue Breakdown ---

    @Query(value = """
        SELECT COALESCE(SUM(ot.price), 0)
        FROM order_tickets ot
        JOIN orders o ON ot.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal sumTicketRevenue(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COALESCE(SUM(os.unit_price * os.quantity), 0)
        FROM order_snacks os
        JOIN orders o ON os.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal sumSnackRevenue(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COALESCE(SUM(o.discount), 0)
        FROM orders o
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal sumDiscount(@Param("from") Instant from, @Param("to") Instant to);

    // --- Movies ---

    @Query(value = """
        SELECT ot.movie_title, COUNT(ot.id) AS tickets_sold, SUM(ot.price) AS revenue
        FROM order_tickets ot
        JOIN orders o ON ot.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        GROUP BY ot.movie_title
        ORDER BY tickets_sold DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<Object[]> topMoviesRaw(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("lim") int limit
    );

    @Query(value = """
        SELECT ot.format, COUNT(ot.id) AS cnt
        FROM order_tickets ot
        JOIN orders o ON ot.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        GROUP BY ot.format
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> formatDistributionRaw(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT TO_CHAR(ot.screening_date, 'Dy') AS day_name,
               EXTRACT(DOW FROM ot.screening_date) AS day_num,
               COUNT(ot.id) AS tickets_sold
        FROM order_tickets ot
        JOIN orders o ON ot.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        GROUP BY day_name, day_num
        ORDER BY day_num
        """, nativeQuery = true)
    List<Object[]> peakDaysRaw(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT v.name AS venue_name,
               COALESCE(AVG(CAST(tc.sold AS FLOAT) / NULLIF(r.capacity, 0)), 0) AS occupancy_rate
        FROM venues v
        JOIN rooms r ON r.venue_id = v.id
        JOIN screenings s ON s.room_id = r.id
        JOIN (
            SELECT ot.screening_id, COUNT(ot.id) AS sold
            FROM order_tickets ot
            JOIN orders o ON ot.order_id = o.id
            WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
            GROUP BY ot.screening_id
        ) tc ON s.id = tc.screening_id
        WHERE s.date BETWEEN :from AND :to
        GROUP BY v.name
        ORDER BY occupancy_rate DESC
        """, nativeQuery = true)
    List<Object[]> occupancyByVenueRaw(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // --- Snacks ---

    @Query(value = """
        SELECT os.snack_name, SUM(os.quantity) AS total_qty, SUM(os.unit_price * os.quantity) AS revenue
        FROM order_snacks os
        JOIN orders o ON os.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        GROUP BY os.snack_name
        ORDER BY total_qty DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<Object[]> topSnacksRaw(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("lim") int limit
    );

    @Query(value = """
        SELECT s.category, SUM(os.unit_price * os.quantity) AS revenue
        FROM order_snacks os
        JOIN snacks s ON os.snack_id = s.id
        JOIN orders o ON os.order_id = o.id
        WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
          AND o.created_at BETWEEN :from AND :to
        GROUP BY s.category
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> snackCategoryRevenueRaw(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT COALESCE(AVG(snack_totals.snack_total), 0)
        FROM (
            SELECT o.id, SUM(os.unit_price * os.quantity) AS snack_total
            FROM orders o
            JOIN order_snacks os ON os.order_id = o.id
            WHERE o.status = 'confirmed' AND o.payment_status = 'approved'
              AND o.created_at BETWEEN :from AND :to
            GROUP BY o.id
        ) snack_totals
        """, nativeQuery = true)
    BigDecimal avgSnackRevenuePerOrder(@Param("from") Instant from, @Param("to") Instant to);

    // --- Memberships ---

    @Query(value = """
        SELECT mp.name, COUNT(am.id) AS user_count
        FROM active_memberships am
        JOIN membership_plans mp ON am.plan_id = mp.id
        WHERE am.expires_at >= CURRENT_DATE
        GROUP BY mp.name
        ORDER BY user_count DESC
        """, nativeQuery = true)
    List<Object[]> membershipDistributionRaw();

    // --- Users ---

    @Query(value = """
        SELECT TO_CHAR(DATE_TRUNC(:bucket, u.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS label,
               COUNT(u.id) AS cnt
        FROM app_users u
        WHERE u.created_at BETWEEN :from AND :to
        GROUP BY DATE_TRUNC(:bucket, u.created_at AT TIME ZONE 'UTC')
        ORDER BY DATE_TRUNC(:bucket, u.created_at AT TIME ZONE 'UTC')
        """, nativeQuery = true)
    List<Object[]> userRegistrationsRaw(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("bucket") String bucket
    );

    @Query(value = """
        SELECT CAST(COUNT(DISTINCT o.user_id) AS FLOAT) / NULLIF(CAST(COUNT(DISTINCT u.id) AS FLOAT), 0)
        FROM app_users u
        LEFT JOIN orders o ON o.user_id = u.id
            AND o.status = 'confirmed' AND o.payment_status = 'approved'
        """, nativeQuery = true)
    Double userPurchaseRate();
}
