package com.theKidOfArcrania.asm.editor.code.parsing;

import org.objectweb.asm.MethodVisitor;

/**
 * Represents a parsed line of statement of the token reader. This is a base class for different types of statements,
 * including instruction lines, label lines, directives, and no-op lines.
 *
 * @author Henry Wang
 */
public abstract class CodeStatement
{
    /**
     * Verifies that all the symbols this statement refers to are resolved. This will emit any errors if needed.
     * @return true if no errors occurred, false if errors occurred.
     */
    public abstract boolean resolveSymbols();

    /**
     * Writes the statement to the method visitor instance.
     * @param writer the visitor to write statement to.
     */
    public abstract void write(MethodVisitor writer);

    /**
     * Resets any side-effects that parsing this code statement might have. This occurs whenever this statement is
     * removed, or if re-parsing has to occur. This SHOULD NOT emit any errors.
     */
    public abstract void reset();
}
