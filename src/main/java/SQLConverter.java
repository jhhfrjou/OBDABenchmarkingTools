import java.util.*;

import uk.ac.ox.cs.chaseBench.model.Atom;
import uk.ac.ox.cs.chaseBench.model.Constant;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.model.Variable;


public class SQLConverter {

    public static String manyQueriestoSQL(List<Rule> queries, boolean src) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < queries.size(); i++) {
            Rule q = queries.get(i);
            builder.append(queryToSQL(q, src).replace(";", ""));
            if (i != queries.size() - 1)
                builder.append(" UNION ");
        }
        builder.append(';');
        return builder.toString();

    }

    public static void main(String[] args) throws Exception {
        System.out.println(manyQueriestoSQL(DataLogToUCQ.getRuleString(args[0]),args.length > 1));
    }

    public static String addAliases(String originalQuery) {
        String[] outputVars = originalQuery.split("DISTINCT")[1].split("FROM")[0].split(",");
        StringBuilder output = new StringBuilder("SELECT ");
        for (int i = 0; i < outputVars.length; i++) {
            String var = outputVars[i];
            output.append(var.trim()).append(" as var").append(i);
            if (i < outputVars.length - 1) {
                output.append(", ");
            }
        }
        output.append(" FROM ").append(originalQuery.split("FROM")[1]);
        return output.toString();
    }

    public static String queryToSQL(Rule query, boolean src) {
        char aliasLetter= 'A';
        List<Variable> distVars = new ArrayList<>();
        Map<Variable, ArrayList<String>> columnMappings = new HashMap<Variable, ArrayList<String>>();
        Map<Constant, ArrayList<String>> columnConstraints = new HashMap<Constant, ArrayList<String>>();
        List<String> predicates = new ArrayList<>();
        Arrays.stream(query.getHeadAtoms()).forEach(atom -> Arrays.stream(atom.getArguments()).filter(term -> term instanceof Variable).forEach(term -> distVars.add((Variable) term)));

        for (Atom a : query.getBodyAtoms()) {
            predicates.add(a.getPredicate().getName());
            for (int i = 0; i < a.getNumberOfArguments(); i++) {
                if (a.getArgument(i) instanceof Variable) {
                    Variable var = (Variable) a.getArgument(i);
                    if (columnMappings.containsKey(var)) {
                        ArrayList<String> columns = columnMappings.get(var);
                        columns.add(aliasLetter +".\"c" + i + "\"");
                        columnMappings.put(var, columns);
                    } else {
                        ArrayList<String> columns = new ArrayList<String>();
                        columns.add(aliasLetter +".\"c" + i + "\"");
                        columnMappings.put(var, columns);
                    }
                } else if (a.getArgument(i) instanceof Constant) {
                    Constant con = (Constant) a.getArgument(i);
                    if (columnConstraints.containsKey(con)) {
                        ArrayList<String> columns = columnConstraints.get(con);
                        columns.add(aliasLetter +".\"c" + i + "\"");
                        columnConstraints.put(con, columns);
                    } else {
                        ArrayList<String> columns = new ArrayList<String>();
                        columns.add(aliasLetter+".\"c" + i + "\"");
                        columnConstraints.put(con, columns);
                    }
                }
            }
            aliasLetter++;
        }
        aliasLetter='A';
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT DISTINCT ");
        String prefix = "";
        for (Variable v : distVars) {
            if (columnMappings.containsKey(v)) {
                builder.append(prefix);
                builder.append(columnMappings.get(v).get(0));
                prefix = ", ";
            }
        }
        builder.append(" FROM ");
        prefix = "";
        for (String s : predicates) {
            builder.append(prefix);
            builder.append("\"");
            if (src)
                builder.append("src_");
            builder.append(s).append("\"");
            builder.append(" as ").append(aliasLetter++);
            prefix = ", ";
        }
        if (equalities(columnMappings, columnConstraints))
            builder.append(" WHERE ");

        prefix = "";
        for (Variable v : columnMappings.keySet()) {
            ArrayList<String> columns = columnMappings.get(v);
            if (columns.size() > 1) {
                for (int i = 1; i < columns.size(); i++) {
                    builder.append(prefix);
                    builder.append(columns.get(i - 1)).append(" = ").append(columns.get(i));
                    prefix = " AND ";
                }
            }
        }

        for (Constant c : columnConstraints.keySet()) {
            ArrayList<String> columns = columnConstraints.get(c);
            for (String column : columns) {
                builder.append(prefix);
                builder.append(column).append(" = ").append(c.toString().replace("?", ""));
            }
        }

        builder.append(";");
        return builder.toString().replace("?", "");

    }

    private static boolean equalities(Map<Variable, ArrayList<String>> columnMappings, Map<Constant, ArrayList<String>> columnConstraints) {
        for (Map.Entry<Variable, ArrayList<String>> entry : columnMappings.entrySet()) {
            if (entry.getValue().size() > 1) {
                return true;
            }
        }
        for (Map.Entry<Constant, ArrayList<String>> entry : columnConstraints.entrySet()) {
            if (entry.getValue().size() > 1) {
                return true;
            }
        }
        return false;

    }


}
