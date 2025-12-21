package org.example.core;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.example.Runner.SchedulerFactory;
import org.example.util.Aggregator;
import org.example.util.IO;

import java.nio.file.Path;
import java.util.*;

public class Experiment {
    private final Config cfg;
    private final Path out;

    public Experiment(Config cfg, Path out) {
        this.cfg = cfg;
        this.out = out;
    }

    public void runRegime(String tag, List<Integer> taskCounts, List<SchedulerFactory> algs) throws Exception {
        int repeats = cfg.getInt("repeats");
        double alpha = cfg.getDouble("alpha_makespan");
        double beta = cfg.getDouble("beta_util");

        System.out.println("\n=== Regime: " + tag + " | Tasks: " + taskCounts + " ===");

        for (int tasks : taskCounts) {
            System.out.println("\n--- Running task size: " + tasks + " ---");
            List<ResultRow> rows = new ArrayList<>();

            for (int r = 0; r < repeats; r++) {
                long seed = cfg.getLongOrDefault("seed", 101) + r;
                Scenario setup = buildScenario(seed, tasks);
                Fitness fitness = new Fitness(alpha, beta, setup.vmMips, setup.cloudletLens);

                for (SchedulerFactory sf : algs) {
                    System.out.println("Starting algorithm: " + sf.name() + " [repeat " + (r + 1) + "]");
                    long t0 = System.currentTimeMillis();

                    MetaheuristicScheduler scheduler = sf.ctor().apply(fitness, seed);

                    var res = scheduler.solve(cfg.getInt("iterations"), cfg.getInt("population"), cfg.getInt("groups"));
                    long schedMs = System.currentTimeMillis() - t0;

                    double[] vmTimes = fitness.vmTimesFromAssign(res.bestAssign());
                    double mk = Fitness.makespan(vmTimes);
                    double doi = Fitness.doi(vmTimes);
                    double utl = Fitness.utilization(vmTimes);

                    double mkSim = runCloudSim(setup, res.bestAssign());
                    System.out.println("Completed " + sf.name()
                            + " | mk=" + String.format("%.4f", mk)
                            + " mkSim=" + String.format("%.4f", mkSim));

                    rows.add(new ResultRow(tag, tasks, sf.name(), r, mk, mkSim, doi, utl, res.bestFitness(), schedMs));
                }
            }
            IO.writeCSV(out.resolve("results_" + tag + "_" + tasks + ".csv"), rows);
            System.out.println("Saved results for task size " + tasks);
        }

        System.out.println("Aggregating results for regime: " + tag);
        Aggregator.aggregateDir(out,
                "results_" + tag + "_*.csv",
                out.resolve("summary_" + tag + ".csv"),
                cfg.getDouble("success_rate_threshold_ratio"));
        System.out.println("Summary created: summary_" + tag + ".csv");
    }

    private static record Scenario(double[] vmMips, long[] cloudletLens, List<Vm> vms, List<Cloudlet> cloudlets, List<Host> hosts) {}

    private Scenario buildScenario(long seed, int tasks) {
        Random rng = new Random(seed);
        int hosts = cfg.getInt("hosts");
        int vmsCount = cfg.getInt("vms");
        int hostMips = cfg.getInt("hostMips");
        int hostPes = cfg.getInt("hostPes");
        int hostRam = cfg.getInt("hostRam");
        long hostBw = cfg.getLong("hostBw");
        long hostStore = cfg.getLong("hostStorage");

        List<Integer> vmMipsOptions = Arrays.stream(cfg.get("vmMipsOptions").split(","))
                .map(String::trim).map(Integer::parseInt).toList();
        int vmPes = cfg.getInt("vmPes");
        int vmRam = cfg.getInt("vmRam");
        long vmBw = cfg.getLong("vmBw");
        long vmStore = cfg.getLong("vmStorage");

        int cMin = cfg.getInt("cloudletLengthMin");
        int cMax = cfg.getInt("cloudletLengthMax");
        int cf = cfg.getInt("cloudletFileSize");
        int co = cfg.getInt("cloudletOutputSize");
        int cpes = cfg.getInt("cloudletPes");

        List<Host> hostList = new ArrayList<>();
        for (int i = 0; i < hosts; i++) {
            List<Pe> pes = new ArrayList<>();
            for (int p = 0; p < hostPes; p++) pes.add(new PeSimple(hostMips));
            Host h = new HostSimple(hostRam, hostBw, hostStore, pes);
            h.setVmScheduler(new org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared());
            hostList.add(h);
        }

        List<Vm> vms = new ArrayList<>();
        double[] vmMips = new double[vmsCount];
        for (int i = 0; i < vmsCount; i++) {
            int mips = vmMipsOptions.get(rng.nextInt(vmMipsOptions.size()));
            Vm vm = new VmSimple(mips, vmPes);
            vm.setRam(vmRam).setBw(vmBw).setSize(vmStore);
            vm.setCloudletScheduler(new org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared());
            vms.add(vm);
            vmMips[i] = mips;
        }

        UtilizationModel umCpu = new UtilizationModelDynamic(0.5);
        UtilizationModel umRam = new UtilizationModelDynamic(0.2);
        UtilizationModel umBw = new UtilizationModelDynamic(0.2);

        List<Cloudlet> cloudlets = new ArrayList<>();
        long[] clLens = new long[tasks];
        for (int i = 0; i < tasks; i++) {
            long len = cMin + rng.nextInt(cMax - cMin + 1);
            Cloudlet cl = new CloudletSimple(i, len, cpes);
            cl.setFileSize(cf)
                    .setOutputSize(co)
                    .setUtilizationModelCpu(umCpu)
                    .setUtilizationModelRam(umRam)
                    .setUtilizationModelBw(umBw);
            cloudlets.add(cl);
            clLens[i] = len;
        }

        return new Scenario(vmMips, clLens, vms, cloudlets, hostList);
    }

    private double runCloudSim(Scenario sc, int[] assign) {
        CloudSim sim = new CloudSim();
        new DatacenterSimple(sim, sc.hosts, new VmAllocationPolicySimple());
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(sim);

        broker.submitVmList(sc.vms);
        broker.submitCloudletList(sc.cloudlets);

        for (int i = 0; i < sc.cloudlets.size(); i++) {
            int vmIdx = Fitness.clamp(assign[i], 0, sc.vms.size() - 1);
            broker.bindCloudletToVm(sc.cloudlets.get(i), sc.vms.get(vmIdx));
        }

        sim.start();
        return sc.cloudlets.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0.0);
    }

    public static class ResultRow {
        public final String regime;
        public final int tasks;
        public final String algo;
        public final int repeat;
        public final double makespanAnalytic;
        public final double makespanSim;
        public final double doi;
        public final double utilization;
        public final double bestFitness;
        public final long schedulerMs;

        public ResultRow(String regime, int tasks, String algo, int repeat,
                         double mkA, double mkS, double doi, double util,
                         double bestF, long schedMs) {
            this.regime = regime;
            this.tasks = tasks;
            this.algo = algo;
            this.repeat = repeat;
            this.makespanAnalytic = mkA;
            this.makespanSim = mkS;
            this.doi = doi;
            this.utilization = util;
            this.bestFitness = bestF;
            this.schedulerMs = schedMs;
        }
    }
}