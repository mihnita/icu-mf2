package com.ibm.icu.message2x;

class Token<T> {
	private static final boolean DEBUG = true;

	final Type kind;
	final String buffer;
	final int begin;
	final int end;
	final T value;
//	private Token next;
//	private Token specialToken;

	static enum Type {
		EOF,
	    STRING, // |....|
	    NUMBER, // abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
		LCURLY, RCURLY, // "{" and "}"
		LDBLCURLY, RDBLCURLY, // "{{" and "}}"
		INPUT, // .input
		LOCAL, // .local
		MATCH // .match
		;
	}

	/**
	 * @param kind token type
	 * @param begin starting index in string
	 * @param end ending index in string
	 * @param buffer the original string buffer
	 * @param value the real object represented by the input.
	 *        For example a real string, with all escapes resolved,
	 *        or a number, when the input string was "3.14" or "-3e7"  
	 */
	public Token(Type kind, String buffer, int begin, int end, T value) {
		this.kind = kind;
		this.buffer = buffer;
		this.begin = begin;
		this.end = end;
		this.value = value;
		if (DEBUG) {
			System.out.println(this.toString());
		}
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return String.format("Token [kind=%s, begin=%s, end=%s, image=%s, value=%s]",
				kind, begin, end, "\"" + buffer.substring(begin, end) + "\"", value);
	}
}
