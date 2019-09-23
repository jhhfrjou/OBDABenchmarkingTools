package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Comparator;

public class Predicate implements Serializable {
    private static final long serialVersionUID = -4106426797562502177L;

    protected final String m_name;

    public Predicate(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    public void toString(StringBuilder builder) {
        builder.append(m_name);
    }

    public void print(PrintWriter output) {
        output.print(m_name);
    }

    @Override
    public int hashCode() {
        return m_name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Predicate) {
            return ((Predicate) obj).getName().equals(this.getName());
        } else
            return false;
    }

    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static final InterningManager<Predicate> s_interningManager = new InterningManager<Predicate>() {
        protected boolean equal(Predicate object1, Predicate object2) {
            return object1.m_name.equals(object2.m_name);
        }
        protected int getHashCode(Predicate object) {
            return object.m_name.hashCode();
        }
    };

    public static Predicate create(String name) {
        return s_interningManager.intern(new Predicate(name));
    }

    public static final Predicate EQUALS = create("=");
    public static final Predicate SKOLEM = create("SKOLEM");
    
    public static final Comparator<Predicate> COMPARATOR = new Comparator<Predicate>() {
		public int compare(Predicate o1, Predicate o2) {
			return o1.m_name.compareTo(o2.m_name);
		}
    };

}
