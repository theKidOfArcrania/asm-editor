package com.theKidOfArcrania.asm.editor.code.highlight;


/**
 * @author Henry Wang
 */
public interface Highlighter
{

    /**
     * Inserts a tag highlight (consisting of a bubble and a syntax highlight).
     * @param tag the tag highlight to add
     */
    void insertTag(Tag tag);

    /**
     * Inserts a syntax highlight.
     * @param syn the syntax highlight to add.
     */
    void insertSyntax(Syntax syn);
}
