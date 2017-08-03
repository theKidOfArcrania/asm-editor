package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * Represents a range of positions within a body of code.
 *
 * @author Henry Wang
 * @see Position
 */
public class Range
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

    public Position getStart()
    {
        return start;
    }

    public Position getEnd()
    {
        return end;
    }
}
