// src/main/java/org/example/Runner.java
package org.example;

import org.example.alg.*;
import org.example.core.*;
import org.example.util.*;

import java.nio.file.Path;
import java.util.List;

public class Runner {
    public static void runAll() throws Exception {
        Config cfg = Config.load();
        Path out = IO.ensureOutDir();

        double lambda = cfg.getDouble("levy_lambda");
        double gamma  = cfg.getDouble("levy_gamma");

        List<SchedulerFactory> algs = List.of(
                new SchedulerFactory("IWHOLF", (f) -> new IwholfScheduler(f, lambda, gamma)),
                new SchedulerFactory("WHO", (f) -> new WhoScheduler(f)),
                new SchedulerFactory("WOA", (f) -> new WoaScheduler(f)),
                new SchedulerFactory("PSO", (f) -> new PsoScheduler(f)),
                new SchedulerFactory("ACO", (f) -> new AcoScheduler(f)),
                new SchedulerFactory("RR",  (f) -> new RoundRobinScheduler(f))
        );

        List<Integer> small = cfg.getIntList("small_tasks");
        List<Integer> large = cfg.getIntList("large_tasks");

        Experiment exp = new Experiment(cfg, out);
        exp.runRegime("small", small, algs);

        exp.runRegime("large", large, algs);
    }

    public record SchedulerFactory(String name, java.util.function.Function<Fitness, MetaheuristicScheduler> ctor) {}
}