package com.theKidOfArcrania.asm.editor.code.parsing.directive;


import com.theKidOfArcrania.asm.editor.code.parsing.*;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameElement;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameType;

import java.util.HashMap;

import static com.theKidOfArcrania.asm.editor.code.parsing.BasicParamType.FRAME_ELEMENTS;
import static org.objectweb.asm.Opcodes.*;

/**
 * This represents all the valid directives that can be written.
 * @author Henry Wang
 */
public enum DirType
{
    FRAME_FULL(F_FULL, FRAME_ELEMENTS, FRAME_ELEMENTS)
    {
        @Override
        public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved)
        {
            return resolveFrameElements(logger, dir, resolved, 0) &&
                    resolveFrameElements(logger, dir, resolved, 1);
        }
    },
    FRAME_APPEND(F_APPEND, FRAME_ELEMENTS)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, DirStatement dir)
        {
            int add = dir.getArgValue(0, FrameElement[].class).length;
            if (add == 0)
            {
                logger.logError("Cannot append no frame elements, use FRAME_SAME.", dir.getArgPos(0));
                return false;
            }
            if (add > 3)
            {
                logger.logError("Cannot append more than three elements, use FRAME_FULL.", dir.getArgPos(0));
                return false;
            }
            return true;
        }

        @Override
        public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved)
        {
            return resolveFrameElements(logger, dir, resolved, 0);
        }
    },
    FRAME_CHOP(F_CHOP, BasicParamType.INTEGER)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, DirStatement dir)
        {
            int chop = dir.getIntArgValue(0);
            if (chop == 0)
            {
                logger.logError("Cannot chop no frame elements, use FRAME_SAME.", dir.getArgPos(0));
                return false;
            }
            if (chop > 3)
            {
                logger.logError("Cannot chop more than three elements, use FRAME_FULL.", dir.getArgPos(0));
                return false;
            }
            return true;
        }
    },
    FRAME_SAME(F_SAME), FRAME_SAME1(F_SAME1, FRAME_ELEMENTS)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, DirStatement dir)
        {
            if (dir.getArgValue(0, FrameElement[].class).length != 1)
            {
                logger.logError("Must only have one frame element in stack.", dir.getArgPos(0));
                return false;
            }
            return true;
        }

        @Override
        public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved)
        {
            return resolveFrameElements(logger, dir, resolved, 0);
        }
    };

    //TODO: import directives.

    private static HashMap<String, DirType> lookup;

    static {
        lookup = new HashMap<>();
        for (DirType type : values())
            lookup.put(type.name(), type);
    }

    /**
     * Locates the directive with the particular name.
     * @param name the name of directive (without dollar-sign prepend).
     * @return the directive type if found.
     */
    public static DirType findDirective(String name)
    {
        return lookup.get(name.toUpperCase());
    }

    private final int opcode;
    private final ParamType[] params;

    /**
     * Verifies that the symbols referred to by the frame elements can be resolved. This will log any errors found.
     * @param logger the error logger.
     * @param dir the directive statement.
     * @param resolved the resolved code symbols.
     * @param arg argument index of frame elements.
     * @return true if resolvable, false if an error occurred.
     */
    private static boolean resolveFrameElements(ErrorLogger logger, DirStatement dir, CodeSymbols resolved, int arg)
    {
        FrameElement[] eles = dir.getArgValue(0, FrameElement[].class);


        String err = null;
        for (FrameElement ele : eles)
        {
            String unresolved = null;
            if (ele.getType() == FrameType.OBJECT)
                unresolved = ele.getRefSig().getUnresolvedClassSymbols();
            else if (ele.getType() == FrameType.UNINITIALIZED)
            {
                if (!resolved.containsLabel(ele.getLabelName()))
                    unresolved = "'" + ele.getLabelName() + "'";
            }
            if (unresolved != null)
                err = err == null ? unresolved : err + ", " + unresolved;
        }
        if (err != null)
        {
            logger.logError("Cannot resolve symbol(s): " + err + ".", dir.getArgPos(arg));
            return false;
        }
        return true;
    }

    /**
     * Creates a new directive type with the associated opcode.
     * @param opcode the opcode associated with directive, -1 if none exists.
     * @param params the parameter types with this directive
     */
    DirType(int opcode, ParamType... params)
    {
        this.opcode = opcode;
        this.params = params;
    }

    public int getOpcode()
    {
        return opcode;
    }

    /**
     * Parses all the arguments of this directive type.
     * @param reader the token reader to parse from.
     * @return a list of parsed arguments
     */
    public Argument[] parseArgs(CodeTokenReader reader)
    {
        return reader.parseArguments(params);
    }

    /**
     * Verifies that this directive is valid. This will emit any errors to the logger if necessary.  This is
     * called right after the parse-phase. By default, this does nothing.
     * @param logger the logger used to log any errors emitted.
     * @param dir the directive that has been parsed.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean verifyParse(ErrorLogger logger, DirStatement dir)
    {
        return true;
    }

    /**
     * Verifies that the symbols needed are resolved. This is called after the entire code body is parsed, and when
     * external changes are made. By default, this does nothing.
     * @param logger the logger used to log any errors emitted.
     * @param dir the directive that has been parsed.
     * @param resolved the list of resolved symbols.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved)
    {
        return true;
    }

    public void writeFrame
}
