package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Rule implements Serializable {
    private static final long serialVersionUID = 7041166935579972749L;

    protected final Atom[] m_head;
    protected final Atom[] m_body;

    public Rule(Atom[] head, Atom[] body) {
        m_head = head;
        m_body = body.clone();
    }

    public int getNumberOfHeadAtoms() {
        return m_head.length;
    }

    public Atom getHeadAtom(int index) {
        return m_head[index];
    }

    public Atom[] getHeadAtoms() {
        return m_head.clone();
    }

    public int getNumberOfBodyAtoms() {
        return m_body.length;
    }

    public Atom getBodyAtom(int index) {
        return m_body[index];
    }

    public Atom[] getBodyAtoms() {
        return m_body.clone();
    }

    public Rule replaceBody(Atom[] newBody) {
        return create(m_head,newBody);
    }

    public Rule replaceHead(Atom newHeadAtom) {
        return create(newHeadAtom, m_body);
    }

    public Rule applySubstitution(Map<Variable, Term> substitution) {
        Atom[] head = new Atom[m_head.length];
        for (int index = 0;index < m_head.length;index++)
            head[index] = m_head[index].applySubstitution(substitution);
        Atom[] body = new Atom[m_body.length];
        for (int index = 0;index < m_body.length;index++)
            body[index] = m_body[index].applySubstitution(substitution);
        return Rule.create(head, body);
    }

    public boolean isSafe() {
        Set<Variable> bodyVariables = new LinkedHashSet<Variable>();
        for (Atom atom : m_body) {
            for (int index = atom.getNumberOfArguments() - 1;index >= 0;--index) {
                Term argument = atom.getArgument(index);
                if (argument instanceof Variable)
                    bodyVariables.add((Variable)argument);
            }
        }
        for (Atom head : m_head) {
            for (int index = head.getNumberOfArguments() - 1;index >= 0;--index) {
                Term argument = head.getArgument(index);
                if (argument instanceof Variable && !bodyVariables.contains(argument))
                    return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    public void toString(StringBuilder builder) {
        for (int index = 0; index < m_body.length; index++) {
            if (index > 0)
                builder.append(", ");
            m_body[index].toString(builder);
        }
        builder.append(" -> ");
        for (int index = 0; index < m_head.length; index++) {
            if (index > 0)
                builder.append(", ");
            m_head[index].toString(builder);
        }
        builder.append(" .");
    }

    public void print(PrintWriter output) {
        for (int index = 0; index < m_body.length; index++) {
            if (index > 0)
                output.print(", ");
            m_body[index].print(output);
        }
        output.print(" -> ");
        for (int index = 0; index < m_head.length; index++) {
            if (index > 0)
                output.print(", ");
            m_head[index].print(output);
        }
        output.print(" .");
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static final InterningManager<Rule> s_interningManager = new InterningManager<Rule>() {
        protected boolean equal(Rule object1, Rule object2) {
            if (object1.m_head != object2.m_head || object1.m_body.length != object2.m_body.length)
                return false;
            for (int index = object1.m_body.length - 1;index >= 0;--index)
                if (object1.m_body[index] != object2.m_body[index])
                    return false;
            return true;
        }
        protected int getHashCode(Rule object) {
            int hashCode = object.m_head.hashCode();
            for (int index = object.m_body.length - 1;index >= 0;--index)
                hashCode = hashCode * 7 + object.m_body[index].hashCode();
            return hashCode;
        }
    };

    public static Rule create(Atom head, Atom... body) {
        return s_interningManager.intern(new Rule(new Atom[] { head }, body));
    }

    public static Rule create(Atom[] head, Atom[] body) {
        return s_interningManager.intern(new Rule(head, body));
    }
}
