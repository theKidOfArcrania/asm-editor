package com.theKidOfArcrania.asm.editor.code.highlight;

import com.theKidOfArcrania.asm.editor.code.parsing.Range;

/**
 * Represents an element that is used to help emphasize a particular component, i.e. a syntax, or some sort of
 * notification within a block of code.
 * @author Henry Wang
 */
public abstract class HighlightMark<T extends Enum<T>>
{
    private final T type;
    private Range span;

    /**
     * Constructs a new highlight marker.
     * @param type the type of the highlight.
     * @param span the span of this highlight
     */
    HighlightMark(T type, Range span)
    {
        this.type = type;
        this.span = span;
    }

    public Range getSpan()
    {
        return span;
    }

    public T getType()
    {
        return type;
    }
}
