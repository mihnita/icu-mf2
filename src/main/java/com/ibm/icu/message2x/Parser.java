package com.ibm.icu.message2x;

public class Parser {
	final String input;
	final Tokenizer tokenizer;
	
	private Parser(String input) {
		this.input = input;
		this.tokenizer = new Tokenizer(input);
	}

	public Mf2DataModel parseImpl() {
		Token<?> token = tokenizer.nextToken();	
		while (token != null && token.kind == Token.Type.EOF)
			token = tokenizer.nextToken();
		return null;
	}

	static public Mf2DataModel parse(String input) {
		return new Parser(input).parseImpl();
	}
}
