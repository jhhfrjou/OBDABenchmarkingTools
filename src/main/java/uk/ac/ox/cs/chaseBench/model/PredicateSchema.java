package uk.ac.ox.cs.chaseBench.model;

public class PredicateSchema {
	protected final Predicate m_predicate;
	protected final boolean m_isTarget;
	protected final String[] m_columnNames;
	protected final Domain[] m_columnDomains;
	
	protected PredicateSchema(Predicate predicate, boolean isTarget, String[] columnNames, Domain[] columnDomains) {
		m_predicate = predicate;
		m_isTarget = isTarget;
		m_columnNames = columnNames;
		m_columnDomains = columnDomains;
	}
	
	public Predicate getPredicate() {
		return m_predicate;
	}
	
	public boolean isTarget() {
		return m_isTarget;
	}
	
	public int getArity() {
		return m_columnNames.length;
	}
	
	public String getColumnName(int columnIndex) {
		return m_columnNames[columnIndex];
	}

	public Domain getColumnDomain(int columnIndex) {
		return m_columnDomains[columnIndex];
	}
}