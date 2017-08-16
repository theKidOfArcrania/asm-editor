package com.theKidOfArcrania.asm.editor.context;


import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import org.objectweb.asm.MethodVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * Embodies a context used to identify a method contained within a {@link ClassContext}.
 * @author Henry Wang
 */
public class MethodContext extends MemberContext
{

    private HashSet<ClassContext> exceptions;
    private MethodBody body;

    /**
     * Constructs a method context. This should only be internally called by {@link ClassContext}.
     * @param owner the class context that contains the method
     * @param modifiers the modifiers (i.e. access modifiers) for this method.
     * @param name the identifier of the method.
     * @param signature the signature of the method.
     * @throws IllegalArgumentException if method name is not a valid identifier (can be &lt;init&gt;), if type
     * signature is not for a method, or if there are some illegal modifiers.
     */
    MethodContext(ClassContext owner, int modifiers, String name, TypeSignature signature)
    {
        super(owner, modifiers, name, signature);
        if (signature.getSort() != TypeSort.METHOD)
            throw new IllegalArgumentException("Expected a METHOD type signature.");
        exceptions = new HashSet<>();
    }

    /**
     * Reads this method body. (Does not include annotations).
     * @param reader the method visitor that will read the current method body.
     */
    public void readBody(MethodVisitor reader)
    {
        if (body == null)
            reader.visitEnd();
        else
            body.accept(reader);
    }

    /**
     * Clears the current method body, and writes a new body for this method.
     * @return this is the visitor that will record the method body.
     */
    public MethodVisitor writeBody()
    {
        body = new MethodBody();
        return body;
    }

    /**
     * Reads the code body of this method context if any.
     * @param global the global code symbols for method handles.
     * @return the resulting code.
     */
    public String readCode(CodeSymbols global)
    {
        MethodBodyLoader loader = new MethodBodyLoader(global);
        body.accept(loader);
        return "# " + getOwner() + "." + name + getSignature() + "\n" + loader.toCode();
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

    /**
     * Adds an exception that this method can throw. For simplicity, this method DOES NOT thoroughly check whether if
     * this class context points to a valid exception class. It will only check whether if this is a class or not.
     * @param except the exception class to add.
     * @return true if this is added, false if not added.
     */
    public boolean addException(ClassContext except)
    {
        if (except.isInterface())
            throw new IllegalArgumentException("Expected a concrete class context.");
        return exceptions.add(except);
    }

    /**
     * Removes an exception that this method can throw.
     * @param except the exception class to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeException(ClassContext except)
    {
        return exceptions.remove(except);
    }

    public Set<ClassContext> getExceptions()
    {
        return new HashSet<>(exceptions);
    }

    /**
     * Removes all inner classes, if any. This will not refactor any changes to the inner class contextes. <em>It is
     * the responsibility of the caller to ensure that the correct values get pushed to the inner classes.</em>
     */
    public void removeAllExceptions()
    {
        exceptions.clear();
    }
}
