import uk.ac.ox.cs.chaseBench.model.*;
import uk.ac.ox.cs.chaseBench.parser.CommonParser;
import uk.ac.ox.cs.chaseBench.processors.InputCollector;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataLogToUCQ {

    private static int existentialVariable = 0;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Supported options:");
            System.out.println("-exts       <string>  | the string contains the EXT rewritings");
            System.out.println("-query      <string>  | the string containing the query");
        } else {
            String subString = null;
            String queryString = null;

            for (int i = 0; i < args.length - 1; i += 2) {
                String argument = args[i];
                switch (argument) {
                    case "-exts":
                        subString = args[i + 1];
                        break;
                    case "-query":
                        queryString = args[i + 1];
                        break;
                    default:
                        System.out.println("Unknown option '" + argument + "'.");
                        break;

                }

            }
            if (subString == null) {
                System.out.println("Invalid argument setup: No EXT rewritings found");
            } else if (queryString == null) {
                System.out.println("Invalid argument setup: No query found");
            } else {
                subString = convertToCb(subString);
                queryString = convertToCb(queryString);
                Rule query = getRuleString(queryString).get(0);
                List<Rule> subs = getRuleString(subString);
                Map<Predicate, List<Rule>> mappings = getMappings(subs);
                List<Rule> results = new ArrayList<>();
                updateQuery(query, mappings, results);
                results.forEach(q -> System.out.println(ModifyChaseBench.queryString(q)));
            }
        }


    }

    //Converts a datalog query string to a chasebench string ready to be parsed
    private static String convertToCb(String subString) {
        //Turns the :- to arrows
        subString = subString.replace(":-", "<-");
        //Need to get the string inside brackets so are variables
        String[] splitOnBrackets = subString.split("[()]");
        //This wont work if the inputs brackets are (( )) or some other weird format
        StringBuilder subStringBuilder = new StringBuilder(splitOnBrackets[0]);
        //Reconstructs the string after splitting only iterates on things inside brackets
        for (int i = 1; i < splitOnBrackets.length; i += 2) {
            subStringBuilder.append("(");
            String[] variablesArray = splitOnBrackets[i].split("[,]");
            for (int j = 0; j < variablesArray.length; j++) {
                //The ? make them variables
                subStringBuilder.append("?").append(variablesArray[j].trim());
                if (j < variablesArray.length - 1)
                    subStringBuilder.append(", ");
            }

            subStringBuilder.append(")");
            //Adds the part after that the brackets back
            if (i != splitOnBrackets.length - 1)
                subStringBuilder.append(" ").append(splitOnBrackets[i + 1]);
        }
        subString = subStringBuilder.toString();
        return subString;
    }

    //Will substitute the atoms in the query for those in the map
    private static void updateQuery(Rule query, Map<Predicate, List<Rule>> mappings, List<Rule> results) {
        Atom[] bodyAtoms = query.getBodyAtoms();
        boolean nochange = true;
        //Recursively calls fun
        for (int i = 0; i < bodyAtoms.length; i++) {
            Atom queryElement = bodyAtoms[i];
            if (mappings.containsKey(queryElement.getPredicate())) {
                for (Rule subs : mappings.get(queryElement.getPredicate())) {
                    Rule updatedRule = applyMappings(query, subs, i);
                    //Will keep being called until there is substation made so is in the final valid query
                    updateQuery(updatedRule, mappings, results);

                }
                nochange = false;
                break;
            }
        }
        if (nochange)
            results.add(query);
    }


    private static Rule applyMappings(Rule query, Rule subs, int i) {
        Atom[] body = query.getBodyAtoms();
        try {
            //this madness iterates through the 2 lists and creates the mappings between the substituted variables and the query
            Map<Variable, Term> substitution = IntStream.range(0, body[i].getNumberOfArguments()).boxed()
                    .collect(Collectors.toMap(j -> (Variable) (subs.getHeadAtom(0).getArgument(j)), j -> body[i].getArguments()[j]));
            //It will fail if a single key is mapped to multiple values, So equivalence substitutions need to be done

            getExistential(subs, substitution);
            Atom[] subbed = subs.applySubstitution(substitution).getBodyAtoms();
            List<Atom> newBody = new ArrayList<>(Arrays.asList(body));
            //Replaces that atom with the body of the substituted rules
            newBody.remove(i);
            newBody.addAll(Arrays.asList(subbed));
            Atom[] newBodyArray = new Atom[body.length + subbed.length - 1];
            newBody.toArray(newBodyArray);
            //Returns a new rule
            return query.replaceBody(newBodyArray);
            //The map creator throws an exception if the same key is being used to add different values.
            //This means some equivalence exists so the query variables needs to be updated
        } catch (Exception e) {
            Map<Variable, Term> substitution = new HashMap<>();
            Map<Variable, Term> duplicate = new HashMap<>();
            for (int j = 0; j < body[i].getNumberOfArguments(); j++) {
                if (!substitution.containsKey(subs.getHeadAtom(0).getArgument(j))) {
                    substitution.put((Variable) subs.getHeadAtom(0).getArgument(j), body[i].getArgument(j));
                } else if (body[i].getArgument(j) != substitution.get(subs.getHeadAtom(0).getArgument(j))) {
                    //Adds to a map to perform a substitution to the starting query
                    duplicate.put((Variable) body[i].getArgument(j), substitution.get(subs.getHeadAtom(0).getArgument(j)));
                }
            }
            getExistential(subs, substitution);
            Atom[] subbed = subs.applySubstitution(substitution).getBodyAtoms();
            //Apply a substitution to the original query and reapply the first subsitution to duplicate variable names in places
            query = query.applySubstitution(duplicate).applySubstitution(substitution);
            List<Atom> newBody = new ArrayList<>(Arrays.asList(query.getBodyAtoms()));
            newBody.remove(i);
            newBody.addAll(Arrays.asList(subbed));
            Atom[] newBodyArray = new Atom[query.getNumberOfBodyAtoms() + subbed.length - 1];
            newBody.toArray(newBodyArray);
            return query.replaceBody(newBodyArray);
        }

    }

    //Finds variables not already mapped and adds an entry to the map to set them as existential variables
    static void getExistential(Rule subs, Map<Variable, Term> substitution) {
        for (Atom atom : subs.getBodyAtoms()) {
            for (Term term : atom.getArguments()) {
                if (!substitution.containsKey(term)) {
                    substitution.put((Variable) term, Variable.create("EXISTENTIALVARIABLE" + existentialVariable++));
                }
            }
        }
    }

    //Reads in the rules/tgd as an input string
    public static List<Rule> getRuleString(String queryString) throws IOException {
        //Very much a duplicate of Mappings.getRules but not using a file
        List<Rule> query = new ArrayList<>();
        Reader input = new StringReader(queryString);
        StringBuilder output = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = input.read(buffer)) != -1)
            output.append(buffer, 0, read);
        input.close();
        InputCollector inputCollector = new InputCollector(null, query, null);
        CommonParser parser = new CommonParser(new StringReader(output.toString()));
        parser.parse(inputCollector);
        return query;
    }

    //loops through the substitions making a map between the heads and the body
    private static Map<Predicate, List<Rule>> getMappings(List<Rule> subs) {
        HashMap<Predicate, List<Rule>> duplicates = new HashMap<>();
        for (Rule rule : subs) {
            List<Rule> rules;
            if (duplicates.containsKey(rule.getHeadAtom(0).getPredicate())) {
                rules = duplicates.get(rule.getHeadAtom(0).getPredicate());
            } else {
                rules = new ArrayList<>();
                duplicates.put(rule.getHeadAtom(0).getPredicate(), rules);
            }
            rules.add(rule);
        }
        return duplicates;
    }

}


