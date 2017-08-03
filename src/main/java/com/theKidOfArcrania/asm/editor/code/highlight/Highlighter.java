package com.theKidOfArcrania.asm.editor.code.highlight;


/**
 * @author Henry Wang
 */
public interface Highlighter
{
    /**
     * Removes all tags and syntax highlights on a specific line.
     * @param line the 1-based line number
     */
    void invalidateLine(int line);

    /**
     * Just removes any tags on the line.
     * @param line the 1-based line number
     */
    void invalidateTags(int line);

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
