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

}
