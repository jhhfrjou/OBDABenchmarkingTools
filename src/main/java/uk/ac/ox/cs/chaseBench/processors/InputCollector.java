package uk.ac.ox.cs.chaseBench.processors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import uk.ac.ox.cs.chaseBench.model.Atom;
import uk.ac.ox.cs.chaseBench.model.DatabaseSchema;
import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;

public class InputCollector implements InputProcessor {
    protected final DatabaseSchema m_databaseSchema;
    protected final Collection<Rule> m_rules;
    protected final ConcurrentLinkedQueue<Atom> m_facts;

    public InputCollector(DatabaseSchema databaseSchema, Collection<Rule> rules, ConcurrentLinkedQueue<Atom> facts) {
        m_databaseSchema = databaseSchema;
        m_rules = rules;
        m_facts = facts;
    }

    public void startProcessing() {
    }

    public void processRule(Rule rule) {
        if (m_rules != null)
            m_rules.add(rule);
    }

    public void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
        if (m_facts != null) {
            m_facts.add(m_databaseSchema.toFact(predicate, argumentLexicalForms, argumentsAreLabeledNulls));
        }


    }

    public void endProcessing() {
    }


}
