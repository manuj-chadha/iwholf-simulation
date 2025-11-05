package org.example.core;

import org.apache.commons.math3.distribution.NormalDistribution;

public class Levy {
    private static final NormalDistribution N = new NormalDistribution(0,1);

    // Mantegna's algorithm for symmetric Levy stable distribution
    public static double step(double lambda){
        double sigmaU = Math.pow(
                (gamma(1+lambda) * Math.sin(Math.PI*lambda/2)) / (gamma((1+lambda)/2) * lambda * Math.pow(2, (lambda-1)/2))
                , 1.0/lambda);
        double u = N.sample()*sigmaU;
        double v = N.sample();
        return u / Math.pow(Math.abs(v), 1.0/lambda);
    }

    // Rough gamma via Lanczos
    private static double gamma(double z){
        double[] p = {
                676.5203681218851, -1259.1392167224028,
                771.32342877765313, -176.61502916214059,
                12.507343278686905, -0.13857109526572012,
                9.9843695780195716e-6, 1.5056327351493116e-7
        };
        int g = 7;
        if(z < 0.5) return Math.PI / (Math.sin(Math.PI*z) * gamma(1-z));
        z -= 1;
        double x = 0.99999999999980993;
        for(int i=0;i<p.length;i++) x += p[i]/(z+i+1);
        double t = z + g + 0.5;
        return Math.sqrt(2*Math.PI) * Math.pow(t, z+0.5) * Math.exp(-t) * x;
    }
}