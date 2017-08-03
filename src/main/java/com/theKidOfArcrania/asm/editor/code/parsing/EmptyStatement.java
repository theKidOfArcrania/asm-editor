package com.theKidOfArcrania.asm.editor.code.parsing;

import org.objectweb.asm.MethodVisitor;

/**
 * Represents an empty no-op blank line.
 * @author Henry Wang
 */
public class EmptyStatement extends CodeStatement
{
    @Override
    public boolean resolveSymbols()
    {
        return true;
    }

    @Override
    public void write(MethodVisitor writer)
    {
        //Does nothing.
    }

    @Override
    public void reset()
    {
        //Does nothing.
    }
}
