package org.example.alg;

import org.example.core.*;
import java.util.*;

public class PsoScheduler extends MetaheuristicScheduler {
    public PsoScheduler(Fitness fit){ super(fit); }

    @Override
    public Result solve(int iterations, int population, int groupsIgnored){
        rng.setSeed(45678);
        int n = fit.taskCount(), k = fit.vmCount();
        List<int[]> pos = new ArrayList<>();
        List<int[]> vel = new ArrayList<>();
        for(int i=0;i<population;i++){
            pos.add(randomAssign());
            vel.add(new int[n]);
        }

        List<int[]> pbest = new ArrayList<>();
        for (int[] p : pos)
            pbest.add(copy(p));

        double[] pbestF = new double[population];
        for(int i=0;i<population;i++)
            pbestF[i] = fit.evaluate(pbest.get(i));

        int gIdx = argmin(pbestF);
        int[] gbest = copy(pbest.get(gIdx));
        double gbestF = pbestF[gIdx];

        long t0 = System.currentTimeMillis();
        double w = 0.7, c1 = 1.5, c2 = 1.5;

        for(int it = 0; it < iterations; it++){
            for(int i = 0; i < population; i++){
                int[] x = pos.get(i);
                int[] v = vel.get(i);
                for(int d = 0; d < n; d++){
                    double r1 = rng.nextDouble(), r2 = rng.nextDouble();
                    double newV = w * v[d] + c1 * r1 * (pbest.get(i)[d] - x[d]) + c2 * r2 * (gbest[d] - x[d]);
                    v[d] = (int)Math.round(newV);
                    x[d] = Fitness.clamp(x[d] + v[d], 0, k - 1);
                }

                double f = fit.evaluate(x);
                if(f < pbestF[i]) {
                    pbestF[i] = f;
                    pbest.set(i, copy(x));
                }
                if(f < gbestF) {
                    gbestF = f;
                    gbest = copy(x);
                }
            }
        }

        long ms = System.currentTimeMillis() - t0;
        return new Result(gbest, gbestF, ms);
    }

    private static int argmin(double[] a){
        int idx = 0;
        for(int i = 1; i < a.length; i++)
            if(a[i] < a[idx]) idx = i;
        return idx;
    }
}