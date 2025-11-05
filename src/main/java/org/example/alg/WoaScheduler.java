package org.example.alg;

import org.example.core.*;

import java.util.*;

public class WoaScheduler extends MetaheuristicScheduler {
    public WoaScheduler(Fitness fit){ super(fit); }

    @Override
    public Result solve(int iterations, int population, int groupsIgnored){
        rng.setSeed(34567);
        int n = fit.taskCount(), k = fit.vmCount();
        List<int[]> pop = new ArrayList<>();
        for(int i=0;i<population;i++) pop.add(randomAssign());

        long t0 = System.currentTimeMillis();
        int[] best = pop.get(0);
        double bestF = fit.evaluate(best);

        for(int it=0; it<iterations; it++){
            double a = 2.0 - 2.0*((double)it/iterations);
            pop.sort(Comparator.comparingDouble(fit::evaluate));
            int[] gbest = pop.get(0);
            List<int[]> next = new ArrayList<>();

            for(int[] x : pop){
                int[] y = copy(x);
                for(int d=0; d<n; d++){
                    double A = 2*a*rng.nextDouble() - a;
                    double C = 2*rng.nextDouble();
                    int target = rng.nextDouble()<0.5 ? gbest[d] : pop.get(rng.nextInt(pop.size()))[d];
                    int dist = Math.abs(target - y[d]);
                    int newVal = (int)Math.round(target - A*dist);
                    if(rng.nextDouble()>=0.5){
                        // spiral update
                        double b=1, l = -1 + 2*rng.nextDouble();
                        int spiral = (int)Math.round( (target - y[d]) * Math.exp(b*l) * Math.cos(2*Math.PI*l) );
                        newVal = y[d] + spiral;
                    }
                    y[d] = Fitness.clamp(newVal, 0, k-1);
                }
                next.add(y);
            }
            next.sort(Comparator.comparingDouble(fit::evaluate));
            pop = next.subList(0, population);

            if(fit.evaluate(pop.get(0))<bestF){
                best = copy(pop.get(0)); bestF=fit.evaluate(best);
            }
        }
        long ms = System.currentTimeMillis()-t0;
        return new Result(best, bestF, ms);
    }
}