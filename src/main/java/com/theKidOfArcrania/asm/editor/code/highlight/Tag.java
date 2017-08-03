package com.theKidOfArcrania.asm.editor.code.highlight;

import com.theKidOfArcrania.asm.editor.code.parsing.Range;

/**
 * Represents a tag highlight. This consists of a bubble/tooltip, and also a syntax highlight. This, like the
 * {@link Syntax} object, can provide hints to the UI as to how to colorize the text to make the code more appealing.
 * Specifically with this particular class, it can also provide the user with helpful feedback as
 * errors/warnings/info on the code.
 *
 * @author Henry Wang
 */
public class Tag
{
    private final TagType type;
    private Range span;
    private String tagDescription;

    /**
     * Creates a tag without a description
     * @param type the type of tag
     * @param span the range span that this tag spans across.
     */
    public Tag(TagType type, Range span)
    {
        this(type, span, "");
    }

    /**
     * Creates a tag with a description.
     * @param type the type of the tag
     * @param span the range span that this tag spans across.
     * @param tagDescription the tag description often shown in a tooltip/bubble.
     */
    public Tag(TagType type, Range span, String tagDescription)
    {
        if (type == null || span == null || tagDescription == null)
            throw new NullPointerException();

        this.type = type;
        this.span = span;
        this.tagDescription = tagDescription;
    }

    public TagType getType()
    {
        return type;
    }

    public Range getSpan()
    {
        return span;
    }

    public String getTagDescription()
    {
        return tagDescription;
    }
}
