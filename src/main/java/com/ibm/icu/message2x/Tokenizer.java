package com.ibm.icu.message2x;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
	private final InputSource input;
	boolean complexMessage = false;

	public Tokenizer(String input) {
		this.input = new InputSource(input);
	}

	public Token<?> nextToken() {
		int startPos = input.getPosition();
		Token<?> result = null;
		while (!input.atEnd()) {
			char c = input.readChar();
			if (c == '.' && input.getPosition() == 0) {
				// Shortcut for a lot of complexity.
				// But TLDR is that all complex messages start with '.'
				// (after going though many-many rules)
				complexMessage = true;
			} else if (c == '}') {
				c = input.readChar();
				if (c == '}') {
					result = new TokenString(Token.Type.RDBLCURLY,
							input.buffer, startPos, startPos + 2, "}}");
				} else {
					input.backup(1);
					result = new TokenString(Token.Type.RCURLY,
							input.buffer, startPos, startPos + 1, "}");
				}
			} else if (c == '{') {
				c = input.readChar();
				if (c == '{') {
					result = new TokenString(Token.Type.LDBLCURLY,
							input.buffer, startPos, startPos + 2, "{{");
				} else {
					input.backup(1);
					result = new TokenString(Token.Type.LCURLY,
							input.buffer, startPos, startPos + 1, "{");
				}
			} else if (c == '|') {
				input.backup(1);
				result = getQuoted();
			} else if (c == '-' || (c >= '0' && c <= '9')) {
				// abnf: number-literal = ["-"] (%x30 / (%x31-39 *DIGIT)) ["." 1*DIGIT] [%i"e" ["-" / "+"] 1*DIGIT]
				input.backup(1);
				result = getNumberLiteral();
			} else {
			}
		}
		return result;
	}

	final static Pattern NUMBER_PATTERN = Pattern.compile("-?(0|([1-9]\\d*))(\\.\\d*)?([eE][-+]?\\d+)?");
	private Token<?> getNumberLiteral() {
		int start = input.getPosition();
		String tmpBuffer = input.buffer.substring(start);
		Matcher m = NUMBER_PATTERN.matcher(tmpBuffer);
		if (m.matches()) {
			String matchingPart = m.group();
			input.skip(matchingPart.length());
			return new TokenNumber(Token.Type.NUMBER,
					input.buffer, start, input.getPosition(),
					Double.valueOf(matchingPart));			
		}
		return new TokenNumber(Token.Type.NUMBER, input.buffer, start, input.getPosition(), 0);
	}

	/**
 	 * abnf: content-char = %x0000-0008        ; omit HTAB (%x09) and LF (%x0A)
 	 * abnf:              / %x000B-000C        ; omit CR (%x0D)
 	 * abnf:              / %x000E-0019        ; omit SP (%x20)
 	 * abnf:              / %x0021-002D        ; omit . (%x2E)
 	 * abnf:              / %x002F-003F        ; omit @ (%x40)
 	 * abnf:              / %x0041-005B        ; omit \ (%x5C)
 	 * abnf:              / %x005D-007A        ; omit { | } (%x7B-7D)
 	 * abnf:              / %x007E-D7FF        ; omit surrogates
 	 * abnf:              / %xE000-10FFFF
 	 */
	boolean isContentChar(char c) {
		return c != '\t'
				&& c != '\r'
				&& c != '\n'
				&& c != ' '
				&& c != '@'
				&& c != '\\'
				&& c != '{'
				&& c != '|'
				&& c != '}'
				&& !Character.isSurrogate(c)
				;
	}

	/**
	 * abnf: backslash = %x5C ; U+005C REVERSE SOLIDUS "\"
 	 */
	boolean isBackslash(char c) {
		return c == '\\';
	}

	/**
	 * ; Whitespace
	 * s = 1*( SP / HTAB / CR / LF / %x3000 )
 	 */
	boolean isWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\u3000';
	}

	/**
	 * abnf: name-start = ALPHA / "_"
	 * abnf:            / %xC0-D6 / %xD8-F6 / %xF8-2FF
	 * abnf:            / %x370-37D / %x37F-1FFF / %x200C-200D
	 * abnf:            / %x2070-218F / %x2C00-2FEF / %x3001-D7FF
	 * abnf:            / %xF900-FDCF / %xFDF0-FFFC / %x10000-EFFFF
 	 */
   	boolean isNameStart(int codePoint) {
   		// ALPHA means plain ASCII, A-Z and a-z, see
   		// https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
   		return (codePoint >= 'A' && codePoint <= 'Z') // ALPHA
   				|| (codePoint >= 'a' && codePoint <= 'z') // ALPHA
   				|| (codePoint >= 0x00C0 && codePoint <= 0x00D6)
   				|| (codePoint >= 0x00D8 && codePoint <= 0x00F6)
   				|| (codePoint >= 0x00F8 && codePoint <= 0x02FF)
   				|| (codePoint >= 0x0370 && codePoint <= 0x037D)
   				|| (codePoint >= 0x037F && codePoint <= 0x1FFF)
   				|| (codePoint >= 0x200C && codePoint <= 0x200D)
   				|| (codePoint >= 0x2070 && codePoint <= 0x218F)
   				|| (codePoint >= 0x2C00 && codePoint <= 0x2FEF)
   				|| (codePoint >= 0x3001 && codePoint <= 0xD7FF)
   				|| (codePoint >= 0xF900 && codePoint <= 0xFDCF)
   				|| (codePoint >= 0xFDF0 && codePoint <= 0xFFFC)
   				|| (codePoint >= 0x10000 && codePoint <= 0xEFFFF);
	}

	/**
     * abnf: name-char = name-start / DIGIT / "-" / "."
	 * abnf:           / %xB7 / %x300-36F / %x203F-2040
 	 */
  	boolean isNameChar(int codePoint) {
   		// DIGIT means plain ASCII, 0-9, see
   		// https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
  		return isNameStart(codePoint)
  				|| (codePoint >= '0' && codePoint <= '9') // DIGIT
  				|| codePoint == '-'
  				|| codePoint == '.'
				|| codePoint == 0x00B7
				|| (codePoint >= 0x0300 && codePoint <= 0x036F)
				|| (codePoint >= 0x203F && codePoint <= 0x2040);
	}

  	/*
  	 * abnf: private-start = "^" / "&"
  	 */
	boolean isPrivateStart(int codePoint) {
		return codePoint == '^' || codePoint == '&';
  	}

	
	/*
	 * abnf: simple-start = simple-start-char / text-escape / placeholder
	 */
	public char getSimpleStart() {
		char c = input.readChar();

		// abnf: simple-start-char = content-char / s / "@" / "|"
		if (isContentChar(c) || isWhitespace(c) || c == '@' || c == '|') {
			return c;
		}
		getTextEscape();
		// placeholder
		return ' ';
	}
	
	// abnf: text-escape = backslash ( backslash / "{" / "}" )
	public char getTextEscape() {
		char c = input.readChar();
		if (c == '\\') {
			c = input.readChar();
			if (c == '\\' || c == '{' || c == '}') {
				return c;
			} else {
				error("Invalid escape sequence, only '\\', '{' and '}' are acceptable here.");
			}
		}
		return ' ';
	}
	
	/*
	 * abnf: quoted = "|" *(quoted-char / quoted-escape) "|"
	 */
	public Token<String> getQuoted() {
		StringBuilder value = new StringBuilder();
		int start = input.getPosition();
		char c = input.readChar();
		if (c != '|') {
			error("Expecter starting '|' at offset {}, found {}");
		}
		do {
			c = input.readChar();
			if (c == '\\') {
				System.out.println("breakpoint");
			}
			if (isQuotedChar(c)) {
				value.append(c);
			} else if (c == '\\') {
				c = input.readChar();
				// abnf: quoted-escape = backslash ( backslash / "|" )
				if (c == '\\' || c == '|') {
					value.append(c);
				} else {
					error("Invalid escape sequence \\{c}");
				}
			}
		} while (!input.atEnd());
		if (c != '|') {
			error("Expecter terminating '|' at offset {}, found {}");
		}
		return new Token<>(Token.Type.STRING,
				input.buffer, start, input.getPosition(), "");
	}

	/*
	 * abnf: quoted-char = content-char / s / "." / "@" / "{" / "}"
	 */
	private boolean isQuotedChar(char c) {
		return isContentChar(c)
				|| isWhitespace(c)
				|| c == '.'
				|| c == '@'
				|| c== '{'
				|| c == '}';
	}

	/* Other literals to encode:
	 * "{{"
	 * "}}"
	 * "="
	 * "*"
	 * "{"
	 * "}"
	 * "/"
	 * "#"
	 * ":"
	 * "@"
	 * "$"
	 * "|"
	 * "."
	 * reserved-annotation-start
	 * "!" | "%" | "*" | "+" | "<" | ">" | "?" | "~"
	 *
	 * "-" => for number-literal
	 * "." => for number-literal
	 * "e" => for number-literal
	 * "-" => for number-literal
	 * "+" => for number-literal
	 *
	 * ".input"
	 * ".local"
	 * ".match" 
	 */
	private void error(String string) {
		// TODO Auto-generated method stub
	}
}
