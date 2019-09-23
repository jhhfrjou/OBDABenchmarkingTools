package uk.ac.ox.cs.chaseBench.processors;

import java.util.List;

import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;

public class NullInputProcessor implements InputProcessor {
    public static final InputProcessor INSTANCE = new NullInputProcessor();

    protected NullInputProcessor() {
    }

    public void startProcessing() {
    }

    public void processRule(Rule rule) {
    }

    public void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
    }

    public void endProcessing() {
    }

}
