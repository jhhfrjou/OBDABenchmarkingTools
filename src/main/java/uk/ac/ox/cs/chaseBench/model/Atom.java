package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public class Atom implements Serializable {
    private static final long serialVersionUID = -6432381113892460896L;

    protected final Predicate m_predicate;
    protected Term[] m_arguments;

    public Atom(Predicate predicate, Term[] arguments) {
        m_predicate = predicate;
        m_arguments = arguments.clone();
    }

    public Predicate getPredicate() {
        return m_predicate;
    }

    public int getNumberOfArguments() {
        return m_arguments.length;
    }

    public Term getArgument(int index) {
        return m_arguments[index];
    }

    public Term[] getArguments() {
        return m_arguments.clone();
    }

    public void setArguments(Term[] arguments) { m_arguments = arguments;}

    public void getArguments(Term[] arguments) {
        System.arraycopy(m_arguments, 0, arguments, 0, m_arguments.length);
    }

    public boolean isGround() {
        for (int index = m_arguments.length - 1;index >= 0;--index)
            if (!m_arguments[index].isGround())
                return false;
        return true;
    }

    public Atom replacePredicate(Predicate newPredicate) {
        return create(newPredicate, m_arguments);
    }

    public Atom applySubstitution(Map<Variable, ? extends Term> substitution) {
        if (substitution.isEmpty())
            return this;
        else {
            Term[] arguments = new Term[m_arguments.length];
            for (int index = 0;index < m_arguments.length;index++)
                arguments[index] = m_arguments[index].applySubstitution(substitution);
            return Atom.create(m_predicate, arguments);
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    public void toString(StringBuilder builder) {
    	if (Predicate.EQUALS.equals(m_predicate) && m_arguments.length == 2) {
    		m_arguments[0].toString(builder);
    		builder.append(" = ");
    		m_arguments[1].toString(builder);
    	}
        else {
            m_predicate.toString(builder);
            if (m_arguments.length > 0) {
                builder.append('(');
                for (int index = 0;index < m_arguments.length;index++) {
                    if (index > 0)
                        builder.append(',');
                    m_arguments[index].toString(builder);
                }
                builder.append(')');
            }
        }
    }

    public void print(PrintWriter output) {
    	if (Predicate.EQUALS.equals(m_predicate) && m_arguments.length == 2) {
    		m_arguments[0].print(output);
    		output.print(" = ");
    		m_arguments[1].print(output);
    	}
        else {
            m_predicate.print(output);
            if (m_arguments.length > 0) {
                output.print('(');
                for (int index = 0;index < m_arguments.length;index++) {
                    if (index > 0)
                        output.print(',');
                    m_arguments[index].print(output);
                }
                output.print(')');
            }
        }
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static final InterningManager<Atom> s_interningManager = new InterningManager<Atom>() {
        protected boolean equal(Atom object1, Atom object2) {
            if (object1.m_predicate != object2.m_predicate || object1.m_arguments.length != object2.m_arguments.length)
                return false;
            for (int index = object1.m_arguments.length - 1;index >= 0;--index)
                if (object1.m_arguments[index] != object2.m_arguments[index])
                    return false;
            return true;
        }
        protected int getHashCode(Atom object) {
            int hashCode = object.m_predicate.hashCode();
            for (int index = object.m_arguments.length - 1;index >= 0;--index)
                hashCode = hashCode * 7 + object.m_arguments[index].hashCode();
            return hashCode;
        }
    };

    public static Atom create(Predicate predicate, Term... arguments) {
        return s_interningManager.intern(new Atom(predicate, arguments));
    }

    public boolean equals(Object o){
        if( o instanceof Atom) {
            return ((Atom) o).getPredicate().getName().equals(this.getPredicate().getName());
        } else
            return false;
    }


    public int hashCode() {
        return m_predicate.getName().hashCode();
    }
}
