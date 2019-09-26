import uk.ac.ox.cs.chaseBench.format.CommonCSVToFileFormat;
import uk.ac.ox.cs.chaseBench.model.*;
import uk.ac.ox.cs.chaseBench.parser.CSVParser;
import uk.ac.ox.cs.chaseBench.processors.InputCollector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.sql.*;

public class SingleStep {

    private static Connection c;

    private static AtomicInteger existentialVariable = new AtomicInteger(0);

    private static int getExistentialVariable() {
        return existentialVariable.getAndIncrement();
    }

    private static boolean isInQuery(String schema, List<Atom> queryAtoms) {
        for (Atom atom : queryAtoms) {
            if (schema.contains(atom.getPredicate().getName()))
                return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Supported options:");
            System.out.println("-s-sch      <file>   | the file containing the source schema in CB format");
            System.out.println("-t-sch      <file>   | the file containing the target schema in CB format");
            System.out.println("-t-sql      <file>   | the file containing the target schema in SQL format");
            System.out.println("-q          <file>   | the file containing the query");
            System.out.println("-st-tgds    <file>   | the file containing the source-to-target TGDs");
            System.out.println("-data     <folder>   | the folder containing the csv files");
        } else {
            String sSchemLoc = null;
            String tSchemLoc = null;
            String tSchemSql = null;
            String sttgds = null;
            String queryLoc = null;
            String data = null;

            for (int i = 0; i < args.length - 1; i += 2) {
                String argument = args[i];
                switch (argument) {
                    case "-data":
                        data = args[i + 1];
                        break;
                    case "-s-sch":
                        sSchemLoc = args[i + 1];
                        break;
                    case "-t-sch":
                        tSchemLoc = args[i + 1];
                        break;
                    case "-st-tgds":
                        sttgds = args[i + 1];
                        break;
                    case "-t-sql":
                        tSchemSql = args[i + 1];
                        break;
                    case "-q":
                        queryLoc = args[i + 1];
                        break;
                    default:
                        System.out.println("Unknown option '" + argument + "'.");
                        break;
                }
            }
            if (sSchemLoc == null) {
                System.out.println("Invalid argument setup: No source CB schema found");
            } else if (tSchemLoc == null) {
                System.out.println("Invalid argument setup: No target CB schema found");
            } else if (tSchemSql == null) {
                System.out.println("Invalid argument setup: No target sql schema found");
            } else if (sttgds == null) {
                System.out.println("Invalid argument setup: No source-to-target TGDs found");
            } else if (queryLoc == null) {
                System.out.println("Invalid argument setup: No query found");
            } else if (data == null) {
                System.out.println("Invalid argument setup: No data folder found");
            } else {
                long originalStart = System.currentTimeMillis();
                long start = System.currentTimeMillis();

                //Loads the sql schema from file
                List<String> sqlSchema = Files.readAllLines(Paths.get(tSchemSql), StandardCharsets.UTF_8);
                //Read in the db schema in CB format
                DatabaseSchema dbSchema = new DatabaseSchema();
                dbSchema.load(new File(tSchemLoc), true);
                dbSchema.load(new File(sSchemLoc), false);
                //Read st-tgds from file
                List<Rule> rules = Mappings.getRules(sttgds);
                //And query
                Rule query = Mappings.getRules(queryLoc).get(0);
                //Create the final sql query
                String sqlQuery = "SELECT COUNT(*) FROM (" + SQLConverter.queryToSQL(query, false,true).replace(";", "") + ") AS query;";
                System.out.println(sqlQuery);
                List<Atom> queryAtoms = Arrays.asList(query.getBodyAtoms());
                //Filter out the schema only for atoms in the query
                sqlSchema = sqlSchema.stream().filter(s -> isInQuery(s, queryAtoms)).collect(Collectors.toList());
                //Create the database
                initDB(sqlSchema);
                ConcurrentLinkedQueue<Atom> atoms = new ConcurrentLinkedQueue<>();
                //Read in the rules
                List<Rule> ruleStream = ruleFilter(rules, queryAtoms);
                //createIndex(ruleStream).entrySet().forEach(System.out::println);
                readCSV(data, dbSchema, atoms, ruleStream);
                System.out.println("Load : " + (System.currentTimeMillis() - start) + "ms");
                ConcurrentHashMap<Atom, ConcurrentLinkedQueue<Term[]>> preparedStatementTerms = new ConcurrentHashMap<>();
                applySubstitution(atoms, ruleStream.parallelStream(), queryAtoms, preparedStatementTerms);
                //Map<Atom,List<Term[]>> inserts = preparedStatementMap(relevant.parallelStream().filter(queryAtoms::contains).collect(Collectors.toList()));

                //printToCSV(queryAtoms, subbed);
                //execQuery(preparedStatementMap.values(),sqlQuery);

                //Execute the terms and query
                runDB(preparedStatementTerms, sqlQuery);
                System.out.println("Total: " + (System.currentTimeMillis() - originalStart) + "ms");
            }

        }
    }

    //Returns map of a single atom and a list of term arrays to be put in a prepared statement but isnt used
    private static Map<Atom, List<Term[]>> preparedStatementTermMap(List<Atom> atoms) {
        Map<Atom, List<Term[]>> map = new HashMap<>();
        for (Atom atom : atoms) {
            List<Term[]> termsMap;
            if (map.containsKey(atom)) {
                termsMap = map.get(atom);
            } else {
                termsMap = new ArrayList<>();
                map.put(atom, termsMap);
            }
            termsMap.add(atom.getArguments());
        }
        return map;
    }


    //Applies terms to a prepared statement
    private static void applyTerms(Atom atom, PreparedStatement pst) throws SQLException {
        for (int i = 0; i < atom.getArguments().length; i++) {
            Term term = atom.getArgument(i);
            pst.setString(i + 1, term.toString());
        }
        pst.executeUpdate();
    }

    //Creates a map between atoms and their sql prepared Statements
    private static Map<Atom, PreparedStatement> preparedStatementMap(List<Atom> atoms) throws SQLException {
        Map<Atom, PreparedStatement> map = new HashMap<>();
        for (Atom atom : atoms) {
            map.put(atom, c.prepareStatement(createInsertStatement(atom)));
        }
        return map;

    }

    private static Map<Atom, List<Rule>> createIndex(List<Rule> rules) {
        HashMap<Atom, List<Rule>> index = new HashMap<>();
        for (Rule rule : rules) {
            for (Atom atom : rule.getHeadAtoms()) {
                List<Rule> rulesMap;
                if (index.containsKey(atom)) {
                    rulesMap = index.get(atom);
                } else {
                    rulesMap = new ArrayList<>();
                    index.put(atom, rulesMap);
                }
                rulesMap.add(rule);
            }
        }
        return index;
    }

    //Read in only the csvs relevant for the query
    private static void readCSV(String directory, DatabaseSchema dbSchema, ConcurrentLinkedQueue<Atom> atoms, List<Rule> ruleStream) throws IOException {
        InputCollector csvFormat = new InputCollector(dbSchema, ruleStream, atoms);
        for (Rule rule : ruleStream) {
            CSVParser.parse(new File(directory + "/" + rule.getBodyAtom(0).getPredicate().getName() + ".csv"), csvFormat);
        }

    }

    //Creates a temporary database. The user and password are hardcoded defaults. Change if you need to later
    private static void initDB(List<String> sqlSchema) {
        try {
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/",
                    "postgres", "password");
            Statement statement = c.createStatement();
            statement.execute("DROP DATABASE IF EXISTS \"temp\";");
            statement.execute("CREATE DATABASE \"temp\";");
            statement.close();
            c.close();
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/temp",
                    "postgres", "password");
            statement = c.createStatement();
            for (String table : sqlSchema) {
                statement.addBatch(table);
            }
            statement.executeBatch();
        } catch (BatchUpdateException b) {
            System.out.println(b.getNextException());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    //Executes a query on the db. not used in current build
    public static void execQuery(String sqlQuery) throws SQLException {
        long start = System.currentTimeMillis();
        Statement statement = c.createStatement();
        ResultSet rs = statement.executeQuery(sqlQuery);
        rs.next();
        System.out.println("Count : " + rs.getInt("count"));
        statement.close();
        System.out.println("Execute Query in DB: " + (System.currentTimeMillis() - start) + "ms");
    }

    //Inserts tuples into db then runs query. This takes quite a while. doing an in memory solution to compare equivalences could be faster
    private static void runDB(Map<Atom, ConcurrentLinkedQueue<Term[]>> insertAtoms, String sqlQuery) {
        long start = System.currentTimeMillis();
        try {
            Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/temp",
                    "postgres", "password");
            Statement statement = c.createStatement();
            //Autocommit slows down inserts. Normally doesnt matter
            c.setAutoCommit(false);
            for (Map.Entry<Atom, ConcurrentLinkedQueue<Term[]>> atoms : insertAtoms.entrySet()) {
                //Create prepared statement to increase the speed of inserts
                PreparedStatement pst = c.prepareStatement(createInsertStatement(atoms.getKey()));

                for (Term[] terms : atoms.getValue()) {
                    for (int i = 0; i < terms.length; i++) {
                        Term term = terms[i];
                        pst.setString(i + 1, term.toString());
                    }
                    pst.addBatch();
                }
                //Batch updates these prepared statements
                pst.executeBatch();
            }
            c.commit();
            //Execute query and find count
            ResultSet rs = statement.executeQuery(sqlQuery);
            rs.next();
            System.out.println("Count : " + rs.getInt("count"));
            statement.close();
            c.close();
            System.out.println("DB access: " + (System.currentTimeMillis() - start) + "ms");

        } catch (BatchUpdateException b) {
            System.out.println(b.getNextException());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    private static List<Rule> ruleFilter(List<Rule> rules, List<Atom> queryAtoms) {
        long start = System.currentTimeMillis();
        //remove st-tgds that dont icnlude anything from the query
        Stream<Rule> ruleStream = rules.parallelStream().filter(rule -> Arrays.stream(rule.getHeadAtoms()).anyMatch(queryAtoms::contains));
        List<Rule> newRules = new ArrayList<>();
        //Remove atoms from st-tgds that arent in query
        ruleStream.forEach(rule -> {
            List<Atom> head = new ArrayList<>();
            for (Atom atom : rule.getHeadAtoms()) {
                if (queryAtoms.contains(atom))
                    head.add(atom);

            }
            Atom[] headArr = new Atom[head.size()];
            head.toArray(headArr);
            newRules.add(Rule.create(headArr, rule.getBodyAtoms()));
        });
        System.out.println("Rule Filtering and Condensing : " + (System.currentTimeMillis() - start) + "ms");
        return newRules;
    }


    //Applies the substitions to the rules in st-tgds and only stored the terms in the atoms in the map
    //Everything is concurrent properties as I tried to parallelise it all as much as i can
    private static void applySubstitution(ConcurrentLinkedQueue<Atom> atoms, Stream<Rule> ruleStream, List<Atom> queryAtoms, ConcurrentHashMap<Atom, ConcurrentLinkedQueue<Term[]>> map) {
        long start = System.currentTimeMillis();
        for(Atom atom : queryAtoms) {
            map.put(atom, new ConcurrentLinkedQueue<>());
        }
        ruleStream.forEach(rule -> {
            atoms.parallelStream()
                    .filter(atom -> atom.getPredicate().equals(rule.getBodyAtom(0).getPredicate()))
                    .map(atom -> IntStream.range(0, atom.getNumberOfArguments()).boxed().collect(
                            Collectors.toMap(j -> (Variable) (rule.getBodyAtom(0).getArgument(j)), j -> atom.getArguments()[j])))
                    .forEach(substitution -> {
                        getExistential(rule, substitution, queryAtoms);
                        for (Atom atom : rule.applySubstitution(substitution).getHeadAtoms()) {
                            ConcurrentLinkedQueue<Term[]> termsMap;
                            termsMap = map.get(atom);
                            termsMap.add(atom.getArguments());
                        }
                    });
        });
        System.out.println("Applying substitutions : " + (System.currentTimeMillis() - start) + "ms");
    }

    //Existential addings to the map
    private static void getExistential(Rule subs, Map<Variable, Term> substitution, List<Atom> queryAtoms) {
        for (Atom atom : subs.getHeadAtoms()) {
            for (Term term : atom.getArguments()) {
                if (!substitution.containsKey(term)) {
                    substitution.put((Variable) term, LabeledNull.create("EXISTENTIALVARIABLE" + getExistentialVariable()));
                }
            }
        }
    }

    //Create the prepared statement for the atom
    private static String createInsertStatement(Atom atom) {
        StringBuilder builder = new StringBuilder();
        Term[] arguments = atom.getArguments();
        builder.append("INSERT INTO ");
        builder.append("\"").append(atom.getPredicate().getName()).append("\"");
        builder.append(" VALUES (");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append('?');
        }
        builder.append(")");
        return builder.toString();
    }

    private static void printToCSV(List<Atom> queryAtoms, List<Rule> subbed) throws IOException {
        long start = System.currentTimeMillis();
        List<Atom> finalRelevant = new ArrayList<>();
        List<Atom> relevant;

        subbed.forEach(rule -> Collections.addAll(finalRelevant, rule.getHeadAtoms()));
        relevant = finalRelevant.parallelStream().filter(queryAtoms::contains).collect(Collectors.toList());
        System.out.println("Refilter : " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        CommonCSVToFileFormat csvOut = new CommonCSVToFileFormat(new File("src/main/resources/intermediary"));
        for (Atom atom : relevant) {
            List<String> lex = new ArrayList<>();
            List<Boolean> labNull = new ArrayList<>();
            Arrays.stream(atom.getArguments()).forEach(term -> {
                lex.add(term.toString());
                if (term instanceof LabeledNull)
                    labNull.add(true);
                else
                    labNull.add(false);
            });
            csvOut.processFact(atom.getPredicate(), lex, labNull);
        }
        csvOut.endProcessing();
        System.out.println("Printing CSV : " + (System.currentTimeMillis() - start) + "ms");
    }


    private static String queryString(Rule rule) {
        StringBuilder builder = new StringBuilder();
        Atom[] head = rule.getHeadAtoms();
        for (int index = 0; index < head.length; index++) {
            if (index > 0)
                builder.append(", ");
            Atom heads = head[index];
            builder.append("Q");
            if (heads.getArguments().length > 0) {
                builder.append('(');
                for (int indexj = 0; indexj < heads.getArguments().length; indexj++) {
                    if (indexj > 0)
                        builder.append(", ");
                    heads.getArguments()[indexj].toString(builder);
                }
                builder.append(')');
            }
        }
        builder.append(" <- ");
        Atom[] body = rule.getBodyAtoms();
        for (int index = 0; index < body.length; index++) {
            if (index > 0)
                builder.append(", ");
            Atom bodys = body[index];
            bodys.getPredicate().toString(builder);
            if (bodys.getArguments().length > 0) {
                builder.append('(');
                for (int indexj = 0; indexj < bodys.getArguments().length; indexj++) {
                    if (indexj > 0)
                        builder.append(", ");
                    bodys.getArguments()[indexj].toString(builder);
                }
                builder.append(')');
            }
        }
        return builder.toString();
    }


}
