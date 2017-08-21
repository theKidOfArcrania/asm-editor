package com.theKidOfArcrania.asm.editor.code.parsing.directive;

import com.theKidOfArcrania.asm.editor.code.parsing.*;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstOpcodes;
import org.objectweb.asm.MethodVisitor;

import static com.theKidOfArcrania.asm.editor.code.parsing.Range.lineRange;
import static com.theKidOfArcrania.asm.editor.code.parsing.Range.tokenRange;
import static com.theKidOfArcrania.asm.editor.code.parsing.directive.DirType.findDirective;

/**
 * This statement represents a directive. Directives are prepended by a $.
 * @author Henry Wang
 */
public class DirStatement extends ArgumentedStatement
{
    public static DirStatement parseStatement(CodeParser parser, CodeTokenReader reader)
    {
        String dirName = reader.getToken().substring(1);
        DirType dirType = findDirective(dirName);
        if (dirType == null)
        {
            reader.error("Invalid directive name.", reader.getTokenPos());
            return null;
        }

        Argument[] args = dirType.parseArgs(reader);
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

        DirStatement dir = new DirStatement(reader, dirType, args);
        if (!dirType.verifyParse(reader.getDelegateLogger(), dir))
            return null;
        return dir;
    }

    private final DirType dirType;

    /**
     * Constructs a new directive statement.
     * @param reader reader associated with directive.
     * @param dirType the directive type that this statement derives from
     * @param args the list of arguments.
     */
    private DirStatement(CodeTokenReader reader, DirType dirType, Argument[] args)
    {
        super(reader, args);
        this.dirType = dirType;
    }



    @Override
    public boolean resolveSymbols()
    {
        return false;
    }

    @Override
    public void write(MethodVisitor writer)
    {

    }

    @Override
    public void reset()
    {

    }
}
