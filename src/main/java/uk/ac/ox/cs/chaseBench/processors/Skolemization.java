package uk.ac.ox.cs.chaseBench.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.cs.chaseBench.model.Atom;
import uk.ac.ox.cs.chaseBench.model.Constant;
import uk.ac.ox.cs.chaseBench.model.Domain;
import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.model.Term;
import uk.ac.ox.cs.chaseBench.model.Variable;

public abstract class Skolemization implements InputProcessor {
    protected final InputProcessor m_inputProcessor;

    public Skolemization(InputProcessor inputProcessor) {
        m_inputProcessor = inputProcessor;
    }

    public void startProcessing() {
        m_inputProcessor.startProcessing();
    }

    public void processRule(Rule rule) {
        // The head stays the same, so transform the body.
        List<Atom> newBody = new ArrayList<Atom>();
        Set<Variable> bodyVariables = new LinkedHashSet<Variable>();
        for (int bodyAtomIndex = 0; bodyAtomIndex < rule.getNumberOfBodyAtoms(); ++bodyAtomIndex) {
            Atom bodyAtom = rule.getBodyAtom(bodyAtomIndex);
            newBody.add(bodyAtom);
            addVariables(bodyAtom, bodyVariables);
        }
        // Determine the head variables and build the reachability graph.
        ReachabilityGraph reachabilityGraph = new ReachabilityGraph();
        Set<Variable> headVariables = new LinkedHashSet<Variable>();
        for (int headAtomIndex = 0; headAtomIndex < rule.getNumberOfHeadAtoms(); ++headAtomIndex) {
            Atom headAtom = rule.getHeadAtom(headAtomIndex);
            addVariables(headAtom, headVariables);
            for (int firstArgumentIndex = 0; firstArgumentIndex < headAtom.getNumberOfArguments(); ++firstArgumentIndex) {
                Term firstArgument = headAtom.getArgument(firstArgumentIndex);
                if (firstArgument instanceof Variable) {
                	Variable firstVariable = (Variable)firstArgument;
                	Set<Variable> firstVariableEdges = reachabilityGraph.getEdges(firstVariable);
                    for (int secondArgumentIndex = firstArgumentIndex + 1; secondArgumentIndex < headAtom.getNumberOfArguments(); ++secondArgumentIndex) {
                        Term secondArgument = headAtom.getArgument(secondArgumentIndex);
                        if (secondArgument instanceof Variable) {
                        	Variable secondVariable = (Variable)secondArgument;
                        	firstVariableEdges.add(secondVariable);
                            reachabilityGraph.getEdges(secondVariable).add(firstVariable);
                        }
                    }
                }
            }
        }
        // Determine the frontier variables.
        Set<Variable> frontierVariables = new LinkedHashSet<Variable>(headVariables);
        frontierVariables.retainAll(bodyVariables);
        // Now append the skolemization to the body for each existential variable.
        for (Variable headVariable : headVariables) {
            if (!frontierVariables.contains(headVariable)) {
                List<Term> skolemArguments = new ArrayList<Term>();
                skolemArguments.add(headVariable);
                skolemArguments.add(Constant.create("ex" + getNextExistentialCounter(), Domain.STRING));
                reachabilityGraph.appendReachableFrom(headVariable, frontierVariables, skolemArguments);
                newBody.add(Atom.create(Predicate.SKOLEM, skolemArguments.toArray(new Term[skolemArguments.size()])));
            }
        }
        // Create the new rule.
        Rule newRule = Rule.create(rule.getHeadAtoms(), newBody.toArray(new Atom[newBody.size()]));
        m_inputProcessor.processRule(newRule);
    }

    public void processFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
        m_inputProcessor.processFact(predicate, argumentLexicalForms, argumentsAreLabeledNulls);
    }

    public void endProcessing() {
        m_inputProcessor.endProcessing();
    }

    protected abstract int getNextExistentialCounter();
    
    protected static void addVariables(Atom atom, Set<Variable> variables) {
        for (int argumentIndex = 0; argumentIndex < atom.getNumberOfArguments(); ++argumentIndex) {
            Term argument = atom.getArgument(argumentIndex);
            if (argument instanceof Variable)
                variables.add((Variable)argument);
        }
    }

    protected static class ReachabilityGraph {
        protected final Map<Variable, Set<Variable>> m_edges;

        public ReachabilityGraph() {
            m_edges = new LinkedHashMap<Variable, Set<Variable>>();
        }
        public void appendReachableFrom(Variable start, Set<Variable> filter, List<Term> result) {
            Set<Variable> explored = new HashSet<Variable>();
            List<Variable> toExplore = new ArrayList<Variable>();
            toExplore.add(start);
            while (!toExplore.isEmpty()) {
                Variable current = toExplore.remove(toExplore.size() - 1);
                if (explored.add(current)) {
                    if (filter.contains(current))
                        result.add(current);
                    else
                        toExplore.addAll(m_edges.get(current));
                }
            }
        }
        public Set<Variable> getEdges(Variable variable) {
            Set<Variable> resultSet = m_edges.get(variable);
            if (resultSet == null) {
                resultSet = new LinkedHashSet<Variable>();
                m_edges.put(variable, resultSet);
            }
            return resultSet;
        }
    }
}
