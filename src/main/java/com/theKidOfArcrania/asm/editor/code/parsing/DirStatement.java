package com.theKidOfArcrania.asm.editor.code.parsing;

import org.objectweb.asm.MethodVisitor;

/**
 * This statement represents a directive. Directives are prepended by a $.
 * @author Henry Wang
 */
public class DirStatement extends CodeStatement
{
    public static DirStatement parseStatement(CodeParser parser, CodeTokenReader reader)
    {
        //TODO
        return null;
    }

    @Override
    public boolean resolveSymbols()
    {
        return false;
    }

    @Override
    public void write(MethodVisitor writer)
    {

    }

    @Override
    public void reset()
    {

    }
}
