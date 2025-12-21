package org.example.alg;

import org.example.core.*;

import java.util.*;

public class AcoScheduler extends MetaheuristicScheduler {

    // ✅ constructor now takes a seed
    public AcoScheduler(Fitness fit, long seed) {
        super(fit, seed);
    }

    @Override
    public Result solve(int iterations, int ants, int groupsIgnored) {
        // ❌ removed: rng.setSeed(56789);

        int n = fit.taskCount(), k = fit.vmCount();
        double[][] tau = new double[n][k];
        for (int i = 0; i < n; i++) Arrays.fill(tau[i], 1.0);
        double alpha = 1.0, betaH = 2.0, rho = 0.1;

        int[] best = randomAssign();
        double bestF = fit.evaluate(best);
        long t0 = System.currentTimeMillis();

        for (int it = 0; it < iterations; it++) {
            List<int[]> sols = new ArrayList<>();
            List<Double> fs = new ArrayList<>();
            for (int a = 0; a < ants; a++) {
                int[] s = new int[n];
                for (int i = 0; i < n; i++) {
                    // heuristic: prefer VMs with higher MIPS index
                    double[] prob = new double[k];
                    double sum = 0;
                    for (int vm = 0; vm < k; vm++) {
                        double eta = 1.0 + vm; // simple heuristic
                        prob[vm] = Math.pow(tau[i][vm], alpha) * Math.pow(eta, betaH);
                        sum += prob[vm];
                    }
                    double r = rng.nextDouble() * sum, acc = 0;
                    int choice = 0;
                    for (int vm = 0; vm < k; vm++) {
                        acc += prob[vm];
                        if (r <= acc) {
                            choice = vm;
                            break;
                        }
                    }
                    s[i] = choice;
                }
                sols.add(s);
                fs.add(fit.evaluate(s));
            }
            // evaporation
            for (int i = 0; i < n; i++)
                for (int vm = 0; vm < k; vm++)
                    tau[i][vm] *= (1.0 - rho);

            // reinforce best in this iteration
            int ib = argmin(fs);
            int[] sb = sols.get(ib);
            double fb = fs.get(ib);
            for (int i = 0; i < n; i++)
                tau[i][sb[i]] += 1.0 / (1.0 + fb);

            if (fb < bestF) {
                bestF = fb;
                best = sb.clone();
            }
        }
        long ms = System.currentTimeMillis() - t0;
        return new Result(best, bestF, ms);
    }

    private static int argmin(List<Double> a) {
        int idx = 0;
        for (int i = 1; i < a.size(); i++)
            if (a.get(i) < a.get(idx)) idx = i;
        return idx;
    }
}