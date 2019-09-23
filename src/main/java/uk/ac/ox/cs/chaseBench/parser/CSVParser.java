package uk.ac.ox.cs.chaseBench.parser;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.processors.InputProcessor;

public class CSVParser {
    protected static boolean readNextField(Reader reader, StringBuilder field, boolean[] isQuoted, boolean[] isLast) throws IOException {
        int c = reader.read();
        if (c == -1)
            return false;
        else {
            field.delete(0, field.length());
            if (c == '"') {
                isQuoted[0] = true;
                c = reader.read();
                while (c != -1) {
                    if (c == '"' && (c = reader.read()) != '"') {
                        if (c == ',')
                            isLast[0] = false;
                        else {
                            isLast[0] = true;
                            if (c == '\r') {
                                c = reader.read();
                                if (c != '\n')
                                    throw new IOException("Invalid line terminator.");
                            }
                            else if (c != '\n' && c != -1)
                                throw new IOException("Quote character (\") encountered within a field.");
                        }
                        return true;
                    }
                    field.append((char)c);
                    c = reader.read();
                }
                throw new IOException("Unterminated string.");
            }
            else {
                isQuoted[0] = false;
                while (c != -1 && c != ',' && c != '\n' && c != '\r') {
                    field.append((char)c);
                    c = reader.read();
                }
                if (c == ',')
                    isLast[0] = false;
                else {
                    isLast[0] = true;
                    if (c == '\r') {
                        c = reader.read();
                        if (c != '\n')
                            throw new IOException("Invalid line terminator.");
                    }
                }
                return true;
            }
        }
    }

    public static void parse(Reader reader, Predicate predicate, InputProcessor inputProcessor) throws IOException {
        List<String> argumentLexicalForms = new ArrayList<String>();
        List<Boolean> argumentsAreLabeledNulls = new ArrayList<Boolean>();
        StringBuilder field = new StringBuilder();
        boolean[] isQuoted = new boolean[1];
        boolean[] isLast = new boolean[1];
        while (readNextField(reader, field, isQuoted, isLast)) {
        	String token = field.toString();
        	if (!isQuoted[0] && token.startsWith("_:")) {
            	argumentLexicalForms.add(token.substring(2));
            	argumentsAreLabeledNulls.add(Boolean.TRUE);
            }
        	else {
            	argumentLexicalForms.add(token);
            	argumentsAreLabeledNulls.add(Boolean.FALSE);
        	}
            if (isLast[0]) {
                inputProcessor.processFact(predicate, argumentLexicalForms, argumentsAreLabeledNulls);
                argumentLexicalForms.clear();
                argumentsAreLabeledNulls.clear();
            }
        }
    }

    public static void parse(File file, InputProcessor inputProcessor) throws IOException {
    	String predicateName = file.getName();
    	int lastDot = predicateName.lastIndexOf('.');
    	if (lastDot != -1)
    		predicateName = predicateName.substring(0, lastDot);
        try (FileReader reader = new FileReader(file)) {
            parse(reader, Predicate.create(predicateName), inputProcessor);
        }
    }

    public static void parseDirectory(File directory, InputProcessor inputProcessor) throws IOException {
        File[] files = listInstanceFiles(directory);
        inputProcessor.startProcessing();
        Stream.of(files).parallel().forEach(file -> {
            try {
                parse(file, inputProcessor);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        inputProcessor.endProcessing();
    }

    public static File[] listInstanceFiles(File directory) {
        final String CSV = ".csv";
        final int CSVlength = CSV.length();
        File[] instanceFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                int length = name.length();
                if (length < CSVlength)
                    return false;
                for (int index = 0; index < CSVlength; ++index)
                    if (Character.toLowerCase(name.charAt(length - CSVlength + index)) != CSV.charAt(index))
                        return false;
                return true;
            }
        });
        if (instanceFiles == null)
        	return new File[0];
        else
        	return instanceFiles;
    }
}
