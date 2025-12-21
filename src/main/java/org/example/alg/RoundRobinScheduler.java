package org.example.alg;

import org.example.core.*;

public class RoundRobinScheduler extends MetaheuristicScheduler {

    // ✅ Updated constructor to accept a seed
    public RoundRobinScheduler(Fitness fit, long seed) {
        super(fit, seed);
    }

    @Override
    public Result solve(int iterationsIgnored, int populationIgnored, int groupsIgnored) {
        int n = fit.taskCount(), k = fit.vmCount();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i % k;
        double f = fit.evaluate(a);
        return new Result(a, f, 1); // deterministic, no randomness involved
    }
}