package com.theKidOfArcrania.asm.editor.code.parsing;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a label statement.
 * @author Henry Wang
 */
public class LabelStatement extends CodeStatement
{
    /**
     * Reads in an label statement and parses it. The associated token-reader should be primed to the first token
     * (the label word) of that line.
     * @param reader the token reader.
     * @return the parsed label.
     * @throws IllegalStateException if this parse statement is called when the reader is not primed correctly to a
     * label statement.
     */
    public static LabelStatement parseStatement(CodeTokenReader reader)
    {
        if (reader.hasTokenError() || reader.getTokenType() != TokenType.LABEL)
            throw new IllegalStateException();
        return new LabelStatement(reader, reader.getToken());
    }

    private CodeTokenReader reader;
    private String name;
    private Label symbol;

    /**
     * Constructs a new label from the name and the associated token reader.
     * @param reader the token reader.
     * @param name the name of the label.
     */
    private LabelStatement(CodeTokenReader reader, String name)
    {
        this.reader = reader;
        this.name = name;
        symbol = new Label();
    }

    @Override
    public boolean resolveSymbols()
    {
        CodeSymbols resolved = reader.getResolvedSymbols();
        if (resolved.containsLabel(name) && resolved.getLabel(name) != symbol)
        {
            reader.error("Label '" + name + "' already used.", reader.getTokenPos());
            return false;
        }
        resolved.addLabel(name, symbol);
        return true;
    }

    @Override
    public void write(MethodVisitor writer)
    {
        writer.visitLabel(symbol);
    }

    @Override
    public void reset()
    {
        CodeSymbols resolved = reader.getResolvedSymbols();
        if (resolved.getLabel(name) == symbol)
            resolved.removeLabel(name);
    }
}
