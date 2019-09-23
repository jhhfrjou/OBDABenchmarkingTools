package uk.ac.ox.cs.chaseBench.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ox.cs.chaseBench.model.Atom;
import uk.ac.ox.cs.chaseBench.model.Constant;
import uk.ac.ox.cs.chaseBench.model.Domain;
import uk.ac.ox.cs.chaseBench.model.LabeledNull;
import uk.ac.ox.cs.chaseBench.model.Predicate;
import uk.ac.ox.cs.chaseBench.model.Rule;
import uk.ac.ox.cs.chaseBench.model.Term;
import uk.ac.ox.cs.chaseBench.model.Variable;
import uk.ac.ox.cs.chaseBench.processors.InputProcessor;

public class CommonParser {
    protected final StreamTokenizer m_tokenizer;

    protected enum TokenType { SYMBOL, INTEGER, DOUBLE, INVALID }

    public CommonParser(Reader reader) throws IOException {
        m_tokenizer = new StreamTokenizer(reader);
        m_tokenizer.resetSyntax();
        m_tokenizer.commentChar('#');
        m_tokenizer.eolIsSignificant(false);
        m_tokenizer.whitespaceChars(' ', ' ');
        m_tokenizer.whitespaceChars('\r', '\r');
        m_tokenizer.whitespaceChars('\n', '\n');
        m_tokenizer.whitespaceChars('\t', '\t');
        m_tokenizer.wordChars('_', '_');
        m_tokenizer.wordChars('a', 'z');
        m_tokenizer.wordChars('A', 'Z');
        m_tokenizer.wordChars('0', '9');
        m_tokenizer.wordChars('.', '.');
        m_tokenizer.wordChars('+', '+');
        m_tokenizer.wordChars('-', '-');
        m_tokenizer.wordChars('?', '?');
        m_tokenizer.wordChars('<', '<');
        m_tokenizer.wordChars('>', '>');
        m_tokenizer.quoteChar('"');
        m_tokenizer.nextToken();
    }

    protected static boolean isFirstSymbolChar(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '-' || c == '_';
    }

    protected static boolean isSymbolChar(char c) {
        return isFirstSymbolChar(c) || ('0' <= c && c <= '9');
    }

    protected static TokenType getTokenType(String token) {
        boolean seenDot = false;
        boolean seenDigitBeforeDot = false;
        boolean seenDigitAfterDot = false;
        int index = 0;
        if (token.charAt(0) == '+' || token.charAt(0) == '-')
            ++index;
        for (; index < token.length(); ++index) {
            char c = token.charAt(index);
            if (c == '.') {
                if (seenDot)
                    return TokenType.INVALID;
                seenDot = true;
            }
            else if ('0' <= c && c <= '9') {
                if (seenDot)
                    seenDigitAfterDot = true;
                else
                    seenDigitBeforeDot = true;
            }
            else {
                if (!isFirstSymbolChar(token.charAt(0)))
                    return TokenType.INVALID;
                for (int inner = 1; inner < token.length(); ++inner)
                    if (!isSymbolChar(token.charAt(inner)))
                        return TokenType.INVALID;
                return TokenType.SYMBOL;
            }
        }
        if (seenDot) {
            if (seenDigitAfterDot)
                return TokenType.DOUBLE;
            else
                return TokenType.INVALID;
        }
        else {
            if (seenDigitBeforeDot)
                return TokenType.INTEGER;
            else
                return TokenType.INVALID;
        }
    }

    public Term termFromWord(String token) throws IOException {
        if (token.startsWith("?"))
            return Variable.create(token.substring(1));
        else if (token.startsWith("_:"))
            return LabeledNull.create(token.substring(2));
        else {
            TokenType tokenType = getTokenType(token);
            switch (tokenType) {
            case SYMBOL:
                return Constant.create(token, Domain.SYMBOL);
            case INTEGER:
                return Constant.create(token, Domain.INTEGER);
            case DOUBLE:
                return Constant.create(token, Domain.DOUBLE);
            default:
                throw new IOException("Invalid term '" + token + "'.");
            }
        }
    }

    public Term parseTerm() throws IOException {
        if (m_tokenizer.ttype == StreamTokenizer.TT_WORD) {
            String token = m_tokenizer.sval;
            m_tokenizer.nextToken();
            return termFromWord(token);
        }
        else if (m_tokenizer.ttype == '"') {
            Constant constant = Constant.create( m_tokenizer.sval, Domain.STRING);
            m_tokenizer.nextToken();
            return constant;
        }
        else
            throw new IOException("A term was expected at this point.");
    }

    public Atom parseAtom() throws IOException {
        if (m_tokenizer.ttype == StreamTokenizer.TT_WORD) {
            String token = m_tokenizer.sval;
            m_tokenizer.nextToken();
            List<Term> arguments = new ArrayList<Term>();
            if (m_tokenizer.ttype == '=') {
                m_tokenizer.nextToken();
                Term term1 = termFromWord(token);
                Term term2 = parseTerm();
                return Atom.create(Predicate.EQUALS, term1, term2);
            }
            else {
                if (m_tokenizer.ttype != '(')
                    throw new IOException("Expected '('.");
                m_tokenizer.nextToken();
                if (m_tokenizer.ttype != ')') {
                    arguments.add(parseTerm());
                    while (m_tokenizer.ttype == ',') {
                        m_tokenizer.nextToken();
                        arguments.add(parseTerm());
                    }
                    if (m_tokenizer.ttype != ')')
                        throw new IOException("Expected ')'.");
                }
                m_tokenizer.nextToken();
                return Atom.create(Predicate.create(token), arguments.toArray(new Term[arguments.size()]));
            }
        }
        else {
            Term term1 = parseTerm();
            if (m_tokenizer.ttype != '=')
                throw new IOException("Expected '='.");
            m_tokenizer.nextToken();
            Term term2 = parseTerm();
            return Atom.create(Predicate.EQUALS, term1, term2);
        }
    }

    public Object parseRuleOrAtom() throws IOException {
        List<Atom> before = new ArrayList<Atom>();
        if (m_tokenizer.ttype != StreamTokenizer.TT_WORD || (!"<-".equals(m_tokenizer.sval) && !"->".equals(m_tokenizer.sval))) {
            Atom atom = parseAtom();
            if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && ".".equals(m_tokenizer.sval)) {
                m_tokenizer.nextToken();
                return atom;
            }
            before.add(atom);
            while (m_tokenizer.ttype == ',' || m_tokenizer.ttype == '&') {
                m_tokenizer.nextToken();
                before.add(parseAtom());
            }
        }
        boolean leftToRight;
        if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && "<-".equals(m_tokenizer.sval))
            leftToRight = false;
        else if (m_tokenizer.ttype == StreamTokenizer.TT_WORD && "->".equals(m_tokenizer.sval))
            leftToRight = true;
        else
            throw new IOException("Expected '<-' or '->'.");
        m_tokenizer.nextToken();
        List<Atom> after = new ArrayList<Atom>();
        if (m_tokenizer.ttype != '.') {
            after.add(parseAtom());
            while (m_tokenizer.ttype == ',' || m_tokenizer.ttype == '&') {
                m_tokenizer.nextToken();
                after.add(parseAtom());
            }
        }
        if (m_tokenizer.ttype != StreamTokenizer.TT_WORD || !".".equals(m_tokenizer.sval)) {
            System.out.println("Character: " + (char)m_tokenizer.ttype);
            throw new IOException("Expected '.' at the end of the rule.");
        }
        m_tokenizer.nextToken();
        Atom[] beforeAtoms = before.toArray(new Atom[before.size()]);
        Atom[] afterAtoms = after.toArray(new Atom[after.size()]);
        if (leftToRight)
            return Rule.create(afterAtoms, beforeAtoms);
        else
            return Rule.create(beforeAtoms, afterAtoms);
    }

    public void parse(InputProcessor inputProcessor) throws IOException {
        inputProcessor.startProcessing();
        try {
            while (m_tokenizer.ttype != StreamTokenizer.TT_EOF) {
                Object ruleOrAtom = parseRuleOrAtom();
                if (ruleOrAtom instanceof Rule)
                    inputProcessor.processRule((Rule)ruleOrAtom);
                else if (ruleOrAtom instanceof Atom) {
                	Atom atom = (Atom)ruleOrAtom;
                	List<String> argumentLexicalForms = new ArrayList<String>();
                	List<Boolean> argumentsAreLabeledNulls = new ArrayList<Boolean>();
                	for (int index = 0; index < atom.getNumberOfArguments(); ++index) {
                		Term argument = atom.getArgument(index);
                		if (argument instanceof LabeledNull) {
                			argumentLexicalForms.add(((LabeledNull)argument).getName());
                			argumentsAreLabeledNulls.add(Boolean.TRUE);
                		}
                		else if (argument instanceof Constant) {
                			argumentLexicalForms.add(((Constant)argument).getLexicalForm());
                			argumentsAreLabeledNulls.add(Boolean.FALSE);
                		}
                		else
                			throw new IOException("Facts should only contain constants and labeled nulls as arguments.");
                	}
                    inputProcessor.processFact(atom.getPredicate(), argumentLexicalForms, argumentsAreLabeledNulls);
                }
            }
        }
        finally {
            inputProcessor.endProcessing();
        }
    }
}
