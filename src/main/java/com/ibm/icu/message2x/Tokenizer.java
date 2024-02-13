package com.ibm.icu.message2x;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.standard.RequestingUserName;

public class Tokenizer {
	private final InputSource input;
	boolean complexMessage = false;
	boolean firstTime = true;

	public Tokenizer(String input) {
		this.input = new InputSource(input);
	}

	public Token<?> nextToken() {
		int startPos = input.getPosition();
		Token<?> result = null;
		while (!input.atEnd()) {
			char c = input.readChar();
			if (firstTime) {
				firstTime = false;
				if (startPos == 0 && c == '.') { // complex message
					// A bit of a cheat, to simplify things.
					// TODO: check the behavior for ".", ".123" and other strings that are probably simple-message
					complexMessage = true;
				}
			}
			if (c == '}') {
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
			} else if (complexMessage && c == '.') {
				String keyworkName = getKeyword();
				switch (keyworkName) {
					case "input":
						result = new TokenString(Token.Type.INPUT, input.buffer, startPos, input.getPosition(), keyworkName);
						break;
					case "local":
						result = new TokenString(Token.Type.LOCAL, input.buffer, startPos, input.getPosition(), keyworkName);
						break;
					case "match":
						result = new TokenString(Token.Type.MATCH, input.buffer, startPos, input.getPosition(), keyworkName);
						break;
					default:
						if (!keyworkName.isEmpty()) {
							result = new TokenString(Token.Type.RESERVED_KEYWORD, input.buffer, startPos, input.getPosition(), keyworkName);
						} else {
							// back to simple message?
							input.backup(input.getPosition() - startPos);
							complexMessage = false;
						}
						break;
				}
			} else if (isTextChar(c)) {
				StringBuilder patternBuffer = new StringBuilder();
				patternBuffer.append(c);
				while (!input.atEnd()) {
					c = input.readChar();
					if (c == '\\') {
						c = input.readChar();
						// abnf: text-escape = backslash ( backslash / "{" / "}" )
						if (c == '\\' || c == '{' || c == '}')
							patternBuffer.append(c);
						else {
							error("invalid escape");
						}
					} else if (isTextChar(c)) {
						patternBuffer.append(c);
					}
				}
				if (!input.atEnd()) {
					input.backup(1);
				}
				result = new TokenString(Token.Type.PATTERN, input.buffer, startPos, input.getPosition(), patternBuffer.toString());
			} else {
				error("Should never get here?");
			}
		}
		if (result == null) {
			return new TokenString(Token.Type.EOF, input.buffer, startPos, input.getPosition(), null);
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
	
	/*
	 * abnf: text-char = content-char / s / "." / "@" / "|"
	 */
	boolean isTextChar(char c) {
		return isContentChar(c) || isWhitespace(c) || c == '.' || c == '@' || c == '|';
	}

	/**
	 * abnf: backslash = %x5C ; U+005C REVERSE SOLIDUS "\"
 	 */
	boolean isBackslash(char c) {
		return c == '\\';
	}

	/* Covers all tokens that start with a '.', for now `.input`, `.local`, `.match`, and `reserved-keyword`
	 * abnf: ; Keywords; Note that these are case-sensitive
	 * abnf: input = %s".input"
	 * abnf: local = %s".local"
	 * abnf: match = %s".match"
	 * abnf: reserved-keyword   = "." name
	 */
	private String getKeyword() {
		if (input.atEnd()) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		char c = input.readChar();
		if (isNameChar(c)) {
			result.append(c);
		}
		while (!input.atEnd()){
			c = input.readChar();
			if (isNameChar(c) && !input.atEnd()) {
				result.append(c);
			}
		} 
		return result.toString();
	}
	
	/**
	 * ; Whitespace
	 * abnf: s = 1*( SP / HTAB / CR / LF / %x3000 )
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
			if (isQuotedChar(c)) {
				value.append(c);
			} else if (c == '\\') {
				c = input.readChar();
				// abnf: quoted-escape = backslash ( backslash / "|" )
				if (c == '|' || c == '\\') {
					value.append(c);
				} else {
					error("Invalid escape sequence \\{c}");
				}
			} else if (c == '|') {
				// end of string. Exit the loop, and don't include it in the parsed value
				break;
			} else {
				value.append(c);
			}
		} while (!input.atEnd());
		if (c != '|') {
			error("Expecter terminating '|' at offset {}, found {}");
		}
		return new TokenString(Token.Type.STRING,
				input.buffer, start, input.getPosition(), value.toString());
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

	/*
	 * abnf: reserved-char = content-char / "."
	 */
	boolean isReservedChar(char c) {
		return isContentChar(c) || c == '.';
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
