package com.theKidOfArcrania.asm.editor.test;

import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import com.theKidOfArcrania.asm.editor.context.TypeSort;
import org.junit.Test;

import static org.junit.Assert.*;
import static com.theKidOfArcrania.asm.editor.context.TypeSort.*;

@SuppressWarnings("JavaDoc")
public class TypeSignatureTest
{
    private static final String[] PRIM_NAMES = {"B", "C", "D", "F", "I", "J", "S", "V", "Z"};
    private static final TypeSort[] PRIM_TYPES = {BYTE, CHAR, DOUBLE, FLOAT, INTEGER, LONG, SHORT, VOID, BOOLEAN};

    private static TypeSignature checkParse(String descript)
    {
        TypeSignature sig = TypeSignature.parseTypeSig(descript);
        assertNotNull("Cannot parse '" + descript + "'.", sig);
        return sig;
    }

    @Test
    public void testPrimitiveTypes()
    {
        for (int i = 0; i < PRIM_NAMES.length; i++)
        {
            TypeSignature sig = checkParse(PRIM_NAMES[i]);
            assertEquals("Not correct primitive value.", sig.getSort(), PRIM_TYPES[i]);
        }
    }

    @Test
    public void testClassTypes()
    {
        TypeSignature sig = checkParse("Ljava/lang/Class;");
        assertNull("Unresolved symbols.", sig.getUnresolvedClassSymbols());

        sig = checkParse("Lcom/theKidOfArcrania/asm/editor/test/TypeSignatureTest;");
        assertNull("Unresolved symbols.", sig.getUnresolvedClassSymbols());

        sig = checkParse("Lno/such/class;");
        assertEquals("no/such/class", sig.getUnresolvedClassSymbols());
        assertEquals("no/such/class", sig.getClassDescriptor());
    }

    @Test
    public void testArrayTypes()
    {
        TypeSignature sig = checkParse("[[[B");
        assertEquals(sig.getDimensions(), 3);
        assertEquals(TypeSignature.BYTE_TYPE, sig.getComponentType());
    }

    @Test
    public void testMethodNoParam() throws Exception
    {
        assertEquals(TypeSort.METHOD, checkParse("()V").getSort());
    }

    @Test
    public void testMethodObjParam() throws Exception
    {
        assertEquals(TypeSort.METHOD, checkParse("(Ljava/lang/Object;BBB)V").getSort());
    }

    @Test
    public void testBadSigs()
    {
        String[] badSigs = {"[[[[", "Y", "[[[[[Y", ";", "L", "Ljfve", "BB", "(BB(II)V)V", "(BB)", "(VV)V",
                "Ljava.lang.Object;", "[[[BB"};
        for (String s : badSigs)
        {
            TypeSignature sig = TypeSignature.parseTypeSig(s);
            assertNull("Faulty signature is valid: '" + s + "'", sig);
        }
    }
}