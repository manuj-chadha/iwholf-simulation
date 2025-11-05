package org.example.core;


import java.util.Arrays;

public class Fitness {
    private final double alpha, beta;
    private final double[] vmMips;
    private final long[] cloudletLen;
    private final int vmCount;

    public Fitness(double alpha, double beta, double[] vmMips, long[] cloudletLen){
        this.alpha = alpha; this.beta = beta;
        this.vmMips = vmMips; this.cloudletLen = cloudletLen;
        this.vmCount = vmMips.length;
    }

    public int vmCount(){ return vmCount; }
    public int taskCount(){ return cloudletLen.length; }

    public double evaluate(int[] assign){
        // per-VM total time = sum(length/mips)
        double[] vmTimes = new double[vmCount];
        Arrays.fill(vmTimes, 0.0);
        for(int i=0;i<assign.length;i++){
            int vm = clamp(assign[i], 0, vmCount-1);
            vmTimes[vm] += ((double)cloudletLen[i])/vmMips[vm];
        }
        double makespan = 0.0, sumCT = 0.0;
        for(double t: vmTimes){ makespan = Math.max(makespan, t); sumCT+=t; }
        if(makespan==0) return Double.MAX_VALUE;
        double m = vmTimesMean(vmTimes);
        double doi = m==0?0: (max(vmTimes)-min(vmTimes))/m;
        double utilization = sumCT/(makespan*vmCount);
        // scalarization: lower is better; negate utilization to maximize
        return alpha*makespan + beta*(1.0 - utilization) + 0.05*doi;
    }

    public static double doi(double[] vmTimes){
        double m = vmTimesMean(vmTimes);
        if(m==0) return 0;
        return (max(vmTimes)-min(vmTimes))/m;
    }

    public static double makespan(double[] vmTimes){
        return max(vmTimes);
    }

    public static double utilization(double[] vmTimes){
        double sum=0, mk=max(vmTimes);
        for(double t: vmTimes) sum+=t;
        return mk==0?0:sum/(mk*vmTimes.length);
    }

    public double[] vmTimesFromAssign(int[] assign){
        double[] vmTimes = new double[vmCount];
        for(int i=0;i<assign.length;i++){
            int vm = clamp(assign[i],0,vmCount-1);
            vmTimes[vm] += ((double)cloudletLen[i])/vmMips[vm];
        }
        return vmTimes;
    }

    public static int clamp(int v,int lo,int hi){ return Math.max(lo, Math.min(hi, v)); }
    private static double max(double[] a){ double m=a[0]; for(double x:a) m=Math.max(m,x); return m; }
    private static double min(double[] a){ double m=a[0]; for(double x:a) m=Math.min(m,x); return m; }
    private static double vmTimesMean(double[] a){ double s=0; for(double x:a)s+=x; return s/a.length; }
}