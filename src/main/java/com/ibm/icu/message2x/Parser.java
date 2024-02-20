package com.ibm.icu.message2x;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    final String input;
    final Tokenizer tokenizer;

    private Parser(String input) {
        this.input = input;
        this.tokenizer = new Tokenizer(input);
    }

    public Mf2DataModel parseImpl() {
        Token<?> token = tokenizer.nextToken();	
        while (token != null && token.getKind() != Token.Type.EOF)
            token = tokenizer.nextToken();
        return null;
    }

    static public Mf2DataModel parse(String input) {
        return new Parser(input).parseImpl();
    }

    // visible for debugging
    static List<Token<?>> tokenizeAll(String input) {
        Parser parser = new Parser(input);
        List<Token<?>> result = new ArrayList<>();
        Token<?> token;
        do {
            token = parser.tokenizer.nextToken();	
            result.add(token);
        } while (token.getKind() != Token.Type.EOF);
        return result;
    }

    static class SimpleMessage {
    }

    static class ComplexMessage {
    }

    static public Mf2DataModel parseX(String input) {
        InputSource src = new InputSource(input);

        // abnf: message = simple-message / complex-message
        SimpleMessage simpleMessage = getSimpleMessage(src);
        ComplexMessage complexMessage = getComplexMessage(src);
        if (simpleMessage == null) {
            complexMessage = getComplexMessage(src);
        }
        if (simpleMessage == null && complexMessage == null) {
            throw new Mf2Exception("Invalid syntax");
        }
        return null;
    }

    // abnf: simple-message    = [simple-start pattern]
    private static SimpleMessage getSimpleMessage(InputSource src) {
        Mf2DataModel.Pattern parts = new Mf2DataModel.Pattern(); 
        Mf2DataModel.PatternPart simplePart = getSimpleStart(src);
        if (simplePart == null) {
            // Does not look like a simple message
            return null;
        }
        parts.parts.add(simplePart);
        while (true) {
            Mf2DataModel.PatternPart part = getPatternPart(src);
            if (part == null) {
                break;
            }
            parts.parts.add(part);
        }
        return null;
    }

    // abnf: simple-start      = simple-start-char / text-escape / placeholder
    private static Mf2DataModel.PatternPart getSimpleStart(InputSource src) {
        StringBuilder buffer = new StringBuilder();

        int cp = src.readCodePoint();
        if (StringUtils.isSimpleStartChar(cp)) {
            buffer.appendCodePoint(cp);
            Mf2DataModel.StringPart sp = new Mf2DataModel.StringPart();
            sp.value = buffer.toString();
            return sp;
        } else if (cp == '\\') {
            // abnf: text-escape     = backslash ( backslash / "{" / "}" )
            cp = src.readCodePoint();
            if (cp == '{' || cp == '}') {
                buffer.appendCodePoint(cp);
                Mf2DataModel.StringPart sp = new Mf2DataModel.StringPart();
                sp.value = buffer.toString();
                return sp;
            } else {
                throw new Mf2Exception("Invalid escape sequence \"\\" + cp + "\"");
            }
        } else if (cp == '{') { // placeholder
             Placeholder placeholder = getPlaceholder(src);
             return new Mf2DataModel.FunctionExpression();
        }
        // Does not look like a simple start
        return null;
    }

    private static Mf2DataModel.PatternPart getPatternPart(InputSource src) {
        StringBuilder buffer = new StringBuilder();
        int cp = src.readCodePoint();
        if (StringUtils.isTextChar(cp)) {
            buffer.appendCodePoint(cp);
            Mf2DataModel.StringPart sp = new Mf2DataModel.StringPart();
            sp.value = buffer.toString();
            return sp;
        } else if (cp == '\\') {
            // abnf: text-escape     = backslash ( backslash / "{" / "}" )
            cp = src.readCodePoint();
            if (cp == '{' || cp == '}') {
                buffer.appendCodePoint(cp);
                Mf2DataModel.StringPart sp = new Mf2DataModel.StringPart();
                sp.value = buffer.toString();
                return sp;
            } else {
                throw new Mf2Exception("Invalid escape sequence \"\\" + cp + "\"");
            }
        } else if (cp == '{') { // placeholder
             Placeholder placeholder = getPlaceholder(src);
             return new Mf2DataModel.FunctionExpression();
        }
        // Does not look like a simple start
        return null;
    }

    private static Placeholder getPlaceholder(InputSource src) {
        return null;
    }

    private static class Placeholder {
        
    }

    // abnf: pattern           = *(text-char / text-escape / placeholder)
    private static void getPattern(InputSource src) {
        int cp = src.readCodePoint();
        if (StringUtils.isTextChar(cp)) {
            
        }

    }

    private static ComplexMessage getComplexMessage(InputSource src) {
        return null;
    }

    private static void error() {
        // TODO Auto-generated method stub
        
    }

}
