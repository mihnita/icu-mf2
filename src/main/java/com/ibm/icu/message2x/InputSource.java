package com.ibm.icu.message2x;

class InputSource {
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

    public int peakChar() {
        if (atEnd()) {
            return -1;
        }
        return buffer.charAt(cursor);
    }

    int lastReadCursor = -1;
    int lastReadCount = 0;
    public int readCodePoint() {
        // TODO: REMOVE
        // START Detect possible infinite loop
        if (lastReadCursor != cursor) {
            lastReadCursor = cursor;
            lastReadCount = 1;
        } else {
            lastReadCount++;
            if (lastReadCount >= 10) {
                throw new RuntimeException("Stuck in a loop!");
            }
        }
        // TODO: END Detect possible infinite loop

        if (atEnd()) {
            return -1;
        }

        char c = buffer.charAt(cursor++);
        if (Character.isHighSurrogate(c)) {
            if (!atEnd()) {
                char c2 = buffer.charAt(cursor++);
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(c, c2);
                } else { // invalid, high surrogate followed by non-surrogate
                    cursor--;
                    return c;
                }
            }
        }
        return c;
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

    public void gotoPosition(int position) {
        //TODO: validate
        cursor = position;
    }
}
