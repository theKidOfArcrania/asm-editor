package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * Represents a range of positions within a body of code.
 *
 * @author Henry Wang
 * @see Position
 */
public class Range implements Comparable<Range>
{
    private final Position start;
    private final Position end;

    /**
     * Obtains a range only a character long at the specified position.
     * @param p the character's position.
     * @return the specified range.
     */
    public static Range characterRange(Position p)
    {
        return new Range(p, new Position(p.getLineNumber(), p.getColumnNumber() + 1));
    }

    /**
     * Obtains a range only a character long at the specified position.
     * @param lineNum the line number of character
     * @param columnNum the column index of the character
     * @return the specified range.
     */
    public static Range characterRange(int lineNum, int columnNum)
    {
        return new Range(lineNum, columnNum, lineNum, columnNum + 1);
    }

    /**
     * Obtains an entire line range that the reader is currently on.
     * @param reader the token reader
     * @return the specified range.
     */
    public static Range lineRange(CodeTokenReader reader)
    {

        return tokenRange(reader.getLineNumber(), 0, reader.getLine().length());
    }

    /**
     * Obtains an entire line range.
     * @param reader the token reader
     * @param lineNum the line number
     * @return the specified range.
     */
    public static Range lineRange(CodeTokenReader reader, int lineNum)
    {
        return new Range(lineNum, 0, lineNum, reader.getLine(lineNum).length());
    }

    /**
     * Obtains a range of a token with the specified start and end index. This can only be used to select a token on
     * one single line.
     * @param lineNum the line number
     * @param tokenStart the starting index
     * @param tokenEnd the ending index
     * @return the specified range.
     */
    public static Range tokenRange(int lineNum, int tokenStart, int tokenEnd)
    {
        return new Range(lineNum, tokenStart, lineNum, tokenEnd);
    }

    /**
     * Constructs a range from two positions, consisting of a line num and column num.
     * @param startLineNum staring line number.
     * @param startColumnNum staring column number (inclusive).
     * @param endLineNum ending line number.
     * @param endColumnNum ending column number (exclusive).
     */
    public Range(int startLineNum, int startColumnNum, int endLineNum, int endColumnNum)
    {
        this(new Position(startLineNum, startColumnNum), new Position(endLineNum, endColumnNum));
    }

    /**
     * Constructs a range from two positions.
     * @param start the starting position (inclusive).
     * @param end the ending position (exclusive).
     */
    public Range(Position start, Position end)
    {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        return start.equals(range.start) && end.equals(range.end);
    }

    @Override
    public int hashCode()
    {
        int result = start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    @Override
    public int compareTo(Range o)
    {
        if (start.equals(o.start))
            return end.compareTo(o.end);
        else
            return start.compareTo(o.start);
    }

    public Position getStart()
    {
        return start;
    }

    public Position getEnd()
    {
        return end;
    }

    @Override
    public String toString()
    {
        return "[" + start + ", " + end + "]";
    }
}
