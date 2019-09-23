package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;

public class LabeledNull extends Term implements Serializable {
	private static final long serialVersionUID = -7448229761680157573L;

	protected final String m_name;

    protected LabeledNull(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public boolean isGround() {
    	return true;
    }
    
    public Term applySubstitution(Map<Variable, ? extends Term> substitution) {
        return this;
    }

    public void toString(StringBuilder builder) {
    	builder.append("_:");
    	builder.append(m_name);
    }
    
    public void print(PrintWriter output) {
    	output.print("_:");
    	output.print(m_name);
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static final InterningManager<LabeledNull> s_interningManager = new InterningManager<LabeledNull>() {
        protected boolean equal(LabeledNull object1, LabeledNull object2) {
            return object1.m_name.equals(object2.m_name);
        }
        protected int getHashCode(LabeledNull object) {
            return object.m_name.hashCode();
        }
    };

    public static LabeledNull create(String name) {
        return s_interningManager.intern(new LabeledNull(name));
    }
}
