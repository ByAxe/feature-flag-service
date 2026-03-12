package com.demo.featureflagservice.controller;

import com.demo.featureflagservice.dto.FlagStatsResponse;
import com.demo.featureflagservice.dto.GlobalStatsResponse;
import com.demo.featureflagservice.service.stats.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public GlobalStatsResponse globalStats() {
        return statsService.getGlobalStats();
    }

    @GetMapping("/{flagKey}")
    public FlagStatsResponse flagStats(@PathVariable String flagKey) {
        return statsService.getFlagStats(flagKey);
    }
}
