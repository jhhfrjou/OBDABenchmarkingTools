package uk.ac.ox.cs.chaseBench.format;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.processors.InputProcessor;

public abstract class CommonCSVFormat implements InputProcessor {
    protected final Map<Predicate, PrintWriter> m_dataOutputs;

    public CommonCSVFormat() {
        m_dataOutputs = new LinkedHashMap<Predicate, PrintWriter>();
    }

    public void startProcessing() {
    }

    public void processRule(Rule rule) {
    }

    public void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
        PrintWriter dataOutput = m_dataOutputs.get(predicate);
        if (dataOutput == null) {
            Writer output = newDataOutput(predicate);
            if (output instanceof PrintWriter)
            	dataOutput = (PrintWriter)output;
            else
            	dataOutput = new PrintWriter(output);
            m_dataOutputs.put(predicate, dataOutput);
        }
        for (int argumentIndex = 0; argumentIndex < argumentLexicalForms.size(); ++argumentIndex) {
            if (argumentIndex != 0)
            	dataOutput.print(',');
            String lexicalForm = argumentLexicalForms.get(argumentIndex);
            if (argumentsAreLabeledNulls.get(argumentIndex)) {
            	dataOutput.print("_:");
            	dataOutput.print(lexicalForm);
            }
            else {
            	if (lexicalForm.indexOf(',') != -1) {
            		dataOutput.print('"');
            		for (int index = 0; index < lexicalForm.length(); ++index) {
            			char c = lexicalForm.charAt(index);
            			if (c == '"')
            				dataOutput.print('"');
            			dataOutput.print(c);
            		}
            	}
            	else
            		dataOutput.print(lexicalForm);
            }
        }
        dataOutput.println();
    }

    public void endProcessing() {
    	for (PrintWriter dataOutput : m_dataOutputs.values())
            dataOutput.close();
    }

    protected abstract Writer newDataOutput(Predicate predicate);
}
