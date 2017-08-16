package com.theKidOfArcrania.asm.editor.test;

import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameElement;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameException;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameType;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.StackMapFrame;
import com.theKidOfArcrania.asm.editor.code.parsing.frame.StackMapFrame.TestInterface;
import com.theKidOfArcrania.asm.editor.context.ClassContext;
import com.theKidOfArcrania.asm.editor.context.MethodContext;
import com.theKidOfArcrania.asm.editor.context.TypeSort;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameType.*;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.parseTypeSig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("JavaDoc")
public class StackMapFrameTest
{
    private static ClassContext classContext;
    private static MethodContext mthContext;
    static
    {
        classContext = ClassContext.findContext("TestClass");
        if (classContext == null)
        {
            classContext = ClassContext.createContext("TestClass", false);
            mthContext = classContext.addMethod(Modifier.PUBLIC, "TestMethod", parseTypeSig("()V"));
        }
        else
            mthContext = classContext.findMethod("TestMethod", parseTypeSig("()V"), false);
    }

    private TestInterface tester;
    private CodeSymbols globalSymbols;

    @Before
    public void setUp() throws Exception
    {
        StackMapFrame frame = new StackMapFrame(mthContext);
        Constructor testerCreater = TestInterface.class.getDeclaredConstructor(StackMapFrame.class);
        testerCreater.setAccessible(true);
        tester = (TestInterface)testerCreater.newInstance(frame);
        globalSymbols = new CodeSymbols(null, classContext);
    }

    @Test
    public void testVars() throws Exception
    {
        int maxVar = tester.getMaxVars();
        assertEquals("Not correct index", maxVar, tester.pushVar());
        assertEquals("Not correct max vars", maxVar + 1, tester.getMaxVars());

        tester.allocateVar(4, new FrameElement(LONG));
        assertEquals("Not correct max vars", 6, tester.getMaxVars());

        assertEquals("Not correct index", 4, tester.popVar());
    }

    @Test
    public void testInvokeOps() throws Exception
    {
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.pushOp(new FrameElement(NULL));
        tester.invokeOps(parseTypeSig("Ljava/lang/Class;"), parseTypeSig("I"), parseTypeSig("J"), parseTypeSig
                ("Ljava/lang/Object;"));
        assertEquals("java/lang/Class", tester.popOp(OBJECT).getRefSig().getClassDescriptor());

        tester.invokeOps(parseTypeSig("J"));
        tester.popOp(LONG);
    }

    @Test
    public void testInvokeInitOps() throws Exception
    {
        tester.allocateVar(0, new FrameElement(UNINITIALIZED_THIS));
        tester.pushOp(new FrameElement(UNINITIALIZED_THIS));
        tester.pushOp(new FrameElement(FLOAT));
        tester.pushOp(new FrameElement(UNINITIALIZED_THIS));
        tester.pushOp(new FrameElement(INTEGER));
        tester.invokeInitOps(globalSymbols, classContext, parseTypeSig("I"));

        tester.popOp(FLOAT);
        assertEquals(classContext.toString(), tester.popOp(OBJECT).getRefSig().getClassDescriptor());
        assertEquals(classContext.toString(), tester.getVar(0).getRefSig().getClassDescriptor());
    }

    @Test(expected=FrameException.class)
    public void testPopLongFailed() throws Exception
    {
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(LONG));
        tester.popOp(LONG);
    }

    @Test(expected=FrameException.class)
    public void testPopClobber() throws Exception
    {
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.popOp(TOP);
    }

    @Test
    public void testPushPopFail() throws Exception
    {
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(INTEGER));

        boolean failed = false;
        try
        {
            tester.pushPopOps(INTEGER, LONG);
        }
        catch (FrameException e)
        {
            failed = true;
        }

        assertTrue("Did not trigger frame exception.", failed);
        tester.pushPopOps(null, INTEGER, INTEGER);
    }

    @Test
    public void testPushElePopOps() throws Exception
    {
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.pushOp(new FrameElement(INTEGER));
        FrameElement[] eles = tester.pushElePopOps(new FrameElement(UNINITIALIZED_THIS), LONG, INTEGER);

        assertEquals(LONG, eles[0].getType());
        assertEquals(INTEGER, eles[1].getType());

        assertEquals(UNINITIALIZED_THIS, tester.popOp(UNINITIALIZED_THIS).getType());
    }

    @Test
    public void testPushPopOps() throws Exception
    {
        tester.pushOp(new FrameElement(TOP));
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.pushOp(new FrameElement(INTEGER));
        FrameElement[] eles = tester.pushPopOps(UNINITIALIZED_THIS, LONG, INTEGER);

        assertEquals(LONG, eles[0].getType());
        assertEquals(INTEGER, eles[1].getType());

        assertEquals(UNINITIALIZED_THIS, tester.pushPopOps(OBJECT, UNINITIALIZED_THIS)[0].getType());

        assertEquals(TOP, tester.popOp(TOP).getType());
    }

    @Test
    public void testPopNoCheck() throws Exception
    {
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.pushOp(new FrameElement(DOUBLE));
        tester.pushOp(new FrameElement(TOP, true));

        FrameElement invalid = tester.popNoCheck(1)[0];
        assertEquals(TOP, invalid.getType());
        assertTrue(invalid.isBlock());

        FrameElement[] frames = tester.popNoCheck(4);
        assertEquals(TOP, frames[0].getType());

        assertEquals(LONG, frames[1].getType());
        assertEquals(TOP, frames[2].getType());
        assertEquals(DOUBLE, frames[3].getType());
    }

    @Test(expected = FrameException.class)
    public void testCheckStackUnderflow() throws Exception
    {
        tester.checkStackUnderflow(1);
    }

    @Test
    public void testDup() throws Exception
    {
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.pushOp(new FrameElement(UNINITIALIZED_THIS));
        tester.dup(1, 2);
        tester.popOp(UNINITIALIZED_THIS);
        tester.dup(2, 1);
        tester.popOp(LONG);
        tester.popOp(UNINITIALIZED_THIS);
        tester.popOp(LONG);
        tester.dup(1, 0);
        tester.popOp(INTEGER);
        tester.popOp(INTEGER);
    }

    @Test
    public void testPushOp() throws Exception
    {
        int oldMax = tester.getMaxStack();
        tester.pushOp(new FrameElement(FrameType.TOP));
        assertEquals(oldMax + 1, tester.getMaxStack());
    }

    @Test
    public void testCheckVarType() throws Exception
    {
        tester.allocateVar(0, new FrameElement(LONG));
        tester.allocateVar( 2, new FrameElement(INTEGER));
        tester.allocateVar(3, new FrameElement(NULL));
        tester.checkVarType(0, LONG);
        tester.checkVarType(2, INTEGER);
        tester.checkVarType(3, OBJECT);

        assertTrue("Not blocked", tester.getVar(1).isBlock());
    }

    @Test(expected = FrameException.class)
    public void testCheckVarFail() throws Exception
    {
        tester.allocateVar(0, new FrameElement(LONG));
        tester.allocateVar(1, new FrameElement(LONG));
        tester.checkVarType(0, LONG);
    }

    @Test
    public void testCheckArrayLoad() throws Exception
    {
        tester.pushOp(new FrameElement(OBJECT, parseTypeSig("[[J")));
        tester.pushOp(new FrameElement(INTEGER));
        tester.checkArrayLoad(TypeSort.OBJECT);
        tester.pushOp(new FrameElement(INTEGER));
        tester.checkArrayLoad(TypeSort.LONG);
        tester.popOp(LONG);
    }

    @Test
    public void testCheckArrayStore() throws Exception
    {
        tester.pushOp(new FrameElement(OBJECT, parseTypeSig("[[J")));
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(NULL));
        tester.checkArrayStore(TypeSort.OBJECT);
        tester.pushOp(new FrameElement(OBJECT, parseTypeSig("[[J")));
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(OBJECT, parseTypeSig("[J")));
        tester.checkArrayStore(TypeSort.OBJECT);
        tester.pushOp(new FrameElement(OBJECT, parseTypeSig("[[J")));
        tester.pushOp(new FrameElement(INTEGER));
        tester.checkArrayLoad(TypeSort.OBJECT);
        tester.pushOp(new FrameElement(INTEGER));
        tester.pushOp(new FrameElement(LONG));
        tester.pushOp(new FrameElement(TOP, true));
        tester.checkArrayStore(TypeSort.LONG);
    }

}