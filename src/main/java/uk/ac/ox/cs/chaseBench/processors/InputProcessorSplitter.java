package uk.ac.ox.cs.chaseBench.processors;

import java.util.List;

import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;

public class InputProcessorSplitter implements InputProcessor {
    protected final InputProcessor m_inputProcessor1;
    protected final InputProcessor m_inputProcessor2;

    public InputProcessorSplitter(InputProcessor inputProcessor1, InputProcessor inputProcessor2) {
        m_inputProcessor1 = inputProcessor1;
        m_inputProcessor2 = inputProcessor2;
    }

    public void startProcessing() {
        m_inputProcessor1.startProcessing();
        m_inputProcessor2.startProcessing();
    }

    public void processRule(Rule rule) {
        m_inputProcessor1.processRule(rule);
        m_inputProcessor2.processRule(rule);
    }

    public void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
        m_inputProcessor1.processFact(predicate, argumentLexicalForms, argumentsAreLabeledNulls);
        m_inputProcessor2.processFact(predicate, argumentLexicalForms, argumentsAreLabeledNulls);
    }

    public void endProcessing() {
        m_inputProcessor1.endProcessing();
        m_inputProcessor2.endProcessing();
    }

}
