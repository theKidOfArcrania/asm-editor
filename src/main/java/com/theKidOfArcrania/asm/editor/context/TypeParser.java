package com.theKidOfArcrania.asm.editor.context;

import static com.theKidOfArcrania.asm.editor.context.TypeSort.*;

/**
 * This utility class is used to track certain states within parsing a type signature descriptor. This is internally
 * used by {@link TypeSignature} in order to parse an internal descriptor into a {@code TypeSignature}.
 *
 * @author Henry Wang
 */
class TypeParser
{

    private static final TypeSort[] nameMarkers = new TypeSort[Byte.MAX_VALUE];

    static
    {
        for (TypeSort s : TypeSort.values())
            nameMarkers[s.getMarker()] = s;
    }

    private char[] buff;
    private int ind;

    private String classDescriptor;
    private int dim;

    /**
     * Constructs a new parser from a type-signature descriptor.
     * @param descriptor the type-signature descriptor.
     */
    public TypeParser(String descriptor)
    {
        buff = descriptor.toCharArray();
        ind = 0;
    }

    public boolean isEnding()
    {
        return ind >= buff.length;
    }

    /**
     * Moves parsing to the next sort within the type signature descriptor.
     * @return the next type sort, or null if an error occurs.
     */
    public TypeSort nextTypeSort()
    {
        if (ind >= buff.length)
            return null;

        TypeSort ts = nameMarkers[buff[ind]];
        if (ts == null)
            return null;

        switch (ts)
        {
            case ARRAY:
                dim = 0;
                while(ind < buff.length && buff[ind] == ARRAY.getMarker())
                {
                    ind++;
                    dim++;
                }
                if (ind >= buff.length)
                    return null;
                break;
            case OBJECT:
                int start = ind + 1;
                while (ind < buff.length && buff[ind] != OBJECT.getEndMarker())
                    ind++;
                if (ind >= buff.length)
                    return null;
                classDescriptor = new String(buff, start, ind - start);
                ind++;
                if (!ClassContext.verifyClassNameFormat(classDescriptor))
                    return null;
                break;
            case METHOD:
                if (ind != 0)
                    return null;
                ind++;
                break;
            default:
                ind++;
        }
        return ts;
    }

    public int getDimensions()
    {
        return dim;
    }

    public String getClassDescriptor()
    {
        return classDescriptor;
    }

    public boolean isEndingParameter()
    {
        if (ind < buff.length && buff[ind] == METHOD.getEndMarker())
        {
            ind++;
            return true;
        }
        return false;
    }
}
