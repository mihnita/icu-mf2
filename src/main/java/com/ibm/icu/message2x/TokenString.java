package com.ibm.icu.message2x;

class TokenString extends Token<String> {
	public TokenString(Type kind, String buffer, int begin, int end, String value) {
		super(kind, buffer,begin, end, value);
	}
}
