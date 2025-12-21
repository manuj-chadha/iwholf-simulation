package org.example.util;

import com.opencsv.CSVWriter;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Aggregator {
    public static void aggregateDir(Path dir, String glob, Path out, double thrRatio) throws Exception {
        List<Path> files = Files.list(dir)
                .filter(p -> p.getFileName().toString().matches(globToRegex(glob)))
                .toList();

        List<Map<String,String>> rows = new ArrayList<>();
        for (Path f : files) rows.addAll(readCSV(f));

        if (rows.isEmpty()) return;

        Map<String, List<Map<String,String>>> groups = rows.stream()
                .filter(r -> r.get("regime") != null && r.get("tasks") != null && r.get("algo") != null)
                .collect(Collectors.groupingBy(r -> r.get("regime") + "|" + r.get("tasks") + "|" + r.get("algo")));

        List<String[]> outRows = new ArrayList<>();
        outRows.add(new String[]{"regime","tasks","algo","n","mk_mean","mk_std","doi_mean","util_mean","sched_ms_mean","success_rate"});

        Map<String, Double> thresholds = new HashMap<>();
        Map<String, Double> meanMkPerGroup = new HashMap<>();

        for (var e : groups.entrySet()) {
            double[] x = e.getValue().stream()
                    .map(r -> safeParse(r.get("makespanAnalytic")))
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            meanMkPerGroup.put(e.getKey(), mean(x));
        }

        Map<String, Double> bestPerSetting = new HashMap<>();
        for (String key : groups.keySet()) {
            String setting = key.substring(0, key.lastIndexOf('|'));
            double mk = meanMkPerGroup.get(key);
            bestPerSetting.merge(setting, mk, Math::min);
        }
        for (String setting : bestPerSetting.keySet()) {
            thresholds.put(setting, thrRatio * bestPerSetting.get(setting));
        }

        for (var e : groups.entrySet()) {
            var list = e.getValue();
            String[] spl = e.getKey().split("\\|");
            String setting = spl[0] + "|" + spl[1];
            double thr = thresholds.getOrDefault(setting, Double.MAX_VALUE);

            double[] mk = list.stream().map(r -> safeParse(r.get("makespanAnalytic"))).filter(Objects::nonNull).mapToDouble(Double::doubleValue).toArray();
            double[] doi = list.stream().map(r -> safeParse(r.get("doi"))).filter(Objects::nonNull).mapToDouble(Double::doubleValue).toArray();
            double[] util = list.stream().map(r -> safeParse(r.get("utilization"))).filter(Objects::nonNull).mapToDouble(Double::doubleValue).toArray();
            double[] ms = list.stream().map(r -> safeParse(r.get("schedulerMs"))).filter(Objects::nonNull).mapToDouble(Double::doubleValue).toArray();

            long success = Arrays.stream(mk).filter(v -> v <= thr).count();
            outRows.add(new String[]{
                    spl[0], spl[1], spl[2], String.valueOf(list.size()),
                    String.format("%.4f", mean(mk)), String.format("%.4f", std(mk)),
                    String.format("%.4f", mean(doi)), String.format("%.4f", mean(util)),
                    String.format("%.2f", mean(ms)), String.format("%.4f", (double) success / Math.max(1, mk.length))
            });
        }

        try (CSVWriter w = new CSVWriter(new FileWriter(out.toFile()))) {
            for (String[] r : outRows) w.writeNext(r);
        }
    }

    private static Double safeParse(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return null; }
    }

    private static double mean(double[] a) { double s = 0; for (double x : a) s += x; return a.length == 0 ? 0 : s / a.length; }
    private static double std(double[] a) { double m = mean(a), s = 0; for (double x : a) s += (x - m) * (x - m); return a.length < 2 ? 0 : Math.sqrt(s / (a.length - 1)); }

    private static List<Map<String,String>> readCSV(Path f) throws Exception {
        List<Map<String,String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(f)) {
            String header = br.readLine(); if (header == null) return rows;
//            String[] h = header.split(",");
            String[] h = header.replace("\"", "").split(",");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
//                String[] v = line.split(",", -1);
                String[] v = line.replace("\"", "").split(",", -1);
                Map<String,String> m = new HashMap<>();
                for (int i = 0; i < h.length && i < v.length; i++) m.put(h[i], v[i]);
                rows.add(m);
            }
        }
        return rows;
    }

    private static String globToRegex(String glob) {
        return glob.replace(".", "\\.").replace("*", ".*");
    }
}