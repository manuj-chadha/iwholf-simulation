package org.example.util;

import java.lang.reflect.Field;
import java.util.*;

public class Reflect {
    public static String[] header(Object o){
        return Arrays.stream(o.getClass().getFields()).map(Field::getName).toArray(String[]::new);
    }
    public static String[] row(Object o, String[] header){
        try{
            String[] row = new String[header.length];
            for(int i=0;i<header.length;i++){
                Field f = o.getClass().getField(header[i]);
                Object v = f.get(o);
                row[i] = String.valueOf(v);
            }
            return row;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}