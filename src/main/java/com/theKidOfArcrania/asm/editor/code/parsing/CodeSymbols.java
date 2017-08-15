package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.context.ClassContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;

import java.util.HashMap;
import java.util.Objects;

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
    private final HashMap<InstStatement, Label> mappedLabels;
    private final HashMap<Label, InstStatement> mappedStatements;

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
        this.mappedLabels = new HashMap<>();
        this.mappedStatements = new HashMap<>();
    }

    /**
     * Add a resolved named label. This will not add a named-label if a label with that name already existed.
     * @param name the name of the label.
     * @param lbl the corresponding named label
     * @return true if a label was added, false if none was added.
     */
    public boolean addLabel(String name, Label lbl)
    {
        Objects.requireNonNull(lbl);
        Objects.requireNonNull(name);
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
        Objects.requireNonNull(name);
        Objects.requireNonNull(parsed);
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
        return handles.containsKey(name) || parent != null && parent.containsHandle(name);
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
        if (h == null && parent != null)
            return parent.getHandle(name);
        return h;
    }

    public ClassContext getThisContext()
    {
        return thisCtx;
    }

    /**
     * Removes a named label. This also removes its associated mapping to the instruction statement (if any).
     * @param name the name of the label.
     * @return true if removed, false if nothing happened.
     */
    public boolean removeLabel(String name)
    {
        Label removed = labels.remove(name);
        if (mappedStatements.containsKey(removed))
            mappedLabels.remove(mappedStatements.remove(removed));
        return removed != null;
    }

    /**
     * Determines whether if this label is anonymous. This label is anonymous if it is not in the named labels list.
     * This is typically the case when {@link #findStatementLabel(InstStatement)} is called with no label attached to
     * statement. If this label is not mapped to any statement, this will also return <code>false</code>,
     *
     * @param lbl the label to check.
     * @return true if anonymous, false if not.
     */
    public boolean isAnonymousLabel(Label lbl)
    {
       return mappedStatements.containsKey(lbl) && !labels.containsValue(lbl);
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

    /**
     * Maps a statement to a label. If the label is currently mapped to another statement, that mapping will be removed.
     * @param lbl the label to associate with the statement.
     * @param inst the instruction statement.
     */
    public void mapStatement(Label lbl, InstStatement inst)
    {
        InstStatement prev = mappedStatements.put(lbl, inst);
        if (prev != null)
            mappedLabels.remove(prev);
    }

    /**
     * Finds the associated label with this statement if any. If not found, this will create a new anonymous
     * (unnamed) label.
     * @param inst the instruction statement to loop up.
     * @return the associated label.
     */
    public Label findStatementLabel(InstStatement inst)
    {
        return mappedLabels.computeIfAbsent(inst, key -> new Label());
    }

    /**
     * Finds a statement from it's associated label. If no statement is associated with this label, this will return
     * <code>null</code>.
     * @param lbl the label to search up.
     * @return the associated statement, if any.
     */
    public InstStatement findStatement(Label lbl)
    {
        return mappedStatements.get(lbl);
    }

    /**
     * Removes a currently mapped instruction statement
     * @param inst the instruction statement to remove.
     */
    public void removeMappedStatement(InstStatement inst)
    {
        Label removed = mappedLabels.remove(inst);
        if (removed != null)
            mappedStatements.remove(removed);
    }
}
