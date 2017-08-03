package com.theKidOfArcrania.asm.editor.context;

import java.lang.reflect.Modifier;

/**
 * Represents the various access modifiers that can be applied on a member of a class.
 * @author Henry Wang
 */
public enum AccessModifier
{
    PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE;

    /**
     * Obtains an access modifier from the modifiers mask.
     * @param modifiers the modifier values used internally.
     * @return an access modifier.
     */
    public static AccessModifier getAccessModifier(int modifiers)
    {
        if (Modifier.isPublic(modifiers))
            return PUBLIC;
        else if (Modifier.isProtected(modifiers))
            return PROTECTED;
        else if (Modifier.isPrivate(modifiers))
            return PRIVATE;
        else
            return PACKAGE_PRIVATE;
    }

    /**
     * Determines whether if this access modifier can imply the access modifier specified. In other word, if a
     * class member is accessible, described by this access modifier, whether if another class member with the
     * specified access modifier similarly placed in the same scope as the first class member can also be
     * accessed. In essence this compares the following expression: <code>this.ordinal() >= other.ordinal()</code>.
     * @param other the other access modifier to compare against.
     * @return true if other modifier can likewise be accessible, false if inaccessible.
     */
    public boolean implies(AccessModifier other)
    {
        return this.ordinal() >= other.ordinal();
    }
}
