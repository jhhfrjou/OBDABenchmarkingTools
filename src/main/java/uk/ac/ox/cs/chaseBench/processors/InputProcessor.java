package uk.ac.ox.cs.chaseBench.processors;

import java.util.List;

import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;

public interface InputProcessor {
    void startProcessing();
    void processRule(Rule rule);
    void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls);
    void endProcessing();
}
