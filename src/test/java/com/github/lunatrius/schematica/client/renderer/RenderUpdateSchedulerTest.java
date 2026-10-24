package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RenderUpdateSchedulerTest {
    @Test public void resumesCleanScansAcrossFramesWithoutStarvingLaterDirtyEntries() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        List<List<Integer>> groups = Arrays.asList(Arrays.asList(0, 1, 2, 3));
        List<Integer> rebuilt = new ArrayList<>();
        for (int frame = 0; frame < 4; frame++) {
            scheduler.update(groups, 3, () -> false, n -> n == 3, rebuilt::add);
        }
        assertEquals(Collections.singletonList(3), rebuilt);
    }

    @Test public void cacheReuseDoesNotConsumeRebuildSlots() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        List<Integer> visited = new ArrayList<>();
        assertEquals(3, scheduler.update(Arrays.asList(Arrays.asList(0, 1, 2, 3, 4)), 3,
            () -> true, n -> true, n -> { visited.add(n); return n >= 2; }));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), visited);
    }

    @Test public void stopsAtDeadlineAfterReuseAndKeepsGroupFairness() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        List<List<Integer>> groups = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        List<Integer> visited = new ArrayList<>();
        assertEquals(0, scheduler.update(groups, 3, () -> false, n -> true, n -> { visited.add(n); return false; }));
        assertEquals(Collections.singletonList(1), visited);
        assertEquals(1, scheduler.update(groups, 3, () -> false, n -> true, visited::add));
        assertEquals(Arrays.asList(1, 3), visited);
    }

    @Test public void boundsCleanScansAndNeverRetriesTheSameEntryWithinAFrame() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        AtomicInteger visited = new AtomicInteger();
        assertEquals(0, scheduler.update(Arrays.asList(Arrays.asList(1, 2, 3)), 3, () -> false,
            n -> { visited.incrementAndGet(); return false; }, n -> true));
        assertEquals(1, visited.get());
        visited.set(0);
        assertEquals(0, scheduler.update(Arrays.asList(Arrays.asList(1, 2, 3)), 3, () -> true,
            n -> true, n -> { visited.incrementAndGet(); return false; }));
        assertEquals(3, visited.get());
    }

    @Test public void sharesBudgetAcrossInstancesWithLargeBacklogs() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        List<List<Integer>> groups = Arrays.asList(Arrays.asList(1, 2, 3, 4), Arrays.asList(5, 6, 7, 8));
        List<Integer> rebuilt = new ArrayList<>();
        assertEquals(3, scheduler.update(groups, 3, () -> true, n -> !rebuilt.contains(n), rebuilt::add));
        assertEquals(Arrays.asList(1, 5, 2), rebuilt);
        assertEquals(3, scheduler.update(groups, 3, () -> true, n -> !rebuilt.contains(n), rebuilt::add));
        assertEquals(Arrays.asList(1, 5, 2, 6, 3, 7), rebuilt);
    }

    @Test public void advancesBetweenFramesEvenWhenTheFirstRebuildExceedsTimeBudget() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        List<List<Integer>> groups = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3));
        List<Integer> rebuilt = new ArrayList<>();
        for (int frame = 0; frame < 6; frame++) {
            assertEquals(1, scheduler.update(groups, 3, () -> false, n -> true, rebuilt::add));
        }
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), rebuilt);
    }

    @Test public void skipsCleanInstancesAndHandlesRemoval() {
        RenderUpdateScheduler<Integer> scheduler = new RenderUpdateScheduler<>();
        List<List<Integer>> groups = Arrays.asList(Collections.emptyList(), Arrays.asList(1), Arrays.asList(2));
        List<Integer> rebuilt = new ArrayList<>();
        assertEquals(1, scheduler.update(groups, 3, () -> true, n -> n == 2 && !rebuilt.contains(n), rebuilt::add));
        assertEquals(0, scheduler.update(groups, 3, () -> true, n -> !rebuilt.contains(n) && n == 2, rebuilt::add));
        assertEquals(0, scheduler.update(Collections.emptyList(), 3, () -> true, n -> true, rebuilt::add));
        assertEquals(1, scheduler.update(Arrays.asList(Arrays.asList(3)), 3, () -> true,
            n -> !rebuilt.contains(n), rebuilt::add));
        assertEquals(Arrays.asList(2, 3), rebuilt);
    }
}
