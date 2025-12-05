package org.example.odes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvParser {
    public static List<Map<String, String>> parseCSV(File file) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();
        try (Reader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'))) {
            for (CSVRecord csvRecord : csvParser) {
                records.add(csvRecord.toMap());
            }
        }
        return records;
    }
}
