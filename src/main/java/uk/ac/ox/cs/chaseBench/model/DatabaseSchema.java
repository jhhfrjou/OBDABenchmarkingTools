package uk.ac.ox.cs.chaseBench.model;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

public class DatabaseSchema {
	protected final Map<Predicate, PredicateSchema> m_predicateSchemas;
	
	public DatabaseSchema() {
		m_predicateSchemas = new TreeMap<Predicate, PredicateSchema>(Predicate.COMPARATOR);
	}
	
	public void clear() {
		m_predicateSchemas.clear();
	}
	
	public void addPredicateSchema(Predicate predicate, boolean isTarget, String[] columnNames, Domain[] columnDomains) {
		if (m_predicateSchemas.containsKey(predicate))
			throw new IllegalArgumentException("Schema for predicate '" + predicate.getName() + "' already exists.");
		m_predicateSchemas.put(predicate, new PredicateSchema(predicate, isTarget, columnNames, columnDomains));
	}
	
	public PredicateSchema getPredicateSchema(Predicate predicate) {
		return m_predicateSchemas.get(predicate);
	}
	
	public Set<Predicate> getPredicates() {
		return m_predicateSchemas.keySet();
	}
	
	public Atom toFact(Predicate predicate, List<String> argumentLexicalForms, List<Boolean> argumentsAreLabeledNulls) {
		PredicateSchema predicateSchema = getPredicateSchema(predicate);
		if (predicateSchema == null)
			throw new IllegalArgumentException("Cannot find the schema for predicate '" + predicate.getName() + "'.");
		if (predicateSchema.getArity() != argumentLexicalForms.size())
			throw new IllegalArgumentException("The arity of the fact does not match the arity of the predicate in the schema.");
		Term[] arguments = new Term[argumentLexicalForms.size()];
		for (int index = 0; index < argumentLexicalForms.size(); ++index) {
			if (argumentsAreLabeledNulls.get(index))
				arguments[index] = LabeledNull.create(argumentLexicalForms.get(index));
			else
				arguments[index] = Constant.create(argumentLexicalForms.get(index), predicateSchema.getColumnDomain(index));
		}
		return Atom.create(predicate, arguments);
	}
	
	public boolean isCorrectlyTyped(Atom atom, Map<Variable, Domain> variableTypes, boolean isTarget) {
		Predicate predicate = atom.getPredicate();
		if (!Predicate.SKOLEM.equals(predicate)) {
			if (Predicate.EQUALS.equals(predicate)) {
				if (atom.getNumberOfArguments() != 2)
					return false;
				Term argument0 = atom.getArgument(0);
				Term argument1 = atom.getArgument(1);
				if (argument0 instanceof LabeledNull || argument1 instanceof LabeledNull)
					return true;
				Domain domain0 = (argument0 instanceof Variable ? variableTypes.get(argument0) : ((Constant)argument0).getDomain());
				Domain domain1 = (argument1 instanceof Variable ? variableTypes.get(argument1) : ((Constant)argument1).getDomain());
                return domain0 != null && domain1 != null && domain0.equals(domain1);
			}
			else {
				PredicateSchema predicateSchema = getPredicateSchema(predicate);
				if (predicateSchema == null || predicateSchema.isTarget() != isTarget)
					return false;
				for (int argumentIndex = 0; argumentIndex < atom.getNumberOfArguments(); ++argumentIndex) {
					Domain domain = predicateSchema.getColumnDomain(argumentIndex);
					Term term = atom.getArgument(argumentIndex);
					if (term instanceof Constant) {
						Constant constant = (Constant)term;
						if (!constant.getDomain().equals(domain))
							return false;
					}
					else if (term instanceof Variable) {
						Variable variable = (Variable)term;
						Domain currentDomain = variableTypes.get(variable);
						if (currentDomain == null)
							variableTypes.put(variable, domain);
						else if (!currentDomain.equals(domain))
							return false;
					}
				}
			}
		}
		return true;
	}
	
	public boolean isCorrectlyTyped(Rule rule, boolean bodyIsTarget) {
		Map<Variable, Domain> variableTypes = new HashMap<Variable, Domain>();
		for (int bodyIndex = 0; bodyIndex < rule.getNumberOfBodyAtoms(); ++bodyIndex)
			if (!isCorrectlyTyped(rule.getBodyAtom(bodyIndex), variableTypes, bodyIsTarget))
				return false;
		for (int headIndex = 0; headIndex < rule.getNumberOfHeadAtoms(); ++headIndex)
			if (!isCorrectlyTyped(rule.getHeadAtom(headIndex), variableTypes, true))
				return false;
		return true;
	}
	
	public void inferMissingPredicateSchemas(Collection<Rule> sourceTargetDepeendencies, Collection<Rule> targetDepeendencies, Collection<Predicate> unknownPredicates) {
		PredicatePositionGraph predicatePositionGraph = new PredicatePositionGraph();
		for (Rule rule : sourceTargetDepeendencies)
			predicatePositionGraph.addRule(rule, false);
		for (Rule rule : targetDepeendencies)
			predicatePositionGraph.addRule(rule, true);
		predicatePositionGraph.inferMissingPredicateSchemas(unknownPredicates);
	}
	
	public void save(File file, boolean saveTarget) throws IOException {
		FileWriter writer = new FileWriter(file);
		try {
			save(writer, saveTarget);
		}
		finally {
			writer.close();
		}
	}

	public void save(Writer writer, boolean saveTarget) throws IOException {
		boolean first = true;
		PrintWriter output = writer instanceof PrintWriter ? (PrintWriter)writer : new PrintWriter(writer);
		for (PredicateSchema predicateSchema : m_predicateSchemas.values()) {
			if (predicateSchema.isTarget() == saveTarget) {
				if (first)
					first = false;
				else
					output.println();
				output.print(predicateSchema.getPredicate().getName());
				output.print(" {");
				for (int columnIndex = 0; columnIndex < predicateSchema.getArity(); ++columnIndex) {
					if (columnIndex != 0)
						output.print(',');
					output.println();
					output.print("    ");
					output.print(predicateSchema.getColumnName(columnIndex));
					output.print(" : ");
					output.print(predicateSchema.getColumnDomain(columnIndex).toString());
				}
				output.println();
				output.println("}");
			}
		}
	}
	
	public void load(File file, boolean isTarget) throws IOException {
		FileReader reader = new FileReader(file);
		try {
			load(reader, isTarget);
		}
		finally {
			reader.close();
		}
	}

	public void load(Reader reader, boolean isTarget) throws IOException {
		StreamTokenizer tokenizer = new StreamTokenizer(reader);
		tokenizer.resetSyntax();
        tokenizer.commentChar('#');
        tokenizer.eolIsSignificant(false);
        tokenizer.whitespaceChars(' ', ' ');
        tokenizer.whitespaceChars('\r', '\r');
        tokenizer.whitespaceChars('\n', '\n');
        tokenizer.whitespaceChars('\t', '\t');
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('0', '9');
        tokenizer.nextToken();
        while (tokenizer.ttype != StreamTokenizer.TT_EOF) {
        	if (tokenizer.ttype != StreamTokenizer.TT_WORD)
        		throw new IOException("A predicate name is missing.");
        	Predicate predicate = Predicate.create(tokenizer.sval);
        	List<String> columnNames = new ArrayList<String>();
        	List<Domain> columnDomains = new ArrayList<Domain>();
        	tokenizer.nextToken();
        	if (tokenizer.ttype != '{')
        		throw new IOException("Predicate name should be followed by '{'.");
        	tokenizer.nextToken();
        	if (tokenizer.ttype != '}') {
        		if (tokenizer.ttype != StreamTokenizer.TT_WORD)
            		throw new IOException("Predicate column name expected.");
        		columnNames.add(tokenizer.sval);
        		tokenizer.nextToken();
        		if (tokenizer.ttype != ':')
            		throw new IOException("A predicate column name should be followed by ':'.");
        		tokenizer.nextToken();
        		if (tokenizer.ttype != StreamTokenizer.TT_WORD)
            		throw new IOException("Predicate column domain expected.");
        		try {
        			columnDomains.add(Domain.valueOf(tokenizer.sval));
        		}
        		catch (IllegalArgumentException e) {
        			throw new IOException("Unknown domain '" + tokenizer.sval + "'.");
        		}
        		tokenizer.nextToken();
	        	while (tokenizer.ttype == ',') {
	        		tokenizer.nextToken();
	        		if (tokenizer.ttype != StreamTokenizer.TT_WORD)
	            		throw new IOException("Predicate column name expected.");
	        		columnNames.add(tokenizer.sval);
	        		tokenizer.nextToken();
	        		if (tokenizer.ttype != ':')
	            		throw new IOException("A predicate column name should be followed by ':'.");
	        		tokenizer.nextToken();
	        		if (tokenizer.ttype != StreamTokenizer.TT_WORD)
	            		throw new IOException("Predicate column domain expected.");
	        		try {
	        			columnDomains.add(Domain.valueOf(tokenizer.sval));
	        		}
	        		catch (IllegalArgumentException e) {
	        			throw new IOException("Unknown domain '" + tokenizer.sval + "'.");
	        		}
	        		tokenizer.nextToken();
	        	}
	        	if (tokenizer.ttype != '}')
	        		throw new IOException("Predicate schema definition should be terminated by '}'.");
        	}
        	tokenizer.nextToken();
        	addPredicateSchema(predicate, isTarget, columnNames.toArray(new String[columnNames.size()]), columnDomains.toArray(new Domain[columnDomains.size()]));
        }
	}
	
	protected static class NodeKey {
		protected final Predicate m_predicate;
		protected final int m_position;

		public NodeKey(Predicate predicate, int position) {
			m_predicate = predicate;
			m_position = position;
		}
		
		public boolean equals(Object that) {
			if (this == that)
				return true;
			if (!(that instanceof NodeKey))
				return false;
			NodeKey thatNodeKey = (NodeKey)that;
			return m_predicate.equals(thatNodeKey.m_predicate) && m_position == thatNodeKey.m_position;
		}
		
		public int hashCode() {
			return m_predicate.hashCode() * 7 + m_position;
		}
		
	}
	
	protected static class Node {
		protected final NodeKey m_nodeKey;
		protected final PredicateInfo m_predicateInfo;
		protected final Map<NodeKey, Node> m_adjacentNodes;
		protected Domain m_domain;
		
		public Node(NodeKey nodeKey, PredicateInfo predicateInfo) {
			m_nodeKey = nodeKey;
			m_predicateInfo = predicateInfo;
			m_adjacentNodes = new LinkedHashMap<NodeKey, Node>();
			m_domain = null;
		}
		
		public void addAdjacentNode(Node node) {
			m_adjacentNodes.put(node.m_nodeKey, node);
		}
		
		public boolean setDomain(Domain domain) {
			if (m_domain == null) {
				m_domain = domain;
				return true;
			}
			else if (!m_domain.equals(domain))
				throw new IllegalArgumentException("Position " + m_nodeKey.m_position + " in predicate '" + m_nodeKey.m_predicate.getName() + "' is used with inconsistent types.");
			else
				return false;
		}
		
	}

	protected static class PredicateInfo {
		protected final Predicate m_predicate;
		protected final boolean m_isTarget;
		protected final Node[] m_arguments;
		protected boolean m_existsInSchema;
		
		public PredicateInfo(Predicate predicate, boolean isTarget, int arity) {
			m_predicate = predicate;
			m_isTarget = isTarget;
			m_arguments = new Node[arity];
			m_existsInSchema = false;
		}
		
		public int getArity() {
			return m_arguments.length;
		}
		
		public boolean isComplete() {
			for (Node node : m_arguments)
				if (node.m_domain == null)
					return false;
			return true;
		}
		
	}
	
	protected class PredicatePositionGraph {
		protected final Map<Predicate, PredicateInfo> m_predicateInfosByPredicate;
		protected final Map<NodeKey, Node> m_nodesByKey;
		
		public PredicatePositionGraph() {
			m_predicateInfosByPredicate = new LinkedHashMap<Predicate, PredicateInfo>();
			m_nodesByKey = new LinkedHashMap<NodeKey, Node>();
		}
		
		protected Node getNode(Predicate predicate, int position) {
			return m_nodesByKey.get(new NodeKey(predicate, position));
		}
		
		protected void ensureCreated(Predicate predicate, int arity, boolean isTarget) {
			PredicateInfo predicateInfo = m_predicateInfosByPredicate.get(predicate);
			if (predicateInfo == null) {
				predicateInfo = new PredicateInfo(predicate, isTarget, arity);
				m_predicateInfosByPredicate.put(predicate, predicateInfo);
				for (int position = 0; position < arity; ++position) {
					NodeKey nodeKey = new NodeKey(predicate, position);
					predicateInfo.m_arguments[position] = new Node(nodeKey, predicateInfo);
					m_nodesByKey.put(nodeKey, predicateInfo.m_arguments[position]);
				}
				PredicateSchema predicateSchema = getPredicateSchema(predicate);
				if (predicateSchema != null) {
					if (predicateSchema.getArity() != arity)
						throw new IllegalArgumentException("Predicate '" + predicate.getName() + "' is used with inconsistent arity.");
					predicateInfo.m_existsInSchema = true;
					for (int position = 0; position < arity; ++position)
						predicateInfo.m_arguments[position].setDomain(predicateSchema.getColumnDomain(position));
				}
			}
			else if (predicateInfo.getArity() != arity)
				throw new IllegalArgumentException("Predicate '" + predicate.getName() + "' is used with varying arity.");
		}

		protected void processAtom(Atom atom, Map<Variable, Node> variablesToNodes) {
			for (int argumentIndex = 0; argumentIndex < atom.getNumberOfArguments(); ++argumentIndex) {
				Term argument = atom.getArgument(argumentIndex);
				Node node = getNode(atom.getPredicate(), argumentIndex);
				if (node == null)
					System.out.println("Node!!!");
				if (argument instanceof Constant)
					node.setDomain(((Constant)argument).getDomain());
				else if (argument instanceof Variable) {
					Variable variable = (Variable)argument;
					Node existingNode = variablesToNodes.get(variable);
					if (existingNode == null)
						variablesToNodes.put(variable, node);
					else if (!node.equals(existingNode)) {
						node.addAdjacentNode(existingNode);
						existingNode.addAdjacentNode(node);
					}
				}
			}
		}
		
		public void addRule(Rule rule, boolean bodyIsTarget) {
			for (int bodyIndex = 0; bodyIndex < rule.getNumberOfBodyAtoms(); ++bodyIndex) {
				Atom bodyAtom = rule.getBodyAtom(bodyIndex);
				ensureCreated(bodyAtom.getPredicate(), bodyAtom.getNumberOfArguments(), bodyIsTarget);
			}
			for (int headIndex = 0; headIndex < rule.getNumberOfHeadAtoms(); ++headIndex) {
				Atom headAtom = rule.getHeadAtom(headIndex);
				ensureCreated(headAtom.getPredicate(), headAtom.getNumberOfArguments(), true);
			}
			Map<Variable, Node> variablesToNodes = new LinkedHashMap<Variable, Node>();
			for (int bodyIndex = 0; bodyIndex < rule.getNumberOfBodyAtoms(); ++bodyIndex)
				processAtom(rule.getBodyAtom(bodyIndex), variablesToNodes);
			for (int headIndex = 0; headIndex < rule.getNumberOfHeadAtoms(); ++headIndex)
				processAtom(rule.getHeadAtom(headIndex), variablesToNodes);
		}
		
		public void inferMissingPredicateSchemas(Collection<Predicate> unknownPredicates) {
			Queue<Node> queue = new LinkedList<Node>();
			for (Node node : m_nodesByKey.values()) {
				if (node.m_domain != null)
					queue.add(node);
			}
			Set<Node> processed = new HashSet<Node>();
			while (!queue.isEmpty()) {
				Node node = queue.remove();
				for (Node adjacentNode : node.m_adjacentNodes.values())
					if (adjacentNode.setDomain(node.m_domain) && processed.add(adjacentNode))
						queue.add(adjacentNode);
			}
			for (PredicateInfo predicateInfo : m_predicateInfosByPredicate.values()) {
				if (!predicateInfo.m_existsInSchema)
					if (predicateInfo.isComplete()) {
						String[] columnNames = new String[predicateInfo.getArity()];
						Domain[] columnDomains = new Domain[predicateInfo.getArity()];
						for (int index = 0; index < predicateInfo.getArity(); ++index) {
							columnNames[index] = "c" + index;
							columnDomains[index] = predicateInfo.m_arguments[index].m_domain;
						}
						addPredicateSchema(predicateInfo.m_predicate, predicateInfo.m_isTarget, columnNames, columnDomains);
						predicateInfo.m_existsInSchema = true;
					}
					else
						unknownPredicates.add(predicateInfo.m_predicate);
			}
		}
	}
}
