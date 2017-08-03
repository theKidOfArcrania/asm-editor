package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * This is an descriptor for an argument/operand to an instruction. This contains the parsed value
 * itself, and also a variety of metadata about how it was parsed.
 */
public class Argument
{
    private final Object value;
    private final Range tokenPos;
    private final ParamType type;

    /**
     * Creates a new argument object from the current state of the token reader.
     * @param reader the token reader.
     * @param type type of the argument (doesn't have to be exact)
     */
    public Argument(CodeTokenReader reader, ParamType type)
    {
        this(reader.getTokenValue(), reader.getTokenPos(), type.getExactType(reader));
    }

    /**
     * Creates a new argument object.
     * @param value the value of the argument.
     * @param tokenPos the position of the argument token.
     * @param type the exact type of the argument.
     */
    public Argument(Object value, Range tokenPos, ParamType type)
    {
        this.value = value;
        this.tokenPos = tokenPos;
        this.type = type;
    }

    public Object getValue()
    {
        return value;
    }

    public Range getTokenPos()
    {
        return tokenPos;
    }

    public ParamType getExactType()
    {
        return type;
    }
}
