import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.ox.cs.chaseBench.model.Atom;
import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.model.Term;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Owls {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Supported options:");
            System.out.println("-t-tgds    <file>  | the file containing the target-to-target TGDs");
            System.out.println("-out       <file>  | the file to print out the owl ontology");
            System.out.println("-stSrc     <file>  | print a file containing 1-1 st-tgds");
        } else {
            String ttgds = null;
            String out = null;
            String stSrc = null;

            for (int i = 0; i < args.length - 1; i += 2) {
                String argument = args[i];
                switch (argument) {
                    case "-t-tgds":
                        ttgds = args[i + 1];
                        break;
                    case "-out":
                        out = args[i + 1];
                        break;
                    case "-stSrc":
                        stSrc = args[i + 1];
                        break;
                    default:
                        System.out.println("Unknown option '" + argument + "'.");
                        break;

                }
            }
            if (out == null) {
                System.out.println("Invalid argument setup: No output location found");
            } else if (ttgds == null) {
                System.out.println("Invalid arguments : No target-to-target TGDs found");
            } else {
                //Gets t-tgds from the file
                List<Rule> tRules = Mappings.getRules(ttgds);
                //Creates and output folder ready to be written to
                File output = new File(out);
                if (!output.exists()) {
                    output.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(output);
                PrintStream printer = new PrintStream(fileOutputStream);
                createHeader(printer);
                Map<Atom, AtomWrapper> atomsMap = new HashMap<>();
                //Adds all atoms from the tgds file
                tRules.forEach(rule -> collectAtoms(atomsMap, rule));

                if (stSrc != null) {
                    FileOutputStream srcOutput = new FileOutputStream(stSrc);
                    PrintStream srcPrinter = new PrintStream(srcOutput);
                    atomsMap.keySet().forEach(atom -> printGenSrc(atom, srcPrinter));
                    srcPrinter.close();
                    srcOutput.close();
                }

                atomsMap.values().forEach(atomWrapper -> printOWLs(atomWrapper, printer));
                createFooter(printer);
                printer.close();
                fileOutputStream.close();
            }
        }
    }

    static void getAtoms(Rule rule, Set<Atom> atoms) {
        atoms.addAll(Arrays.asList(rule.getBodyAtoms()));
        atoms.addAll(Arrays.asList(rule.getHeadAtoms()));
    }

    private static void printGenSrc(Atom head, PrintStream printer) {
        Predicate predicate = new Predicate("src_" + head.getPredicate().getName());
        Atom body = new Atom(predicate, head.getArguments());
        Rule rule = new Rule(new Atom[]{head}, new Atom[]{body});
        printer.println(rule);
    }

    private static void createHeader(PrintStream printer) {
        printer.println("<?xml version=\"1.0\"?>\n" +
                "<rdf:RDF xmlns=\"http://example.com/example.owl#\" xml:base=\"http://example.com/example.owl\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">  \n" +
                "\n" +
                "<owl:Ontology rdf:about=\"http://example.com/example.owl\"/>\n");
    }

    private static void createFooter(PrintStream printer) {
        printer.println("</rdf:RDF>\n");
    }

    //Collect the elements of a rule
    static void collectAtoms(Map<Atom, AtomWrapper> atoms, Rule rule) {

        Atom body = rule.getBodyAtom(0);
        Atom[] heads = rule.getHeadAtoms();
        AtomWrapper wrapper;
        if (atoms.containsKey(body))
            wrapper = atoms.get(body);
        else {
            wrapper = new AtomWrapper();
            wrapper.atom = body;
            atoms.put(body, wrapper);
        }
        for (Atom head : heads) {
            if (!atoms.containsKey(head)) {
                wrapper = new AtomWrapper();
                wrapper.atom = head;
                atoms.put(head, wrapper);
            }
        }

        Term bodyDomVar = body.getArgument(0);
        Term bodyRanVar = null;
        if (body.getNumberOfArguments() == 2) {
            bodyRanVar = body.getArgument(1);
        }
        for (Atom atom : heads) {
            if (atom.getNumberOfArguments() == 2) {
                if (subClassOf(atom, body)) {
                    atoms.get(body).subProperty = atom.getPredicate().getName();
                }
                if (inverseOf(atom, body)) {
                    atoms.get(body).inverseOf = atom.getPredicate().getName();
                    wrapper.inverseOf = body.getPredicate().getName();
                }
                if (atom.getArgument(0).toString().equals(bodyDomVar.toString())) {
                    wrapper.domOnProp = atom.getPredicate().getName();
                } else if (atom.getArgument(1).toString().equals(bodyDomVar.toString())) {
                    wrapper.domOnProp = "io_" + atom.getPredicate().getName();
                }
                if (bodyRanVar != null) {
                    if (atom.getArgument(0).toString().equals(bodyRanVar.toString())) {
                        wrapper.ranOnProp = atom.getPredicate().getName();
                        String ran = atom.getArgument(1).toString();
                        for (Atom atom2 : heads) {
                            if (atom2.getNumberOfArguments() == 1 && atom2.getArgument(0).toString().equals(ran)) {
                                wrapper.ranSomValues = atom2.getPredicate().getName();
                            }
                        }
                    }
                }


            } else {
                if (atom.getArgument(0).toString().equals(bodyDomVar.toString()))
                    wrapper.regDom = atom.getPredicate().getName();
                else if (bodyRanVar != null && atom.getArgument(0).toString().equals(bodyRanVar.toString()))
                    wrapper.regRan = atom.getPredicate().getName();
                if (subClassOf(atom, body)) {
                    atoms.get(body).subProperty = atom.getPredicate().getName();
                }
            }
        }
    }

    private static boolean subClassOf(Atom atom, Atom body) {
        Term[] atomArgs = atom.getArguments();
        Term[] bodyArgs = body.getArguments();
        if (atomArgs.length != bodyArgs.length) {
            return false;
        } else {
            for (int i = 0; i < atomArgs.length; i++) {
                if (atomArgs[i] != bodyArgs[i])
                    return false;
            }
        }
        return true;
    }

    private static boolean inverseOf(Atom atom, Atom body) {
        Term[] atomArgs = atom.getArguments();
        Term[] bodyArgs = body.getArguments();
        if (atomArgs.length != bodyArgs.length) {
            return false;
        } else {
            for (int i = 0; i < atomArgs.length; i++) {
                if (atomArgs[i] != bodyArgs[bodyArgs.length-1-i])
                    return false;
            }
        }
        return true;
    }

    //Prints the atoms out
    private static void printOWLs(AtomWrapper atomWrapper, PrintStream print) {
        Atom body = atomWrapper.atom;
        if (body.getNumberOfArguments() == 2) {
            print.println("<owl:ObjectProperty rdf:about=\"#" + body.getPredicate().getName() + "\">");
            if (atomWrapper.subProperty != null) {
                print.println("<rdfs:subPropertyOf rdf:resource=\"#" + atomWrapper.subProperty + "\"/>");
            }
            if (atomWrapper.inverseOf != null) {
                print.println("<rdfs:inverseOf rdf:resource=\"#" + atomWrapper.inverseOf + "\"/>");
            }

            if (atomWrapper.regDom != null) {
                print.println("<rdfs:domain rdf:resource=\"#" + atomWrapper.regDom + "\"/>");
            } else if (atomWrapper.domOnProp != null) {
                print.println("<rdfs:domain>");
                print.println("<owl:Restriction>");

                print.println("<owl:onProperty rdf:resource=\"#" + atomWrapper.domOnProp + "\"/>");
                print.println("<owl:someValuesFrom rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\"/>");

                print.println("</owl:Restriction>");
                print.println("</rdfs:domain>");
            }

            if (atomWrapper.regRan != null) {
                print.println("<rdfs:range rdf:resource=\"#" + atomWrapper.regRan + "\"/>");
            } else if (atomWrapper.ranOnProp != null) {
                print.println("<rdfs:range>");
                print.println("<owl:Restriction>");

                print.println("<owl:onProperty rdf:resource=\"#" + atomWrapper.ranOnProp + "\"/>");
                if (atomWrapper.ranSomValues != null)
                    print.println("<owl:someValuesFrom rdf:resource=\"#" + atomWrapper.ranSomValues + "\"/>");

                print.println("</owl:Restriction>");
                print.println("</rdfs:range>");
            }

            print.println("</owl:ObjectProperty>");
            if ( atomWrapper.inverseOf == null) {
                print.println("<owl:ObjectProperty rdf:ID=\"io_" + body.getPredicate().getName() + "\">\n" +
                        "<owl:inverseOf rdf:resource=\"" + body.getPredicate().getName() + "\"/>\n" +
                        "</owl:ObjectProperty>");
            }


        } else {
            if (atomWrapper.subProperty != null) {
                print.println("<owl:Class rdf:about=\"#" + body.getPredicate().getName() + "\">");
                print.println("<rdfs:subClassOf rdf:resource=\"#" + atomWrapper.subProperty + "\"/>");
                print.println("</owl:Class>");
            } else {
                print.println("<owl:Class rdf:about=\"#" + body.getPredicate().getName() + "\"/>");
            }
        }
    }
}

//Quick class that contains info to create the properties of the atom in the ontology
class AtomWrapper {
    Atom atom = null;
    String ranOnProp = null;
    String ranSomValues = null;
    String domOnProp = null;
    String regDom = null;
    String regRan = null;
    String subProperty = null;
    String inverseOf =null;
}
