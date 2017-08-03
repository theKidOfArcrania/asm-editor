package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * Marks a position with a line number and column number corresponding to a character within a body of code
 * @author Henry Wang
 */
public class Position
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

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getColumnNumber()
    {
        return columnNumber;
    }
}
