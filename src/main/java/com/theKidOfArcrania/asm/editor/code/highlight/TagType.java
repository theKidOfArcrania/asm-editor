package com.theKidOfArcrania.asm.editor.code.highlight;

/**
 * Represents all the valid tag types that can be used.
 * @author Henry Wang
 */
public enum TagType
{
    ERROR, WARNING;

    @Override
    public String toString()
    {
        return "tag-" + super.toString().toLowerCase();
    }
}
