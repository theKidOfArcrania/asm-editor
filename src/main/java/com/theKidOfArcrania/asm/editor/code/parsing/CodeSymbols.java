package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.context.ClassContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;

import java.util.HashMap;

/**
 * Represents all the symbols of a particular piece of code.
 * @author Henry Wang
 */
public class CodeSymbols
{
    private final CodeSymbols parent;
    private final ClassContext thisCtx;
    private final HashMap<String, Label> labels;
    private final HashMap<String, Handle> handles;


    /**
     * Constructs a new code symbol table.
     * @param parent a parent code symbols to inherit from, or null if this is a global code symbol.
     * @param thisCtx the class context
     */
    public CodeSymbols(CodeSymbols parent, ClassContext thisCtx)
    {
        this.parent = parent;
        this.thisCtx = thisCtx;
        this.labels = new HashMap<>();
        this.handles = new HashMap<>();
    }

    /**
     * Add a resolved named label. This will not add a named-label if a label with that name already existed.
     * @param name the name of the label.
     * @param lbl the corresponding named label
     * @return true if a label was added, false if none was added.
     */
    public boolean addLabel(String name, Label lbl)
    {
        return labels.putIfAbsent(name, lbl) == null;
    }

    /**
     * Add another resolved method handle.
     * @param name the identifier name for the handle.
     * @param parsed the parsed method handle.
     * @return the new handle.
     */
    public boolean addHandle(String name, Handle parsed)
    {
        return handles.putIfAbsent(name, parsed) == null;
    }

    /**
     * Determines whether if this label name has been resolved.
     * @param name the label name
     * @return true if this exists, false if it doesn't.
     */
    public boolean containsLabel(String name)
    {
        return labels.containsKey(name);
    }

    /**
     * Determines whether if this handle name has been resolved.
     * @param name the handle name
     * @return true if this exists, false if it doesn't.
     */
    public boolean containsHandle(String name)
    {
        return handles.containsKey(name) || parent.containsHandle(name);
    }

    /**
     * Gets the named label with the particular name. This will not query any parent code symbols (since labels only
     * work with method local scopes.
     * @param name the name of the label.
     * @return the resolved named label.
     */
    public Label getLabel(String name)
    {
        return labels.get(name);
    }

    /**
     * Gets the named handle with the particular name
     * @param name the name of the handle.
     * @return the resolved named handle.
     */
    public Handle getHandle(String name)
    {
        Handle h = handles.get(name);
        if (h == null)
            return parent.getHandle(name);
        return h;
    }

    public ClassContext getThisContext()
    {
        return thisCtx;
    }

    /**
     * Removes a named label.
     * @param name the name of the label.
     * @return true if removed, false if nothing happened.
     */
    public boolean removeLabel(String name)
    {
        return labels.remove(name) != null;
    }

    /**
     * Removes a named handle. This will only explicitly remove the handle contained at this scope, NOT including the
     * parent's handles.
     * @param name the name of the handle.
     * @return true if removed, false if nothing happened.
     */
    public boolean removeHandle(String name)
    {
        return handles.remove(name) != null;
    }
}
