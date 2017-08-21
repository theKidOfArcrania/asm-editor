package com.theKidOfArcrania.asm.editor.test;

import com.theKidOfArcrania.asm.editor.code.parsing.*;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstOpcodes;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstStatement;
import com.theKidOfArcrania.asm.editor.context.ClassContext;
import com.theKidOfArcrania.asm.editor.context.MethodContext;
import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

@SuppressWarnings("JavaDoc")
@RunWith(Parameterized.class)
public class InstStatementTest
{
    private static ClassContext classContext;
    private static MethodContext mthContext;

    static
    {
        classContext = ClassContext.findContext("TestClass");
        if (classContext == null)
        {
            classContext = ClassContext.createContext("TestClass", false);
            mthContext = classContext.addMethod(Modifier.PUBLIC, "TestMethod", TypeSignature.parseTypeSig("()V"));
        }
        else
            mthContext = classContext.findMethod("TestMethod", TypeSignature.parseTypeSig("()V"), false);
    }

    @Parameterized.Parameters(name="Mode {3}: {1}")
    public static Object[][] params()
    {
        return new Object[][] {
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Math, E, @D", "", 0},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Math, <init>, @D", "char", 1},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Math, E, @(V)V", "expected", 1},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Mat, E, @D", "resolve", 2},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Math, E, @I", "type", 2},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Math, twoToTheDou, @D", "resolve", 2},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC java/lang/Math, twoToTheDoubleScaleUp, @D", "access", 2},
                {InstOpcodes.INST_GETSTATIC, "GETSTATIC com/theKidOfArcrania/asm/editor/test/TestAccess$Inner, foo, @I", "access", 2},
                {InstOpcodes.INST_IINC, "IINC 1, -2", "", 0},
                {InstOpcodes.INST_IINC, "IINC -1, 5", "expected", 1},
                {InstOpcodes.INST_BIPUSH, "BIPUSH -128", "", 0},
                {InstOpcodes.INST_BIPUSH, "BIPUSH -129", "value", 1},
                {InstOpcodes.INST_NEWARRAY, "NEWARRAY T_BOOLEAN", "", 0},
                {InstOpcodes.INST_NEWARRAY, "NEWARRAY T_NOTYPE", "expected", 1},
                {InstOpcodes.INST_INVOKEDYNAMIC, "INVOKEDYNAMIC foo, @(III)V, &bsm, 536, 5566L, .66F, .35, @(II)" +
                        "V, \"Something\", &handle1, @Ljava/lang/Class;", "", 0},
                {InstOpcodes.INST_GOTO, "GOTO label1", "", 0},
                {InstOpcodes.INST_LDC, "LDC \"ANOTHERSTRING\"", "", 0},
                {InstOpcodes.INST_LDC, "LDC &handle222", "resolve", 2},
                {InstOpcodes.INST_LOOKUPSWITCH, "LOOKUPSWITCH label1, 1, label1, 2, label1, 3, label1, 100, label1", "", 0},
                {InstOpcodes.INST_TABLESWITCH, "TABLESWITCH 1, 5, label1, label1, label1, label1, label1, label1", "", 0},
                {InstOpcodes.INST_NEW, "NEW java/lang/Math", "", 0},
                {InstOpcodes.INST_NEW, "NEW [Ljava/lang/Math;", "object", 2},
                {InstOpcodes.INST_INVOKESTATIC, "INVOKESTATIC java/lang/Math, sin, @(D)D", "", 0},
                {InstOpcodes.INST_INVOKEVIRTUAL, "INVOKEVIRTUAL java/lang/Object, <init>, @()V", "", 0},
                {InstOpcodes.INST_INVOKEVIRTUAL, "INVOKEVIRTUAL java/lang/Math, sinh, @V", "expected", 1},
                {InstOpcodes.INST_INVOKEVIRTUAL, "INVOKEVIRTUAL java/lang/Math, expm1, @(I)D", "resolve", 2},
                {InstOpcodes.INST_INVOKEVIRTUAL, "INVOKEVIRTUAL java/lang/Math, expm, @(D)D", "resolve", 2},
                {InstOpcodes.INST_INVOKESTATIC, "INVOKESTATIC java/lang/Math, powerOfTwoD, @(I)D", "access", 2},
                {InstOpcodes.INST_INVOKEVIRTUAL, "INVOKEVIRTUAL com/theKidOfArcrania/asm/editor/test/TestAccess$Inner, bar, @()V", "access", 2},
                {InstOpcodes.INST_ALOAD, "ALOAD 0", "", 0},
                {InstOpcodes.INST_ALOAD, "ALOAD -1", "expected", 1},
                {InstOpcodes.INST_MULTIANEWARRAY, "MULTIANEWARRAY @[[Ljava/lang/Object;, 2", "", 0},
                {InstOpcodes.INST_MULTIANEWARRAY, "MULTIANEWARRAY @[[Ljava/lang/Object;, 3", "expected", 1},
                {InstOpcodes.INST_IOR, "IOR", "", 0},
                {InstOpcodes.INST_IOR, "IOR blah", "end of statement", 1}
        };
    }

    private CodeTokenReader reader;
    private InstStatement parsed;
    private int curMode;

    private boolean found = false;

    private final String code;
    private final InstOpcodes opcode;
    private final int errMode;
    private final String search;

    public InstStatementTest(InstOpcodes opcode, String code, String search, int errMode)
    {
        this.code = code;
        this.opcode = opcode;
        this.errMode = errMode;
        this.search = search.toLowerCase();
    }

    @Before
    public void setup()
    {
        CodeSymbols global = new CodeSymbols(null, classContext);
        global.addHandle("handle1", new Handle(Opcodes.H_GETFIELD, "owner", "name", "()V", true));
        global.addHandle("bsm", new Handle(Opcodes.H_INVOKESTATIC, "owner", "name2",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;JJ" +
                        "Ljava/lang/Float;Ljava/lang/Object;Ljava/lang/invoke/MethodType;Ljava/lang/String;" +
                        "Ljava/lang/invoke/MethodHandle;Ljava/lang/Class;)Ljava/lang/invoke/CallSite;", true));

        reader = new CodeTokenReader(global, mthContext, code);
        reader.getResolvedSymbols().addLabel("label1", new Label());
        reader.nextLine();
        reader.addErrorLogger(new ErrorLogger()
        {
            @Override
            public void logError(String description, Range highlight)
            {
                assertEquals(1, highlight.getStart().getLineNumber());
                assertEquals(1, highlight.getEnd().getLineNumber());
                if (curMode != errMode || !description.toLowerCase().contains(search))
                    fail(description);
                else
                    found = true;
            }

            @Override
            public void logWarning(String description, Range highlight)
            {
                fail(description);
            }
        });
    }

    @Test
    public void testParse() throws Exception
    {
        assertLogError(1, this::parseStatement);
        if (parsed != null)
        {
            assertEquals(opcode.getNumber(), parsed.getOpcodeNum());
            assertEquals(opcode.getInstSpec(), parsed.getSpec());
        }
    }

    @Test
    public void testResolved() throws Exception
    {
        if (errMode == 2 || errMode == 0)
        {
            this.parseStatement();
            assertLogError(2, parsed::resolveSymbols);
        }
    }

    private void parseStatement()
    {
        reader.nextToken();
        parsed = InstStatement.parseStatement(reader);
    }

    private void assertLogError(int checkMode, Runnable action)
    {
        curMode = checkMode;
        action.run();
        if (!found && curMode == errMode)
            fail("Did not log error with keyword: '" + search + "'");
    }
}
