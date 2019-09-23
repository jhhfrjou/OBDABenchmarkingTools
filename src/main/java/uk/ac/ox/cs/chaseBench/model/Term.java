package uk.ac.ox.cs.chaseBench.model;

import java.io.PrintWriter;
import java.util.Map;

public abstract class Term {
    public abstract boolean isGround();

    public abstract Term applySubstitution(Map<Variable, ? extends Term> substitution);

    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
    }

    public abstract void toString(StringBuilder builder);

    public abstract void print(PrintWriter output);
}
