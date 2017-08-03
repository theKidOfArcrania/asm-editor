package com.theKidOfArcrania.asm.editor.context;

/**
 * Represents a basic set of categories of <code>TypeSignature</code>s. This also offers some metadata used in
 * parsing these types from its internal descriptor format.
 * @author Henry Wang
 */
public enum TypeSort
{
    ARRAY('['), BOOLEAN('Z'), BYTE('B'), CHAR('C'), DOUBLE('D'), FLOAT('F'), INTEGER('I'), LONG('J'), SHORT('S'),
    VOID('V'), METHOD('(', ')'), OBJECT('L', ';');

    private final char marker;
    private char endMarker;

    /**
     * Constructor for a regular sort.
     *
     * @param marker the character marker.
     */
    TypeSort(char marker)
    {
        this.marker = marker;
    }

    /**
     * Constructor with a marker and end-marker sort.
     *
     * @param marker    the character marker.
     * @param endMarker the character end marker.
     */
    TypeSort(char marker, char endMarker)
    {
        this(marker);
        this.endMarker = endMarker;
    }

    char getMarker()
    {
        return marker;
    }

    char getEndMarker()
    {
        return endMarker;
    }
}
