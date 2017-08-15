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
        if (type == null || span == null)
            throw new NullPointerException();
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HighlightMark<?> that = (HighlightMark<?>) o;

        return type.equals(that.type) && span.equals(that.span);
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + span.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return getType() + "@" + span;
    }
}
