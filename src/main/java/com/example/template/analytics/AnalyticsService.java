package com.example.template.analytics;

import com.example.template.analytics.dto.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository repo;

    public AnalyticsService(AnalyticsRepository repo) {
        this.repo = repo;
    }

    // --- Period helpers ---

    private Instant periodStart(AnalyticsPeriod period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (period) {
            case TODAY -> today.atStartOfDay(ZoneOffset.UTC).toInstant();
            case WEEK  -> today.minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant();
            case MONTH -> today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            case YEAR  -> today.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }

    private Instant periodEnd() {
        return Instant.now();
    }

    private LocalDate periodStartDate(AnalyticsPeriod period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (period) {
            case TODAY -> today;
            case WEEK  -> today.minusDays(6);
            case MONTH -> today.withDayOfMonth(1);
            case YEAR  -> today.withDayOfYear(1);
        };
    }


    // --- KPIs ---

    public KpiOverviewDto getKpis(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();
        LocalDate fromDate = periodStartDate(period);
        LocalDate toDate   = LocalDate.now(ZoneOffset.UTC);

        BigDecimal revenue   = repo.sumRevenue(from, to);
        long tickets         = repo.countTicketsSold(from, to);
        BigDecimal aov       = repo.avgOrderValue(from, to);
        long newUsers        = repo.countNewUsers(from, to);
        long activeMemberships = repo.countActiveMemberships();
        double occupancy     = repo.avgOccupancyRate(fromDate, toDate);

        return new KpiOverviewDto(revenue, tickets, aov, newUsers, activeMemberships, occupancy);
    }

    // --- Revenue Series ---

    public List<RevenueSeriesDto> getRevenueSeries(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        List<Object[]> rows = switch (period) {
            case TODAY        -> repo.revenueSeriesByHour(from, to);
            case WEEK, MONTH  -> repo.revenueSeriesByDay(from, to);
            case YEAR         -> repo.revenueSeriesByWeek(from, to);
        };

        return rows.stream()
            .map(r -> new RevenueSeriesDto((String) r[0], toBigDecimal(r[1])))
            .toList();
    }

    // --- Revenue Breakdown ---

    public RevenueBreakdownDto getRevenueBreakdown(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        return new RevenueBreakdownDto(
            repo.sumTicketRevenue(from, to),
            repo.sumSnackRevenue(from, to),
            repo.sumDiscount(from, to)
        );
    }

    // --- Movies ---

    public List<TopMovieDto> getTopMovies(AnalyticsPeriod period, int limit) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        return repo.topMoviesRaw(from, to, limit).stream()
            .map(r -> new TopMovieDto(
                (String) r[0],
                toLong(r[1]),
                toBigDecimal(r[2])
            ))
            .toList();
    }

    public List<FormatDistributionDto> getFormatDistribution(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        return repo.formatDistributionRaw(from, to).stream()
            .map(r -> new FormatDistributionDto((String) r[0], toLong(r[1])))
            .toList();
    }

    public List<PeakDayDto> getPeakDays(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        return repo.peakDaysRaw(from, to).stream()
            .map(r -> new PeakDayDto((String) r[0], toLong(r[2])))
            .toList();
    }

    public List<OccupancyDto> getOccupancyByVenue(AnalyticsPeriod period) {
        LocalDate from = periodStartDate(period);
        LocalDate to   = LocalDate.now(ZoneOffset.UTC);

        return repo.occupancyByVenueRaw(from, to).stream()
            .map(r -> new OccupancyDto((String) r[0], toDouble(r[1])))
            .toList();
    }

    // --- Snacks ---

    public List<TopSnackDto> getTopSnacks(AnalyticsPeriod period, int limit) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        return repo.topSnacksRaw(from, to, limit).stream()
            .map(r -> new TopSnackDto(
                (String) r[0],
                toLong(r[1]),
                toBigDecimal(r[2])
            ))
            .toList();
    }

    public List<SnackCategoryRevenueDto> getSnackCategoryRevenue(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        return repo.snackCategoryRevenueRaw(from, to).stream()
            .map(r -> new SnackCategoryRevenueDto((String) r[0], toBigDecimal(r[1])))
            .toList();
    }

    public BigDecimal getAvgSnackRevenuePerOrder(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();
        return repo.avgSnackRevenuePerOrder(from, to);
    }

    // --- Memberships ---

    public List<MembershipPlanDistributionDto> getMembershipDistribution() {
        return repo.membershipDistributionRaw().stream()
            .map(r -> new MembershipPlanDistributionDto((String) r[0], toLong(r[1])))
            .toList();
    }

    // --- Users ---

    public List<TimeSeriesDto> getUserRegistrations(AnalyticsPeriod period) {
        Instant from = periodStart(period);
        Instant to   = periodEnd();

        List<Object[]> rows = switch (period) {
            case TODAY        -> repo.userRegistrationsByHour(from, to);
            case WEEK, MONTH  -> repo.userRegistrationsByDay(from, to);
            case YEAR         -> repo.userRegistrationsByWeek(from, to);
        };

        return rows.stream()
            .map(r -> new TimeSeriesDto((String) r[0], toLong(r[1])))
            .toList();
    }

    public double getUserPurchaseRate() {
        Double rate = repo.userPurchaseRate();
        return rate != null ? rate : 0.0;
    }

    // --- Type helpers ---

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return new BigDecimal(v.toString());
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }
}
