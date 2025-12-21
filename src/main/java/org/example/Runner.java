package org.example;

import org.example.alg.*;
import org.example.core.*;
import org.example.util.*;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

public class Runner {
    public static void runAll() throws Exception {
        Config cfg = Config.load();
        Path out = IO.ensureOutDir();

        double lambda = cfg.getDouble("levy_lambda");
        double gamma  = cfg.getDouble("levy_gamma");

        List<SchedulerFactory> algs = List.of(
                new SchedulerFactory("IWHOLF", (f, seed) -> new IwholfScheduler(f, lambda, gamma, seed)),
                new SchedulerFactory("WHO",    (f, seed) -> new WhoScheduler(f, seed)),
                new SchedulerFactory("WOA",    (f, seed) -> new WoaScheduler(f, seed)),
                new SchedulerFactory("PSO",    (f, seed) -> new PsoScheduler(f, seed)),
                new SchedulerFactory("ACO",    (f, seed) -> new AcoScheduler(f, seed)),
                new SchedulerFactory("RR",     (f, seed) -> new RoundRobinScheduler(f, seed))
        );

        List<Integer> small = cfg.getIntList("small_tasks");
        List<Integer> large = cfg.getIntList("large_tasks");

        Experiment exp = new Experiment(cfg, out);
        exp.runRegime("small", small, algs);
        exp.runRegime("large", large, algs);
    }

    public record SchedulerFactory(
            String name,
            BiFunction<Fitness, Long, MetaheuristicScheduler> ctor
    ) {}
}