package com.devexperts.dxlab.lincheck.strategy.stress;

/*
 * #%L
 * Lincheck
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.devexperts.dxlab.lincheck.Actor;
import com.devexperts.dxlab.lincheck.Reporter;
import com.devexperts.dxlab.lincheck.execution.ExecutionScenario;
import com.devexperts.dxlab.lincheck.runner.ParallelThreadsRunner;
import com.devexperts.dxlab.lincheck.runner.Runner;
import com.devexperts.dxlab.lincheck.strategy.Strategy;
import com.devexperts.dxlab.lincheck.verifier.Verifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Phaser;

/**
 * This strategy
 */
public class StressStrategy extends Strategy {
    private static final int MAX_WAIT = 1000;
    private final Random random = new Random(0);

    private final int invocations;
    private final Runner runner;

    private final List<int[]> waits;

    public StressStrategy(Class<?> testClass, ExecutionScenario scenario,
        Verifier verifier, StressCTestConfiguration testCfg, Reporter reporter)
    {
        super(scenario, verifier, reporter);
        this.invocations = testCfg.invocationsPerIteration;
        // Create waits if needed
        waits = testCfg.addWaits ? new ArrayList<>() : null;
        if (testCfg.addWaits) {
            for (List<Actor> actorsForThread : scenario.parallelExecution) {
                waits.add(new int[actorsForThread.size()]);
            }
        }
        // Create runner
        Phaser phaser = new Phaser(testCfg.threads);
        runner = new ParallelThreadsRunner(scenario, this, testClass, null) {
            @Override
            public void onStart(int iThread) {
                super.onStart(iThread);
                phaser.arriveAndAwaitAdvance();
            }
        };
    }

    @Override
    public void run() throws InterruptedException {
        try {
            // Run invocations
            for (int invocation = 0; invocation < invocations; invocation++) {
                // Specify waits if needed
                if (waits != null) {
                    int maxWait = (int) ((float) invocation * MAX_WAIT / invocations) + 1;
                    for (int[] waitsForThread : waits) {
                        for (int i = 0; i < waitsForThread.length; i++) {
                            waitsForThread[i] = random.nextInt(maxWait);
                        }
                    }
                }
                verifyResults(runner.run());
            }
        } finally {
            runner.close();
        }
    }
}
