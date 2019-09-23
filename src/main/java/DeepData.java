import uk.ac.ox.cs.chaseBench.format.CommonCSVToFileFormat;
import uk.ac.ox.cs.chaseBench.model.Atom;
import uk.ac.ox.cs.chaseBench.model.LabeledNull;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.model.Term;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeepData {
    public static void main(String[] args) throws Exception {
        List<Rule> sttgds = Mappings.getRules(args[0]);
        String outFolder = args[1];
        for (Rule rule : sttgds) {
            printCSV(outFolder, rule.getBodyAtom(0));
        }
    }

    public static void printCSV(String outFolder, Atom atom) throws IOException {
        CommonCSVToFileFormat csvOut = new CommonCSVToFileFormat(new File(outFolder));
        List<String> lex = new ArrayList<>();
        List<Boolean> labNull = new ArrayList<>();
        Term[] arguments = atom.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            lex.add("X" + i);
            labNull.add(false);
        }
        csvOut.processFact(atom.getPredicate(), lex, labNull);
        csvOut.endProcessing();
    }
}
