package com.theKidOfArcrania.asm.editor.context;

import java.lang.reflect.Modifier;

/**
 * Embodies a context used to identify a field contained within a {@link ClassContext}.
 * @author Henry Wang
 */
public class FieldContext extends MemberContext
{

    /**
     * Constructs a field context. This should only be internally called by {@link ClassContext}.
     * @param owner the class context that contains the field
     * @param modifiers the modifiers (i.e. access modifiers) for this field.
     * @param name the name of the field this code is written to
     * @param signature the signature of the field.
     * @throws IllegalArgumentException if field name is not a valid identifier or if type signature is not for a
     * field, or if there are some illegal modifiers.
     */
    FieldContext(ClassContext owner, int modifiers, String name, TypeSignature signature)
    {
        super(owner, modifiers, name, signature);
        if ((~Modifier.fieldModifiers() & modifiers) != 0)
            throw new IllegalArgumentException("Illegal modifier flag(s).");
        if (signature.getSort() == TypeSort.METHOD || signature.getSort() == TypeSort.VOID)
            throw new IllegalArgumentException("Expected a field type signature.");
    }

    /**
     * Sets a new field name and/or signature. This is internally called by
     * {@link ClassContext#renameField(FieldContext, String, TypeSignature)}.
     * @param mold the method context model to change name/type-signature to.
     */
    void renameTo(FieldContext mold)
    {
        this.name = mold.name;
        this.signature = mold.signature;
    }
}
