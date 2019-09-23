package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;

public class Variable extends Term implements Serializable {
    private static final long serialVersionUID = 519850891163499708L;

    protected final String m_name;

    protected Variable(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public boolean isGround() {
        return false;
    }

    public Term applySubstitution(Map<Variable, ? extends Term> substitution) {
        Term substitutedTerm = substitution.get(this);
        if (substitutedTerm == null)
            return this;
        else
            return substitutedTerm;
    }

    public void toString(StringBuilder builder) {
        builder.append('?');
        builder.append(m_name);
    }

    public void print(PrintWriter output) {
        output.print('?');
        output.print(m_name);
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static final InterningManager<Variable> s_interningManager = new InterningManager<Variable>() {
        protected boolean equal(Variable object1, Variable object2) {
            return object1.m_name.equals(object2.m_name);
        }
        protected int getHashCode(Variable object) {
            return object.m_name.hashCode();
        }
    };

    public static Variable create(String name) {
        return s_interningManager.intern(new Variable(name));
    }
}
