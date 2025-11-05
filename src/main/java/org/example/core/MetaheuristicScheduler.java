package org.example.core;
import java.util.Random;

public abstract class MetaheuristicScheduler {
    protected final Fitness fit;
    protected final Random rng;

    public MetaheuristicScheduler(Fitness fit){
        this.fit = fit;
        this.rng = new Random();
    }

    public abstract Result solve(int iterations, int population, int groups);

    protected int[] randomAssign(){
        int n = fit.taskCount(), k = fit.vmCount();
        int[] a = new int[n];
        for(int i=0;i<n;i++) a[i]=rng.nextInt(k);
        return a;
    }

    protected static int[] copy(int[] x){ int[] y=new int[x.length]; System.arraycopy(x,0,y,0,x.length); return y; }

    public record Result(int[] bestAssign, double bestFitness, long millis){}
}