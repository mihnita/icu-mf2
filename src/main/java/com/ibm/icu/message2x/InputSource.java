package com.ibm.icu.message2x;

public class InputSource {
	final String buffer;
	final private int length;
	private int cursor;

	InputSource(String input) {
		if (input == null) {
			throw new IllegalArgumentException("Input string should not be null");
		}
		this.buffer = input;
		length = buffer.length();
		this.cursor = 0;
	}

	public boolean atEnd() {
		return cursor >= length;
	}

	int lastReadCursor = -1;
	int lastReadCount = 0;
	public char readChar() {
		if (lastReadCursor != cursor) {
			lastReadCursor = cursor;
			lastReadCount = 1;
		} else {
			lastReadCount++;
			if (lastReadCount >= 5) {
				throw new RuntimeException("Stuck in a loop!");
			}
		}
		// intentionally crash, for now
		if (atEnd()) {
//			return ' ';
		}
		return buffer.charAt(cursor++);
	}

	// Backup a number of characters.
	public void backup(int amount) {
		//TODO: validate
		cursor -= amount;
	}

	public String suffix(int len) {
		//TODO: validate
		return buffer.substring(cursor, cursor + len);
	}

	public int getPosition() {
		return cursor;
	}

	public void skip(int amount) {
		//TODO: validate
		cursor += amount;
	}
}
