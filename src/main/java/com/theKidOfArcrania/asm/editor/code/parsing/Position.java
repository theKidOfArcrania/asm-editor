package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * Marks a position with a line number and column number corresponding to a character within a body of code
 * @author Henry Wang
 */
public class Position implements Comparable<Position>
{
    private final int lineNumber;
    private final int columnNumber;

    /**
     * Constructs a new Position
     * @param lineNumber the line number
     * @param columnNumber the new column number
     */
    public Position(int lineNumber, int columnNumber)
    {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    @Override
    public int compareTo(Position o)
    {
        if (lineNumber == o.lineNumber)
            return columnNumber - o.columnNumber;
        else
            return lineNumber - o.lineNumber;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getColumnNumber()
    {
        return columnNumber;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (lineNumber != position.lineNumber) return false;
        return columnNumber == position.columnNumber;
    }

    @Override
    public int hashCode()
    {
        int result = lineNumber;
        result = 31 * result + columnNumber;
        return result;
    }
}
