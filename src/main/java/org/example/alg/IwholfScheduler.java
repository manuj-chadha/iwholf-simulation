// src/main/java/org/example/alg/IwholfScheduler.java
package org.example.alg;

import org.example.core.Fitness;
import org.example.core.MetaheuristicScheduler;
import org.example.core.Levy;

import java.util.*;

public class IwholfScheduler extends MetaheuristicScheduler {
    private final double lambda;
    private final double gamma;

    public IwholfScheduler(Fitness fit, double lambda, double gamma){
        super(fit);
        this.lambda = lambda;
        this.gamma = gamma;
    }

    @Override
    public Result solve(int iterations, int population, int groups){
        rng.setSeed(12345);
        int n = fit.taskCount(), k = fit.vmCount();
        List<int[]> pop = new ArrayList<>();
        for(int i=0;i<population;i++) pop.add(randomAssign());

        long t0 = System.currentTimeMillis();
        int[] best = pop.get(0);
        double bestF = fit.evaluate(best);

        for(int it=0; it<iterations; it++){
            pop.sort(Comparator.comparingDouble(fit::evaluate));
            List<List<int[]>> G = split(pop, groups);
            List<int[]> stallions = new ArrayList<>();
            for(List<int[]> g: G) stallions.add(g.get(0));

            double TDR = 1.0 - ((double)it)/iterations;

            List<int[]> nextPop = new ArrayList<>();
            for(int gi=0; gi<G.size(); gi++){
                var group = G.get(gi);
                int[] leader = stallions.get(gi);

                for(int i=0;i<group.size();i++){
                    int[] x = copy(group.get(i));
                    for(int d=0; d<n; d++){
                        double R = -2 + 4*rng.nextDouble();
                        double Z = rng.nextDouble();
                        double step = 2*Z*Math.cos(2*Math.PI*R*Z);
                        int xv = (int)Math.round(x[d] + step*(leader[d]-x[d])*(TDR));
                        x[d] = Fitness.clamp(xv, 0, k-1);
                    }

                    int otherGi = rng.nextInt(G.size());
                    if(otherGi==gi) otherGi = (gi+1)%G.size();
                    int[] mate = G.get(otherGi).get(rng.nextInt(G.get(otherGi).size()));
                    int[] child = new int[n];
                    for(int d=0; d<n; d++) child[d] = (x[d] + mate[d]) / 2;

                    for(int d=0; d<n; d++){
                        double s = gamma * Levy.step(lambda);
                        int jump = (int)Math.round(s * k);
                        if(rng.nextDouble()<0.5) child[d] = Fitness.clamp(child[d] + jump, 0, k-1);
                    }

                    nextPop.add(child);
                }
            }

            nextPop.addAll(stallions);
            for(int[] x: nextPop){
                if(rng.nextDouble()<0.1){
                    int d = rng.nextInt(n);
                    x[d] = rng.nextInt(k);
                }
            }

            nextPop.sort(Comparator.comparingDouble(fit::evaluate));
            pop = nextPop.subList(0, Math.min(population, nextPop.size()));

            double f0 = fit.evaluate(pop.get(0));
            if(f0 < bestF){
                best = copy(pop.get(0));
                bestF = f0;
            }
        }
        long ms = System.currentTimeMillis()-t0;
        return new Result(best, bestF, ms);
    }

    private static List<List<int[]>> split(List<int[]> pop, int g){
        List<List<int[]>> res = new ArrayList<>();
        for(int i=0;i<g;i++) res.add(new ArrayList<>());
        for(int i=0;i<pop.size();i++) res.get(i%g).add(pop.get(i));
        return res;
    }
}