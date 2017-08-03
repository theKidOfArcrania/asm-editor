package com.theKidOfArcrania.asm.editor.context;


import java.lang.reflect.Modifier;

/**
 * Embodies a context used to identify a method contained within a {@link ClassContext}.
 * @author Henry Wang
 */
public class MethodContext extends MemberContext
{

    /**
     * Constructs a method context. This should only be internally called by {@link ClassContext}.
     * @param owner the class context that contains the method
     * @param modifiers the modifiers (i.e. access modifiers) for this method.
     * @param name the identifier of the method.
     * @param signature the signature of the method.
     * @throws IllegalArgumentException if method name is not a valid identifier (can be &lt;init&gt; or &lt;
     * clinit&gt;), if type signature is not for a method, or if there are some illegal modifiers.
     */
    MethodContext(ClassContext owner, int modifiers, String name, TypeSignature signature)
    {
        super(owner, modifiers, name, signature);
        if ((~Modifier.methodModifiers() & modifiers) != 0)
            throw new IllegalArgumentException("Illegal modifier flag(s).");
        if (signature.getSort() != TypeSort.METHOD)
            throw new IllegalArgumentException("Expected a METHOD type signature.");
    }

    /**
     * Sets a new method name and/or signature. This is internally called by
     * {@link ClassContext#renameMethod(MethodContext, String, TypeSignature)}.
     * @param mold the method context model to change name/type-signature to.
     */
    void renameTo(MethodContext mold)
    {
        this.name = mold.name;
        this.signature = mold.signature;
    }

}
