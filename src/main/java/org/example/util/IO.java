package org.example.util;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.nio.file.*;
import java.util.List;

public class IO {
    public static Path ensureOutDir() throws Exception {
        Path out = Paths.get("out");
        if(!Files.exists(out)) Files.createDirectories(out);
        return out;
    }

    public static <T> void writeCSV(Path file, List<?> rows) throws Exception {
        try(CSVWriter w = new CSVWriter(new FileWriter(file.toFile()))){
            if(rows.isEmpty()) return;
            Object first = rows.get(0);
            String[] header = Reflect.header(first);
            w.writeNext(header);
            for(Object r : rows){
                w.writeNext(Reflect.row(r, header));
            }
        }
    }
}