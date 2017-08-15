package com.theKidOfArcrania.asm.editor.code.highlight;

import com.theKidOfArcrania.asm.editor.code.parsing.Range;

/**
 * Represents a syntax highlight. This just consists of a particular token or characters with special meaning within
 * a block of code. It can be used to colorize the code to make it more appealing.
 * @author Henry Wang
 */
public class Syntax extends HighlightMark<SyntaxType>
{

    /**
     * Creates a syntax highlight
     * @param type the type of syntax
     * @param span the range span that this syntax spans across.
     */
    public Syntax(SyntaxType type, Range span)
    {
        super(type, span);
    }
}
