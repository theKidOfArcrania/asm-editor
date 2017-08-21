package com.theKidOfArcrania.asm.editor.code.parsing.inst;

import com.theKidOfArcrania.asm.editor.code.parsing.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static com.theKidOfArcrania.asm.editor.code.parsing.Range.tokenRange;

/**
 * This statement represents a single instruction.
 * @author Henry Wang
 */
public class InstStatement extends ArgumentedStatement
{
    /**
     * Reads in an instruction statement and parses it. The associated token-reader should be primed to the first token
     * (the instruction word) of that line. If an error occurred while parsing this instruction, this will return null.
     * @param reader the token reader.
     * @return the instruction parsed, or null.
     */
    public static InstStatement parseStatement(CodeTokenReader reader)
    {
        String instName = reader.getToken();
        InstOpcodes opcode = InstOpcodes.fetchOpcode(instName);
        if (opcode == null)
        {
            reader.error("Invalid instruction name.", reader.getTokenPos());
            return null;
        }

        InstSpec instSpec = opcode.getInstSpec();
        Argument[] args = instSpec.parseInstArgs(reader);
        if (args == null)
            return null;

        int end = reader.getTokenEndIndex();
        if (reader.nextToken(false))
        {
            Position start = reader.getTokenPos().getStart();
            reader.error("Expected end of statement.", tokenRange(start.getLineNumber(), end,
                    reader.getLine().length()));
            return null;
        }

        InstStatement inst = new InstStatement(reader, instSpec, opcode, args);
        if (!instSpec.verifyParse(reader.getDelegateLogger(), inst))
            return null;
        return inst;
    }

    private final InstSpec spec;
    private final InstOpcodes opcode;

    /**
     * Constructs a new instruction.
     * @param reader reader associated with instruction.
     * @param spec the instruction specification that this instruction derives from
     * @param opcode the opcode of this instruction
     * @param args the list of arguments.
     */
    private InstStatement(CodeTokenReader reader, InstSpec spec, InstOpcodes opcode, Argument[] args)
    {
        super(reader, args);
        this.spec = spec;
        this.opcode = opcode;
    }

    public CodeSymbols getResolvedSymbols()
    {
        return reader.getResolvedSymbols();
    }

    public InstSpec getSpec()
    {
        return spec;
    }

    public InstOpcodes getOpcode()
    {
        return opcode;
    }

    public int getOpcodeNum()
    {
        return opcode.getNumber();
    }

    @Override
    public boolean resolveSymbols()
    {
        return getSpec().verifySymbol(reader.getDelegateLogger(), this, reader.getResolvedSymbols());
    }

    @Override
    public void write(MethodVisitor writer)
    {
        CodeSymbols symbols = reader.getResolvedSymbols();
        Label lbl = symbols.findStatementLabel(this);
        if (symbols.isAnonymousLabel(lbl))
            writer.visitLabel(lbl);
        getSpec().write(writer, this, symbols);
    }

    @Override
    public void reset()
    {
        reader.getResolvedSymbols().removeMappedStatement(this);
    }
}
