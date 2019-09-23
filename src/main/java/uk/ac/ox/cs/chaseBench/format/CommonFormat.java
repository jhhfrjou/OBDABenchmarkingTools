package uk.ac.ox.cs.chaseBench.format;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import uk.ac.ox.cs.chaseBench.model.Constant;
import uk.ac.ox.cs.chaseBench.model.DatabaseSchema;
import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.PredicateSchema;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.processors.InputProcessor;

public class CommonFormat implements InputProcessor {
	protected final DatabaseSchema m_databaseSchema;
    protected final PrintWriter m_output;

    public CommonFormat(DatabaseSchema databaseSchema, Writer output) {
    	m_databaseSchema = databaseSchema;
    	if (output == null)
    		m_output = null;
    	else if (output instanceof PrintWriter)
            m_output = (PrintWriter)output;
        else
            m_output = new PrintWriter(output);
    }

    public void startProcessing() {
    }

    public void processRule(Rule rule) {
    	rule.print(m_output);
    	m_output.println();
    }

    public void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
    	PredicateSchema predicateSchema = m_databaseSchema.getPredicateSchema(predicate);
    	if (predicateSchema == null)
    		throw new IllegalArgumentException("Cannot find the schema for predicate '" + predicate.getName() + "'.");
    	predicate.print(m_output);
    	m_output.print('(');
    	for (int index = 0; index < argumentLexicalForms.size(); ++index) {
    		if (index != 0)
    			m_output.print(',');
    		if (argumentsAreLabeledNulls.get(index)) {
    			m_output.print("_:");
    			m_output.print(argumentLexicalForms.get(index));
    		}
    		else
    			Constant.print(m_output, argumentLexicalForms.get(index), predicateSchema.getColumnDomain(index));
    	}
        m_output.println(") .");
    }

    public void endProcessing() {
    }
}
