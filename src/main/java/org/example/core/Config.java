package org.example.core;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Config {
    private final Properties p;
    private Config(Properties p){this.p=p;}
    public static Config load() throws Exception {
        Properties p = new Properties();
        try(InputStream in = Config.class.getResourceAsStream("/config.properties")){
            p.load(in);
        }
        return new Config(p);
    }
    public int getInt(String k){return Integer.parseInt(p.getProperty(k));}
    public long getLong(String k){return Long.parseLong(p.getProperty(k));}
    public double getDouble(String k){return Double.parseDouble(p.getProperty(k));}
    public String get(String k){return p.getProperty(k);}
    public List<Integer> getIntList(String k){
        return Arrays.stream(p.getProperty(k).split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
    }
    public Random rng(){ return new Random(getLongOrDefault("seed", 101));}
    public long getLongOrDefault(String k,long d){return p.containsKey(k)?Long.parseLong(p.getProperty(k)):d;}
}