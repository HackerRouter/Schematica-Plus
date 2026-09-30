package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RenderUpdateSchedulerTest {
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
