package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * Represents a composition of parameter types.
 * @author Henry Wang
 */
public class MultipleParamType implements ParamType
{

    private ParamType types[];

    /**
     * Constructs a multiple parameter type from a composition of other parameter types.
     * @param types the parameter types to compose into one type
     */
    public MultipleParamType(ParamType... types)
    {
        if (types.length == 0)
            throw new IllegalArgumentException("No parameter values specified.");
        for (ParamType type : types)
        {
            if (type instanceof MultipleParamType)
                throw new IllegalArgumentException("No nested multiple parameter types.");
        }
        this.types = types;
    }

    @Override
    public ParamType getExactType(CodeTokenReader reader)
    {
        for (ParamType type : types)
        {
            ParamType exact = type.getExactType(reader);
            if (exact != null)
                return exact;
        }
        return null;
    }

    @Override
    public boolean matches(CodeTokenReader reader)
    {
        for (ParamType type : types)
        {
            if (type.matches(reader))
                return true;
        }
        return false;
    }

    @Override
    public boolean checkToken(CodeTokenReader reader)
    {
        for (ParamType type : types)
        {
            if (type.matches(reader))
                return type.checkToken(reader);
        }

        reader.errorExpected(getName());
        return false;
    }

    @Override
    public String getName()
    {
        if (types.length == 1)
            return types[0].getName();
        else if (types.length == 2)
            return types[0] + " or " + types[1];

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (ParamType type : types)
        {
            if (first)
                first = false;
            else
            {
                sb.append(", ");
                if (type == types[types.length - 1])
                    sb.append("or ");
            }
            sb.append(type.getName());
        }
        return sb.toString();
    }
}
