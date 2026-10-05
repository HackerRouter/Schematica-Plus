package com.github.lunatrius.schematica.client.renderer;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class RenderBudgetTest {
    @After public void reset() {
        RenderBudget.configuredDistance = 0;
        RenderBudget.minFps = 30;
    }

    @Test public void shrinksWhileSlowAndGrowsBackWhenFast() {
        RenderBudget.configuredDistance = 0;
        RenderBudget.minFps = 30;
        RenderBudget budget = new RenderBudget();
        assertEquals(RenderBudget.UNLIMITED, budget.limit(), 0);
        assertTrue(budget.update(20));
        assertEquals(384, budget.limit(), 0.001);
        assertFalse(budget.update(20));
        assertEquals(288, budget.limit(), 0.001);
        for (int i = 0; i < 20; i++) budget.update(5);
        assertEquals(RenderBudget.MIN_DISTANCE, budget.limit(), 0);
        assertTrue(budget.reduced());
        budget.update(40);
        budget.update(40);
        assertEquals(RenderBudget.MIN_DISTANCE, budget.limit(), 0);
        budget.update(40);
        assertEquals(40, budget.limit(), 0.001);
        budget.update(35);
        for (int i = 0; i < 60; i++) budget.update(60);
        assertEquals(RenderBudget.UNLIMITED, budget.limit(), 0);
        assertFalse(budget.reduced());
        assertTrue(budget.update(10));
    }

    @Test public void configuredDistanceCapsAndZeroFpsTurnsItOff() {
        RenderBudget.configuredDistance = 100;
        RenderBudget.minFps = 0;
        RenderBudget budget = new RenderBudget();
        assertFalse(budget.update(1));
        assertEquals(100, budget.limit(), 0);
        RenderBudget.minFps = 30;
        budget.update(10);
        assertEquals(75, budget.limit(), 0.001);
        assertEquals(0, RenderBudget.distance(5, 5, 5, 0, 0, 0, 10, 10, 10), 0);
        assertEquals(5, RenderBudget.distance(-3, 5, -4, 0, 0, 0, 10, 10, 10), 1e-9);
    }
}
