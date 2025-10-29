package com.dklamps;

import java.time.Duration;
import java.time.Instant;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DKLampsStatsTracker {

    @Getter
    private int lampsFixed = 0;
    @Getter
    private int lampsPerHr = 0;
    @Getter
    private int totalLampsFixed = 0;
    
    private Instant start;

    public DKLampsStatsTracker() {}

    public void incrementLampsFixed() {
        ++lampsFixed;

        if (start == null) {
            start = Instant.now();
        }

        Duration elapsed = Duration.between(start, Instant.now());
        long elapsedMs = elapsed.toMillis();

        if (lampsFixed >= 3 && elapsedMs > 0) {
            lampsPerHr = (int) ((double) lampsFixed * Duration.ofHours(1).toMillis() / elapsedMs);
        }
    }

    public void updateTotalLampsFixed(int newTotal) {
        if (newTotal > 0 && newTotal > this.totalLampsFixed) {
            this.totalLampsFixed = newTotal;
        }
    }

    public void resetSession() {
        lampsFixed = 0;
        lampsPerHr = 0;
        start = null;
    }
}
