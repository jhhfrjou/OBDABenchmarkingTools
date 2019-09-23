package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;

public class Constant extends Term implements Serializable {
    private static final long serialVersionUID = 3347513364700246887L;

    protected final String m_lexicalForm;
    protected final Domain m_domain;

    protected Constant(String lexicalForm, Domain domain) {
        m_lexicalForm = lexicalForm;
        m_domain = domain;
    }

    public String getLexicalForm() {
        return m_lexicalForm;
    }

    public Domain getDomain() {
        return m_domain;
    }

    public boolean isGround() {
    	return true;
    }
    
    public Term applySubstitution(Map<Variable, ? extends Term> substitution) {
        return this;
    }

    public void toString(StringBuilder builder) {
    	toString(builder, m_lexicalForm, m_domain);
    }
    
    public void print(PrintWriter output) {
    	print(output, m_lexicalForm, m_domain);
    }

    public static void toString(StringBuilder builder, String lexicalForm, Domain type) {
    	switch (type) {
    	case SYMBOL:
    		builder.append(lexicalForm);
    		break;
    	case STRING:
            builder.append('"');
            for (int index = 0; index < lexicalForm.length(); ++index) {
                char c = lexicalForm.charAt(index);
                switch (c) {
                case '"':
                	builder.append("\\\"");
                	break;
                case '\\':
                	builder.append("\\\\");
                	break;
                case '\n':
                	builder.append("\\n");
                	break;
                case '\r':
                	builder.append("\\r");
                	break;
                case '\t':
                	builder.append("\\t");
                	break;
                case '\b':
                	builder.append("\\b");
                	break;
                case '\f':
                	builder.append("\\b");
                	break;
                default:
                    builder.append(c);
                    break;
                }
            }
            builder.append('"');
            break;
    	case INTEGER:
    	case DOUBLE:
    		builder.append(lexicalForm);
    		break;
    	}
    }

    public static void print(PrintWriter output, String lexicalForm, Domain type) {
    	switch (type) {
    	case SYMBOL:
    		output.print(lexicalForm);
    		break;
    	case STRING:
            output.print('"');
            for (int index = 0; index < lexicalForm.length(); ++index) {
                char c = lexicalForm.charAt(index);
                switch (c) {
                case '"':
                	output.print("\\\"");
                	break;
                case '\\':
                	output.print("\\\\");
                	break;
                case '\n':
                	output.print("\\n");
                	break;
                case '\r':
                	output.print("\\r");
                	break;
                case '\t':
                	output.print("\\t");
                	break;
                case '\b':
                	output.print("\\b");
                	break;
                case '\f':
                	output.print("\\b");
                	break;
                default:
                	output.print(c);
                    break;
                }
            }
            output.print('"');
            break;
    	case INTEGER:
    	case DOUBLE:
    		output.print(lexicalForm);
    		break;
    	}
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static final InterningManager<Constant> s_interningManager = new InterningManager<Constant>() {
        protected boolean equal(Constant object1, Constant object2) {
            return object1.m_lexicalForm.equals(object2.m_lexicalForm) && object1.m_domain.equals(object2.m_domain);
        }
        protected int getHashCode(Constant object) {
            return object.m_lexicalForm.hashCode() * 7 + object.m_domain.hashCode();
        }
    };

    public static Constant create(String lexicalForm, Domain domain) {
        return s_interningManager.intern(new Constant(lexicalForm, domain));
    }
}
