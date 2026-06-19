package com.example.template.analytics;

import com.example.template.analytics.dto.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/kpis")
    public ResponseEntity<KpiOverviewDto> getKpis(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getKpis(period));
    }

    @GetMapping("/revenue/series")
    public ResponseEntity<List<RevenueSeriesDto>> getRevenueSeries(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getRevenueSeries(period));
    }

    @GetMapping("/revenue/breakdown")
    public ResponseEntity<RevenueBreakdownDto> getRevenueBreakdown(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getRevenueBreakdown(period));
    }

    @GetMapping("/movies/top")
    public ResponseEntity<List<TopMovieDto>> getTopMovies(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(service.getTopMovies(period, Math.min(limit, 20)));
    }

    @GetMapping("/movies/formats")
    public ResponseEntity<List<FormatDistributionDto>> getFormatDistribution(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getFormatDistribution(period));
    }

    @GetMapping("/movies/peak-days")
    public ResponseEntity<List<PeakDayDto>> getPeakDays(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getPeakDays(period));
    }

    @GetMapping("/movies/occupancy")
    public ResponseEntity<List<OccupancyDto>> getOccupancy(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getOccupancyByVenue(period));
    }

    @GetMapping("/snacks/top")
    public ResponseEntity<List<TopSnackDto>> getTopSnacks(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(service.getTopSnacks(period, Math.min(limit, 20)));
    }

    @GetMapping("/snacks/by-category")
    public ResponseEntity<List<SnackCategoryRevenueDto>> getSnackCategoryRevenue(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getSnackCategoryRevenue(period));
    }

    @GetMapping("/snacks/avg-per-order")
    public ResponseEntity<BigDecimal> getAvgSnackRevenuePerOrder(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getAvgSnackRevenuePerOrder(period));
    }

    @GetMapping("/memberships/distribution")
    public ResponseEntity<List<MembershipPlanDistributionDto>> getMembershipDistribution() {
        return ResponseEntity.ok(service.getMembershipDistribution());
    }

    @GetMapping("/users/registrations")
    public ResponseEntity<List<TimeSeriesDto>> getUserRegistrations(
        @RequestParam(defaultValue = "MONTH") AnalyticsPeriod period
    ) {
        return ResponseEntity.ok(service.getUserRegistrations(period));
    }

    @GetMapping("/users/purchase-rate")
    public ResponseEntity<Double> getUserPurchaseRate() {
        return ResponseEntity.ok(service.getUserPurchaseRate());
    }
}
