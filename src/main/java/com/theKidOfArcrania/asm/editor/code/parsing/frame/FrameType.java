package com.theKidOfArcrania.asm.editor.code.parsing.frame;

import org.objectweb.asm.Opcodes;

/**
 * Represents all the valid verification types used by stack map frames.
 * @author Henry Wang
 */
public enum FrameType
{
    TOP(Opcodes.TOP, 1, "uninitialized value", 'T'), INTEGER(Opcodes.INTEGER, 1, "integer value", 'I'),
    LONG(Opcodes.LONG, 2, "long integer value", 'J'), FLOAT(Opcodes.FLOAT, 1, "single-precision value", 'F'),
    DOUBLE(Opcodes.DOUBLE, 2, "double-precision value", 'D'), NULL(Opcodes.NULL, 1, "null object", 'N'),
    UNINITIALIZED_THIS(Opcodes.UNINITIALIZED_THIS, 1, "uninitialized this", 'U'), OBJECT(null, 1, "object", 'L'),
    ARRAY(null, 1, "object", '['), UNINITIALIZED(null, 1, "uninitialized object", 'X');

    private final Object opcode;
    private final int size;
    private final String humanForm;
    private final char marker;

    /**
     * Constructs a frame type
     * @param opcode the associated opcode, or <code>null</code> if no opcode is associated with it.
     * @param size the number of frame elements this will take up. (either 1 or 2).
     * @param humanForm the human readable string of this frame type.
     * @param marker the one character marker marking this type.
     */
    FrameType(Object opcode, int size, String humanForm, char marker)
    {
        if (size != 1 && size != 2)
            throw new IllegalArgumentException();
        this.opcode = opcode;
        this.size = size;
        this.humanForm = humanForm;
        this.marker = marker;
    }

    public Object getOpcode()
    {
        return opcode;
    }

    public int getSize()
    {
        return size;
    }

    public String getHumanForm()
    {
        return humanForm;
    }

    public char getMarker()
    {
        return marker;
    }

    @Override
    public String toString()
    {
        return humanForm;
    }
}
