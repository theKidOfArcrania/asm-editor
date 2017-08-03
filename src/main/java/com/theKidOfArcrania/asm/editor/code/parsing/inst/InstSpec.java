package com.theKidOfArcrania.asm.editor.code.parsing.inst;

import com.theKidOfArcrania.asm.editor.code.parsing.*;
import org.objectweb.asm.MethodVisitor;

/**
 * An instruction specification representing a list of arguments and argument types for a particular instruction, and
 * also a way to write it into the respective MethodVisitor. This can apply to multiple instruction opcodes if all
 * the instructions have a similar instruction specification.
 *
 * @author Henry Wang
 */
public abstract class InstSpec
{

    private final ParamType[] params;

    /**
     * Constructs a new instruction specification with a specific parameter signature.
     * @param params the parameter signature for this spec.
     */
    InstSpec(ParamType... params)
    {
        this.params = params;
    }

    /**
     * Parses the instruction arguments from the current position
     * @param reader the token reader
     * @return an array of the parsed arguments.
     */
    public Argument[] parseInstArgs(CodeTokenReader reader)
    {
        boolean error = false;
        Argument[] args = new Argument[params.length];
        for (int i = 0; i < params.length; i++)
        {
            if (!reader.nextArgument())
                return null;
            if (!params[i].matches(reader))
            {
                reader.errorExpected(params[i].getName());
                error = true;
            }
            else if (params[i].checkToken(reader))
                args[i] = new Argument(reader, params[i]);
            else
                error = true;
        }

        return error ? null : args;
    }

    /**
     * Verifies that this instruction is valid. This will emit any errors to the token reader if necessary.  This is
     * called right after the parse-phase. By default, this does nothing.
     * @param logger the logger used to log any errors emitted.
     * @param inst the instruction that has been parsed.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean verifyParse(ErrorLogger logger, InstStatement inst)
    {
        return true;
    }

    /**
     * Verifies that the symbols needed are resolved. This is called after the entire code body is parsed, and when
     * external changes are made. By default, this does nothing.
     * @param logger the logger used to log any errors emitted.
     * @param inst the instruction that has been parsed.
     * @param resolved the list of resolved symbols.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
    {
        return true;
    }

    /**
     * Writes the instruction as prescribed by this particular instruction spec.
     * @param writer the visitor to "write" this instruction to
     * @param inst the instruction that has been parsed.
     * @param resolved the list of resolved symbols.
     * @throws IllegalArgumentException if the opcode or args are invalid values.
     */
    public abstract void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved);
}
