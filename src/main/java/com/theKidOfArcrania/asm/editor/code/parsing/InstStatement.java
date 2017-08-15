package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstOpcodes;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstSpec;
import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static com.theKidOfArcrania.asm.editor.code.parsing.Range.lineRange;
import static com.theKidOfArcrania.asm.editor.code.parsing.Range.tokenRange;

/**
 * This statement represents a single instruction.
 * @author Henry Wang
 */
public class InstStatement extends CodeStatement
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
        if (reader.nextToken(true))
        {
            Position start = reader.getTokenPos().getStart();
            reader.error("Expected end of statement.", tokenRange(start.getLineNumber(), end,
                    reader.getLine().length()));
            return null;
        }

        InstStatement inst = new InstStatement(reader, instSpec, opcode, args);
        if (!instSpec.verifyParse(inst.delegateLogger, inst))
            return null;
        return inst;
    }

    private final CodeTokenReader reader;
    private final ErrorLogger delegateLogger;

    private final InstSpec spec;
    private final InstOpcodes opcode;
    private final Argument[] args;

    private final Range lineRange;

    /**
     * Constructs a new instruction.
     * @param reader reader associated with instruction.
     * @param spec the instruction specification that this instruction derives from
     * @param opcode the opcode of this instruction
     * @param args the list of arguments.
     */
    private InstStatement(CodeTokenReader reader, InstSpec spec, InstOpcodes opcode, Argument[] args)
    {
        this.lineRange = lineRange(reader);
        this.reader = reader;
        this.spec = spec;
        this.opcode = opcode;
        this.args = args.clone();

        this.delegateLogger = new ErrorLogger()
        {
            @Override
            public void logError(String description, Range highlight)
            {
                reader.error(description, highlight);
            }

            @Override
            public void logWarning(String description, Range highlight)
            {
                reader.warning(description, highlight);
            }
        };
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

    /**
     * Obtains the associated type signature with the specific argument when converted into an object value.
     * @param ind the index of argument
     * @return the type signature.
     */
    public TypeSignature getArgTypeSig(int ind)
    {
        String sig;
        switch ((BasicParamType)getArgExactType(ind))
        {
            case INTEGER: sig = "I"; break;
            case LONG: sig = "J"; break;
            case FLOAT: sig = "F"; break;
            case DOUBLE: sig = "D"; break;
            case STRING: sig = "Ljava/lang/String;"; break;
            case METHOD_SIGNATURE: sig = "Ljava/lang/invoke/MethodType;"; break;
            case FIELD_SIGNATURE: sig = "Ljava/lang/Class;"; break;
            case METHOD_HANDLE: sig = "Ljava/lang/invoke/MethodHandle;"; break;
            default: throw new IllegalArgumentException();
        }
        return TypeSignature.parseTypeSig(sig);
    }

    /**
     * Obtains the argument descriptor at the particular index
     * @param ind the index of the argument.
     * @return the argument descriptor.
     */
    public Argument getArg(int ind)
    {
        return args[ind];
    }

    /**
     * Obtains the argument's exact type at the particular index.
     * @param ind the index of the argument.
     * @return the exact parameter type.
     * @see ParamType#getExactType(CodeTokenReader)
     * @see Argument#getExactType()
     */
    public ParamType getArgExactType(int ind)
    {
        return getArg(ind).getExactType();
    }

    /**
     * Obtains the parsed argument at the particular index.
     * @param ind the index of the argument.
     * @return a parsed object of the argument.
     */
    public Object getArgValue(int ind)
    {
        return getArg(ind).getValue();
    }

    /**
     * Obtains the parsed argument at the particular index, ensuring that the object is of a particular type.
     * @param ind the index of the argument.
     * @param type the required type
     * @param <T> generic that this object will automatically be cast to.
     * @return the casted object.
     * @throws IllegalArgumentException if object is not of correct type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getArgValue(int ind, Class<T> type)
    {
        Object o = getArgValue(ind);
        if (!type.isInstance(o))
            throw new IllegalArgumentException();
        return (T)o;
    }

    /**
     * Obtains the parsed argument as am integer value.
     * @param ind the index of the argument
     * @return the integer argument.
     * @throws IllegalArgumentException if the argument cannot be converted into an integer without lossy conversion
     */
    public int getIntArgValue(int ind)
    {
        return getArgValue(ind, Integer.class);
    }

    /**
     * Obtains the token range of the particular argument
     * @param ind the index of the argument
     * @return a valid range that describes the position of the argument token.
     */
    public Range getArgPos(int ind)
    {
        return getArg(ind).getTokenPos();
    }

    /**
     * @return the number of arguments.
     */
    public int getArgSize()
    {
        return args.length;
    }

    public Range getLineRange()
    {
        return lineRange;
    }

    @Override
    public boolean resolveSymbols()
    {
        return getSpec().verifySymbol(delegateLogger, this, reader.getResolvedSymbols());
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
