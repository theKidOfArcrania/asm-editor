package com.theKidOfArcrania.asm.editor.context;

import java.util.Objects;

/**
 * @author Henry Wang
 */
public abstract class MemberContext
{

    /**
     * Ensures that the specified name is a valid identifier.
     * @param identifier the identifier to test.
     * @throws IllegalArgumentException if method is not valid identifier.
     */
    private static void ensureIdentifier(String identifier)
    {
        if (identifier.isEmpty())
            throw new IllegalArgumentException("Empty identifier.");

        char[] name = identifier.toCharArray();
        boolean beginning = true;
        for (char ch : name)
        {
            if (beginning)
            {
                beginning = false;
                if (!Character.isJavaIdentifierStart(ch))
                    throw new IllegalArgumentException("Illegal character in identifier.");
            }
            else if (!Character.isJavaIdentifierPart(ch))
                throw new IllegalArgumentException("Illegal character in identifier.");
        }
    }

    private final ClassContext owner;
    private int modifiers;
    String name;
    TypeSignature signature;

    /**
     * Constructs a member context that is owned by a class context.
     * @param owner the class context that contains this member.
     * @param modifiers the modifiers (i.e. access modifiers) for this member.
     * @param name the identifier for this member.
     * @param signature the type signature.
     */
    MemberContext(ClassContext owner, int modifiers, String name, TypeSignature signature)
    {
        this.owner = Objects.requireNonNull(owner);
        this.modifiers = modifiers;
        this.name = name;
        this.signature = signature;

        if (this instanceof MethodContext && (name.equals("<init>") || name.equals("<clinit>")))
            return;
        ensureIdentifier(name);
    }

    public ClassContext getOwner()
    {
        return owner;
    }

    public String getName()
    {
        return name;
    }

    public TypeSignature getSignature()
    {
        return signature;
    }

    public AccessModifier getAccessModifier()
    {
        return AccessModifier.getAccessModifier(getModifiers());
    }

    public int getModifiers()
    {
        return modifiers;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemberContext that = (MemberContext) o;

        if (this instanceof MethodContext)
            return owner.equals(that.owner) && name.equals(that.name) && signature.equals(that.signature);
        else
            return owner.equals(that.owner) && name.equals(that.name);
    }

    @Override
    public int hashCode()
    {
        int result = owner.hashCode();
        result = 31 * result + name.hashCode();
        if (this instanceof MethodContext)
            result = 31 * result + signature.hashCode();
        return result;
    }
}
