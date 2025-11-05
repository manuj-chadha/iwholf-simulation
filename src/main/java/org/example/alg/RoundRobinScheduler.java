package org.example.alg;

import org.example.core.*;

public class RoundRobinScheduler extends MetaheuristicScheduler {
    public RoundRobinScheduler(Fitness fit){ super(fit); }

    @Override
    public Result solve(int iterationsIgnored, int populationIgnored, int groupsIgnored){
        int n = fit.taskCount(), k = fit.vmCount();
        int[] a = new int[n];
        for(int i=0;i<n;i++) a[i]=i%k;
        double f = fit.evaluate(a);
        return new Result(a, f, 1);
    }
}