package uk.ac.ox.cs.chaseBench.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import uk.ac.ox.cs.chaseBench.model.Predicate;

public class CommonCSVToFileFormat extends CommonCSVFormat {
    protected final File m_dataDirectory;

    public CommonCSVToFileFormat(File dataDirectory) throws IOException {
        m_dataDirectory = dataDirectory;
    }

    protected Writer newDataOutput(Predicate predicate) {
        try {
            return new FileWriter(new File(m_dataDirectory, predicate.getName() + ".csv"));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
