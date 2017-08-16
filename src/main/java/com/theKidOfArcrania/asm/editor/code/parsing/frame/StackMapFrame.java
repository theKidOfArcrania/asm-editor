package com.theKidOfArcrania.asm.editor.code.parsing.frame;

import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import com.theKidOfArcrania.asm.editor.code.parsing.InstStatement;
import com.theKidOfArcrania.asm.editor.context.ClassContext;
import com.theKidOfArcrania.asm.editor.context.MethodContext;
import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import com.theKidOfArcrania.asm.editor.context.TypeSort;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static com.theKidOfArcrania.asm.editor.context.ClassContext.findContext;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.VOID_TYPE;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.isAssignable;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.parseTypeSig;

/**
 * Represents a stack map frame element of the java bytecode. A stack map frame provides a "snapshot" of the variables and
 * current operand values on the value stack at a point in execution.
 * @author Henry Wang
 */
public class StackMapFrame
{

    /**
     * Interface used to make all the private methods public for testing. Remove this in production.
     */
    public static class TestInterface
    {
        private final StackMapFrame tester;

        /**
         * Constructs a new test interface. This is private since we will use reflection to access constructor anyways.
         * @param tester the associated stack map frame object to test.
         */
        private TestInterface(StackMapFrame tester)
        {
            this.tester = tester;
        }

        public int getMaxStack()
        {
            return tester.maxStack;
        }

        public int getMaxVars()
        {
            return tester.maxVars;
        }

        /**
         * Gets the local variable at the index
         * @param ind the index of local variable
         * @return the frame corresponding with local variable.
         */
        public FrameElement getVar(int ind)
        {
            return tester.localVariables.get(ind);
        }

        /**
         * Adds another local variable to the end of the stack.
         * @return the index of the variable just pushed.
         */
        public int pushVar()
        {
            return tester.pushVar();
        }

        /**
         * Removes the last local variables from the stack. If the last local variable is a double-slot, this will remove
         * two slots.
         * @return the index of the last variable just removed.
         */
        public int popVar()
        {
            return tester.popVar();
        }

        /**
         * Executes the specified instruction statement.
         * @param inst the instruction to execute.
         * @throws FrameException if the type-check fails.
         */
        public void execute(InstStatement inst) throws FrameException
        {
            tester.execute(inst);
        }

        /**
         * Inserts the implicit object parameter at the beginning of the parameter list.
         * @param objref the object reference
         * @param params the explicit parameter list.
         * @return the full parameter list.
         */
        public TypeSignature[] insertObjParam(TypeSignature objref, TypeSignature[] params)
        {
            return tester.insertObjParam(objref, params);
        }

        /**
         * Parses the type signature from an instruction
         * @param inst the instruction to parse from.
         * @param arg the argument number of the instruction
         * @return the parsed type signature.
         * @throws IllegalArgumentException if unable to parse type signature.
         */
        public TypeSignature parseSig(InstStatement inst, int arg)
        {
            return tester.parseSig(inst, arg);
        }

        /**
         * Executes an invocation with a particular method type signature (a return value and a series of parameters)
         * @param ret the return value
         * @param params the series of parameters.
         * @throws FrameException if the type-check fails.
         * @return the actual parameters
         */
        public FrameElement[] invokeOps(TypeSignature ret, TypeSignature... params) throws FrameException
        {
            return tester.invokeOps(ret, params);
        }

        /**
         * Executes an &lt;init&gt; invocation on the operand stack. This will also initialize any references to the
         * uninitialized object.
         * @param resolved the code symbols needed to resolve the labels.
         * @param initCtx the class context of whose &lt;init&gt; method is being called.
         * @param params the series of parameters (excluding the object reference parameter).
         * @throws FrameException if the type-check fails.
         */
        public void invokeInitOps(CodeSymbols resolved, ClassContext initCtx, TypeSignature... params) throws FrameException
        {
            tester.invokeInitOps(resolved, initCtx, params);
        }

        /**
         * Finds any matching uninitialized frame elements from the list and initializes them.
         * @param eles the frame element list
         * @param resolved the resolved code symbols
         * @param uninit the uninitialized frame element to look for
         * @param init the initialized frame element to replace with
         */
        public void init(List<FrameElement> eles, CodeSymbols resolved, FrameElement uninit, FrameElement init)
        {
            tester.init(eles, resolved, uninit, init);
        }

        /**
         * Pops a few operands, then pushes an operand. Note that the order of these operands is in reverse order,
         * i.e. the last element refers to the first element popped/pushed.
         * @param push the element to be pushed.
         * @param pop the expected frame types to be popped.
         * @return the frame elements popped.
         * @throws FrameException if the type-check fails.
         */
        public FrameElement[] pushElePopOps(FrameElement push, FrameType... pop) throws FrameException
        {
            return tester.pushElePopOps(push, pop);
        }

        /**
         * Pops a few operands, then pushes an operand. Note that the order of these operands is in reverse order,
         * i.e. the last element refers to the first element popped/pushed.
         * @param push the element type to be pushed.
         * @param pop the expected frame types to be popped.
         * @return the frame elements popped.
         * @throws FrameException if the type-check fails.
         */
        public FrameElement[] pushPopOps(FrameType push, FrameType... pop) throws FrameException
        {
            return tester.pushPopOps(push, pop);
        }

        /**
         * Pops the number of frame values from the operation stack without any type verification. Note that this will
         * return the elements in REVERSE order, i.e. the first element of this array is the last element popped. If
         * there are less than enough operands left on the operand stack, this will place in TOP frames
         * @param popCount the number of elements to pop.
         * @return the popped elements in REVERSE order.
         */
        public FrameElement[] popNoCheck(int popCount)
        {
            return tester.popNoCheck(popCount);
        }

        /**
         * Checks that the stack has the specified number of elements remaining.
         * @param size the size of the stack to guarantee
         * @throws FrameException if a stack underflow occurs.
         */
        public void checkStackUnderflow(int size) throws FrameException
        {
            tester.checkStackUnderflow(size);
        }

        /**
         * This will pop an operand with the type specified. This method guarantees that the frame element WILL BE
         * popped from the operand stack regardless of any errors that might occur.
         * @param type the type required for frame element.
         * @return the frame element guaranteed to be of the right type.
         * @throws FrameException if the type-check fails.
         */
        public FrameElement popOp(FrameType type) throws FrameException
        {
            return tester.popOp(type);
        }

        /**
         * Executes a duplication of an element. If there is enough elements in the stack, this guarantees that the
         * operation will be performed (even in the event of some errors).
         * @param dupSize the number of elements to duplicate.
         * @param spacing the number of elements to separate the duplicate from the original.
         * @throws FrameException if doing so would clobber 2-block elements or if a stack underflow would occur.
         */
        public void dup(int dupSize, int spacing) throws FrameException
        {
            tester.dup(dupSize, spacing);
        }

        /**
         * This will push the specified frame element to the top of the operand stack.
         * @param ele the frame element to add.
         */
        public void pushOp(FrameElement ele)
        {
            tester.pushOp(ele);
        }

        /**
         * Checks that the top operand is of the correct type. For special 2 size operands, the top-most MUST be of type
         * TOP and the second top-most MUST be of the specified type. If any conditions fail, an exception will be thrown.
         * @param type the frame type required.
         * @throws FrameException if the type-check fails.
         */
        public void checkOpType(FrameType type) throws FrameException
        {
            tester.checkOpType(type);
        }

        /**
         * Allocates the space for this new variable.
         * @param index the index of the variable.
         * @param ele the frame element to allocate with.
         */
        public void allocateVar(int index, FrameElement ele)
        {
            tester.allocateVar(index, ele);
        }

        /**
         * Checks that the variable specified is the right type. For special 2 size operands, the next slot MUST be of type
         * TOP and the index specified MUST be of the specified type. The specified type must be a queryable type, i.e.
         * has a value that has a meaningful value, not NULL or TOP. If any conditions fail, an exception will be thrown.
         * @param index the index of the variable.
         * @param type the frame type required.
         * @throws FrameException if the type-check fails.
         * @throws IllegalArgumentException if the type passed is not a valid queryable type.
         * @return the frame element representing the variable.
         */
        public FrameElement checkVarType(int index, FrameType type) throws FrameException
        {
            return tester.checkVarType(index, type);
        }

        /**
         * Verifies an array load.
         * @param type the array element type to test stack for.
         * @throws FrameException if the type-check fails.
         * @throws IllegalArgumentException if the type passed is not an array type.
         */
        public void checkArrayLoad(TypeSort type) throws FrameException
        {
            tester.checkArrayLoad(type);
        }

        /**
         * Verifies an array store.
         * @param type the array element type to test stack for.
         * @throws FrameException if the type-check fails.
         * @throws IllegalArgumentException if the type passed is not an array type.
         */
        public void checkArrayStore(TypeSort type) throws FrameException
        {
            tester.checkArrayStore(type);
        }

        /**
         * Obtains the respective frame type from an array type.
         * @param type the primitive type to look up.
         * @return the respective frame type.
         * @throws IllegalArgumentException if the type passed is not an array type.
         */
        public FrameType getFrameType(TypeSort type)
        {
            return tester.getFrameType(type);
        }
    }

    /**
     * Requires the frame element to be non-null.
     * @param ele the frame element to analyze.
     * @return the frame element passed as parameter, if non-null
     * @throws FrameException if the element is null.
     */
    private static FrameElement nonNull(FrameElement ele) throws FrameException
    {
        if (ele.getType() == FrameType.NULL)
            throw new FrameException("Dereferencing null pointer.");
        return ele;
    }

    /**
     * Resolves the following class context from it's internal name descriptor.
     * @param desc the class descriptor to load.
     * @return the class context
     * @throws IllegalArgumentException if the class context fails to load.
     */
    private static ClassContext loadClassContext(String desc)
    {
        ClassContext ctx = findContext(desc);
        if (ctx == null)
            throw new IllegalArgumentException("Cannot find class '" + desc + "'.");
        return ctx;
    }

    /**
     * Checks if the type signature represented by the value can be assigned to the type signature represented by the
     * assignee. In other words this checks whether if the value type signature can be converted via a widening
     * conversion to the assignee.
     * @param assignee the assignee type signature
     * @param value the value type signature.
     * @throws FrameException if the conversion cannot be done.
     */
    private static void checkIsAssignable(TypeSignature assignee, TypeSignature value) throws FrameException
    {
        if (!assignee.isObject() || !value.isObject())
            throw new IllegalArgumentException("Expected two object types.");
        if (!isAssignable(assignee, value))
            throw new FrameException("Unable to convert from '" + value + "' to '" + assignee + "'.");
    }

    private final MethodContext mth;
    private final Deque<FrameElement> operandStack;
    private final ArrayList<FrameElement> localVariables;

    private int maxStack;
    private int maxVars;

    /**
     * Constructs a stack map frame.
     * @param mth the method which this stack frame is created for.
     */
    public StackMapFrame(MethodContext mth)
    {
        this.mth = mth;
        if (mth.getName().equals("<init>"))
            allocateVar(0, new FrameElement(FrameType.UNINITIALIZED_THIS));

        operandStack = new ArrayDeque<>();
        localVariables = new ArrayList<>();
    }

    /**
     * Adds another local variable to the end of the stack.
     * @return the index of the variable just pushed.
     */
    public int pushVar()
    {
        localVariables.add(new FrameElement(FrameType.TOP));
        int size = localVariables.size();
        if (size > maxVars)
            maxVars = size;
        return size - 1;
    }

    /**
     * Removes the last local variables from the stack. If the last local variable is a double-slot, this will remove
     * two slots.
     * @return the index of the last variable just removed.
     */
    public int popVar()
    {

        int last = localVariables.size() - 1;
        if (last > 0 && localVariables.get(last - 1).getType().getSize() == 2)
            localVariables.remove(last--);
        localVariables.remove(last);
        return last;
    }

    /**
     * Executes the specified instruction statement.
     * @param inst the instruction to execute.
     * @throws FrameException if the type-check fails.
     */
    public void execute(InstStatement inst) throws FrameException
    {
        switch (inst.getOpcode())
        {

            case INST_AALOAD:
                checkArrayLoad(TypeSort.OBJECT);
                break;
            case INST_AASTORE:
                checkArrayStore(TypeSort.OBJECT);
                break;
            case INST_ACONST_NULL:
                pushOp(new FrameElement(FrameType.NULL));
                break;
            case INST_ALOAD:
                pushOp(new FrameElement(FrameType.NULL));
                FrameElement ele = checkVarType(inst.getIntArgValue(0), FrameType.OBJECT);
                if (ele.getType() != FrameType.NULL)
                {
                    operandStack.pop();
                    pushOp(ele);
                }
                break;
            case INST_ANEWARRAY:
                pushElePopOps(new FrameElement(FrameType.OBJECT, parseSig(inst, 0)), FrameType.INTEGER);
                break;
            case INST_ARETURN:
                popOp(FrameType.OBJECT);
                break;
            case INST_ARRAYLENGTH:
                FrameElement arr = nonNull(pushPopOps(FrameType.INTEGER, FrameType.OBJECT)[0]);
                if (arr.getRefSig().getSort() != TypeSort.ARRAY)
                    throw new FrameException("Not an array type");
                break;
            case INST_ASTORE:
                int index = inst.getIntArgValue(0);
                allocateVar(index, new FrameElement(FrameType.NULL));
                FrameElement obj = new FrameElement(FrameType.OBJECT, nonNull(popOp(FrameType.OBJECT)).getRefSig());
                allocateVar(index, obj);
                break;
            case INST_ATHROW:
                checkIsAssignable(TypeSignature.fromClass(Throwable.class), nonNull(popOp(FrameType.OBJECT)).getRefSig());
                break;
            case INST_BALOAD:
                checkArrayLoad(TypeSort.BYTE);
                break;
            case INST_BASTORE:
                checkArrayStore(TypeSort.BYTE);
                break;
            case INST_CALOAD:
                checkArrayLoad(TypeSort.CHAR);
                break;
            case INST_CASTORE:
                checkArrayStore(TypeSort.CHAR);
                break;
            case INST_CHECKCAST:
                checkOpType(FrameType.OBJECT);
                TypeSignature type = nonNull(operandStack.peek()).getRefSig();
                TypeSignature target = parseClassType(inst, 0);

                //Ignore implicit type casts.
                if (!isAssignable(target, type))
                {
                    operandStack.pop();
                    pushOp(new FrameElement(FrameType.OBJECT, target));
                }
                break;
            case INST_D2F:
                pushPopOps(FrameType.FLOAT, FrameType.DOUBLE);
                break;
            case INST_D2I:
                pushPopOps(FrameType.INTEGER, FrameType.DOUBLE);
                break;
            case INST_D2L:
                pushPopOps(FrameType.LONG, FrameType.DOUBLE);
                break;
            case INST_DALOAD:
                checkArrayLoad(TypeSort.DOUBLE);
                break;
            case INST_DASTORE:
                checkArrayStore(TypeSort.DOUBLE);
                break;
            case INST_DCMPG:
            case INST_DCMPL:
                pushPopOps(FrameType.INTEGER, FrameType.DOUBLE, FrameType.DOUBLE);
                break;
            case INST_DCONST_0:
            case INST_DCONST_1:
                pushOp(new FrameElement(FrameType.DOUBLE));
                pushOp(new FrameElement(FrameType.TOP, true));
                break;
            case INST_DADD:
            case INST_DSUB:
            case INST_DDIV:
            case INST_DMUL:
            case INST_DREM:
                pushPopOps(FrameType.DOUBLE, FrameType.DOUBLE, FrameType.DOUBLE);
                break;
            case INST_DLOAD:
                pushOp(new FrameElement(FrameType.DOUBLE));
                pushOp(new FrameElement(FrameType.TOP, true));
                checkVarType(inst.getIntArgValue(0), FrameType.DOUBLE);
                break;
            case INST_DNEG:
                pushPopOps(FrameType.DOUBLE, FrameType.DOUBLE);
                break;
            case INST_DRETURN:
                popOp(FrameType.DOUBLE);
                break;
            case INST_DSTORE:
                allocateVar(inst.getIntArgValue(0), new FrameElement(FrameType.DOUBLE));
                popOp(FrameType.DOUBLE);
                break;
            case INST_DUP:
                dup(1, 0);
                break;
            case INST_DUP_X1:
                dup(1, 1);
                break;
            case INST_DUP_X2:
                dup(1, 2);
                break;
            case INST_DUP2:
                dup(2, 0);
                break;
            case INST_DUP2_X1:
                dup(2, 1);
                break;
            case INST_DUP2_X2:
                dup(2, 2);
                break;
            case INST_F2D:
                pushPopOps(FrameType.FLOAT, FrameType.DOUBLE);
                break;
            case INST_F2I:
                pushPopOps(FrameType.FLOAT, FrameType.INTEGER);
                break;
            case INST_F2L:
                pushPopOps(FrameType.FLOAT, FrameType.LONG);
                break;
            case INST_FALOAD:
                checkArrayLoad(TypeSort.FLOAT);
                break;
            case INST_FASTORE:
                checkArrayStore(TypeSort.FLOAT);
                break;
            case INST_FCMPG:
            case INST_FCMPL:
                pushPopOps(FrameType.INTEGER, FrameType.FLOAT, FrameType.FLOAT);
                break;
            case INST_FCONST_0:
            case INST_FCONST_1:
            case INST_FCONST_2:
                pushOp(new FrameElement(FrameType.FLOAT));
                break;
            case INST_FADD:
            case INST_FSUB:
            case INST_FMUL:
            case INST_FDIV:
            case INST_FREM:
                pushPopOps(FrameType.FLOAT, FrameType.FLOAT, FrameType.FLOAT);
                break;
            case INST_FLOAD:
                pushOp(new FrameElement(FrameType.FLOAT));
                checkVarType(inst.getIntArgValue(0), FrameType.FLOAT);
                break;
            case INST_FNEG:
                pushPopOps(FrameType.FLOAT, FrameType.FLOAT);
                break;
            case INST_FRETURN:
                popOp(FrameType.FLOAT);
                break;
            case INST_FSTORE:
                allocateVar(inst.getIntArgValue(0), new FrameElement(FrameType.FLOAT));
                popOp(FrameType.FLOAT);
                break;
            case INST_GETFIELD:
                nonNull(invokeOps(parseSig(inst, 2), parseClassType(inst, 0))[0]);
                break;
            case INST_GETSTATIC:
                invokeOps(parseClassType(inst, 0), parseSig(inst, 2));
                break;
            case INST_GOTO:
            case INST_NOP:
            case INST_RETURN:
                break;
            case INST_I2B:
            case INST_I2S:
            case INST_I2C:
                checkOpType(FrameType.INTEGER);
                break;
            case INST_I2D:
                pushPopOps(FrameType.DOUBLE, FrameType.INTEGER);
                break;
            case INST_I2F:
                pushPopOps(FrameType.FLOAT, FrameType.INTEGER);
                break;
            case INST_I2L:
                pushPopOps(FrameType.LONG, FrameType.INTEGER);
                break;
            case INST_IALOAD:
                checkArrayLoad(TypeSort.INTEGER);
                break;
            case INST_IASTORE:
                checkArrayStore(TypeSort.INTEGER);
                break;
            case INST_ICONST_0:
            case INST_ICONST_1:
            case INST_ICONST_2:
            case INST_ICONST_3:
            case INST_ICONST_4:
            case INST_ICONST_5:
            case INST_ICONST_M1:
            case INST_BIPUSH:
            case INST_SIPUSH:
                pushOp(new FrameElement(FrameType.INTEGER));
                break;
            case INST_IF_ACMPEQ:
            case INST_IF_ACMPNE:
                pushPopOps(null, FrameType.OBJECT, FrameType.OBJECT);
                break;
            case INST_IF_ICMPEQ:
            case INST_IF_ICMPGE:
            case INST_IF_ICMPGT:
            case INST_IF_ICMPLE:
            case INST_IF_ICMPLT:
            case INST_IF_ICMPNE:
                pushPopOps(null, FrameType.INTEGER, FrameType.INTEGER);
                break;
            case INST_IFEQ:
            case INST_IFGE:
            case INST_IFGT:
            case INST_IFLE:
            case INST_IFLT:
            case INST_IFNE:
                popOp(FrameType.INTEGER);
                break;
            case INST_IFNONNULL:
            case INST_IFNULL:
                popOp(FrameType.OBJECT);
                break;
            case INST_IINC:
                checkVarType(inst.getIntArgValue(0), FrameType.INTEGER);
                break;
            case INST_ILOAD:
                pushOp(new FrameElement(FrameType.INTEGER));
                checkVarType(inst.getIntArgValue(0), FrameType.INTEGER);
                break;
            case INST_IADD:
            case INST_ISUB:
            case INST_IMUL:
            case INST_IDIV:
            case INST_IREM:
            case INST_IAND:
            case INST_IOR:
            case INST_ISHL:
            case INST_ISHR:
            case INST_IUSHR:
            case INST_IXOR:
                pushPopOps(FrameType.INTEGER, FrameType.INTEGER, FrameType.INTEGER);
                break;
            case INST_INEG:
                pushPopOps(FrameType.INTEGER, FrameType.INTEGER);
                break;
            case INST_INSTANCEOF:
                pushPopOps(FrameType.INTEGER, FrameType.OBJECT);
                break;
            case INST_INVOKESPECIAL:
                if (inst.getArgValue(1, String.class).equals("<init>"))
                {
                    TypeSignature sig = parseSig(inst, 2);
                    invokeInitOps(inst.getResolvedSymbols(), loadClassContext(inst.getArgValue(0, String.class)),
                            sig.getParameterTypes());
                    break;
                }
                //fall-through
            case INST_INVOKEINTERFACE:
            case INST_INVOKEVIRTUAL:
                TypeSignature sig = parseSig(inst, 2);
                TypeSignature[] params = insertObjParam(parseClassType(inst, 0), sig.getParameterTypes());
                nonNull(invokeOps(sig.getReturnType(), params)[0]);
                break;
            case INST_INVOKEDYNAMIC:
            case INST_INVOKESTATIC:
                sig = parseSig(inst, 2);
                invokeOps(sig.getReturnType(), sig.getParameterTypes());
                break;
            case INST_IRETURN:
                popOp(FrameType.INTEGER);
                break;
            case INST_ISTORE:
                allocateVar(inst.getIntArgValue(0), new FrameElement(FrameType.INTEGER));
                popOp(FrameType.INTEGER);
                break;
            case INST_L2D:
                pushPopOps(FrameType.DOUBLE, FrameType.LONG);
                break;
            case INST_L2F:
                pushPopOps(FrameType.FLOAT, FrameType.LONG);
                break;
            case INST_L2I:
                pushPopOps(FrameType.INTEGER, FrameType.LONG);
                break;
            case INST_LALOAD:
                checkArrayLoad(TypeSort.LONG);
                break;
            case INST_LASTORE:
                checkArrayStore(TypeSort.LONG);
                break;
            case INST_LCMP:
                pushPopOps(FrameType.INTEGER,FrameType.LONG, FrameType.LONG);
                break;
            case INST_LCONST_0:
            case INST_LCONST_1:
                pushOp(new FrameElement(FrameType.LONG));
                pushOp(new FrameElement(FrameType.TOP, true));
                break;
            case INST_LDC:
                sig = inst.getArgTypeSig(0);
                FrameType ftype = getFrameType(sig.getSort());
                if (ftype == FrameType.OBJECT)
                    pushOp(new FrameElement(FrameType.OBJECT, sig));
                else
                {
                    pushOp(new FrameElement(ftype));
                    if (ftype.getSize() == 2)
                        pushOp(new FrameElement(FrameType.TOP, true));
                }
                break;
            case INST_LADD:
            case INST_LSUB:
            case INST_LMUL:
            case INST_LDIV:
            case INST_LREM:
            case INST_LOR:
            case INST_LAND:
            case INST_LSHL:
            case INST_LSHR:
            case INST_LUSHR:
            case INST_LXOR:
                pushPopOps(FrameType.LONG, FrameType.LONG, FrameType.LONG);
                break;
            case INST_LLOAD:
                pushOp(new FrameElement(FrameType.LONG));
                pushOp(new FrameElement(FrameType.TOP, true));
                checkVarType(inst.getIntArgValue(0), FrameType.LONG);
                break;
            case INST_LNEG:
                pushPopOps(FrameType.LONG, FrameType.LONG);
                break;
            case INST_LRETURN:
                popOp(FrameType.LONG);
                break;
            case INST_LSTORE:
                allocateVar(inst.getIntArgValue(0), new FrameElement(FrameType.LONG));
                popOp(FrameType.LONG);
                break;
            case INST_MONITORENTER:
            case INST_MONITOREXIT:
                nonNull(popOp(FrameType.OBJECT));
                break;
            case INST_MULTIANEWARRAY:
                TypeSignature arrType = parseSig(inst, 0);
                int dims = inst.getIntArgValue(1);
                FrameType[] popping = new FrameType[dims];
                Arrays.fill(popping, FrameType.INTEGER);
                pushElePopOps(new FrameElement(FrameType.OBJECT, arrType), popping);
                break;
            case INST_NEW:
                pushOp(new FrameElement(FrameType.UNINITIALIZED, inst));
                break;
            case INST_NEWARRAY:
                String arrSig;
                switch (inst.getIntArgValue(0))
                {
                    case Opcodes.T_BOOLEAN: arrSig = "[Z"; break;
                    case Opcodes.T_CHAR: arrSig = "[C"; break;
                    case Opcodes.T_FLOAT: arrSig = "[F"; break;
                    case Opcodes.T_DOUBLE: arrSig = "[D"; break;
                    case Opcodes.T_BYTE: arrSig = "[B"; break;
                    case Opcodes.T_SHORT: arrSig = "[S"; break;
                    case Opcodes.T_INT: arrSig = "[I"; break;
                    case Opcodes.T_LONG: arrSig = "[J"; break;
                    default: throw new IllegalArgumentException();
                }
                pushElePopOps(new FrameElement(FrameType.OBJECT, parseTypeSig(arrSig)), FrameType.INTEGER);
                break;
            case INST_POP:
                popOp(FrameType.TOP);
                break;
            case INST_POP2:
                operandStack.pop();
                popOp(FrameType.TOP);
                break;
            case INST_PUTFIELD:
                nonNull(invokeOps(VOID_TYPE, parseClassType(inst, 0), parseSig(inst, 2))[0]);
                break;
            case INST_PUTSTATIC:
                invokeOps(VOID_TYPE, parseSig(inst, 2));
                break;
            case INST_SALOAD:
                checkArrayLoad(TypeSort.SHORT);
                break;
            case INST_SASTORE:
                checkArrayStore(TypeSort.SHORT);
                break;
            case INST_SWAP:
                checkStackUnderflow(2);
                FrameElement[] popped = popNoCheck(2);
                pushOp(popped[1]);
                pushOp(popped[0]);
                break;
            case INST_LOOKUPSWITCH:
            case INST_TABLESWITCH:
                popOp(FrameType.INTEGER);
                break;
        }
    }



    /**
     * Inserts the implicit object parameter at the beginning of the parameter list.
     * @param objref the object reference
     * @param params the explicit parameter list.
     * @return the full parameter list.
     */
    private TypeSignature[] insertObjParam(TypeSignature objref, TypeSignature[] params)
    {
        TypeSignature[] ret = new TypeSignature[params.length + 1];
        System.arraycopy(params, 0, ret, 1, params.length);
        ret[0] = objref;
        return ret;
    }

    /**
     * Parses the type signature from an instruction
     * @param inst the instruction to parse from.
     * @param arg the argument number of the instruction
     * @return the parsed type signature.
     * @throws IllegalArgumentException if unable to parse type signature.
     */
    private TypeSignature parseSig(InstStatement inst, int arg)
    {
        TypeSignature sig = parseTypeSig(inst.getArgValue(arg, String.class));
        if (sig == null)
            throw new IllegalArgumentException();
        return sig;
    }

    /**
     * Parses a class identifier type from an instruction as a type signature
     * @param inst the instruction to parse from.
     * @param arg the argument number of the instruction
     * @return the parsed type signature.
     * @throws IllegalArgumentException if unable to parse type signature.
     */
    private TypeSignature parseClassType(InstStatement inst, int arg)
    {
        String classDesc = inst.getArgValue(arg, String.class);
        TypeSignature sig;
        if (classDesc.startsWith("["))
            sig = parseTypeSig(classDesc);
        else
            sig = parseTypeSig("L" + classDesc + ";");
        if (sig == null)
            throw new IllegalArgumentException();
        return sig;
    }

    /**
     * Executes an invocation with a particular method type signature (a return value and a series of parameters)
     * @param ret the return value
     * @param params the series of parameters.
     * @throws FrameException if the type-check fails.
     * @return the actual parameters
     */
    private FrameElement[] invokeOps(TypeSignature ret, TypeSignature... params) throws FrameException
    {
        FrameElement fret;
        if (ret.getSort() == TypeSort.VOID)
            fret = null;
        else
        {
            FrameType ftype = getFrameType(ret.getSort());
            if (ftype == FrameType.OBJECT)
                fret = new FrameElement(FrameType.OBJECT, ret);
            else
                fret = new FrameElement(ftype);
        }

        FrameType fparams[] = new FrameType[params.length];
        for (int i = 0; i < params.length; i++)
            fparams[i] = getFrameType(params[i].getSort());

        FrameElement[] actualParams = pushElePopOps(fret, fparams);
        for (int i = 0; i < actualParams.length; i++)
        {
            if (actualParams[i].getType() == FrameType.OBJECT)
                checkIsAssignable(params[i], actualParams[i].getRefSig());
        }

        return actualParams;
    }

    /**
     * Executes an &lt;init&gt; invocation on the operand stack. This will also initialize any references to the
     * uninitialized object.
     * @param resolved the code symbols needed to resolve the labels.
     * @param initCtx the class context of whose &lt;init&gt; method is being called.
     * @param params the series of parameters (excluding the object reference parameter).
     * @throws FrameException if the type-check fails.
     */
    private void invokeInitOps(CodeSymbols resolved, ClassContext initCtx, TypeSignature... params) throws
            FrameException
    {
        FrameType fparams[] = new FrameType[params.length + 1];
        for (int i = 0; i < params.length; i++)
            fparams[i + 1] = getFrameType(params[i].getSort());

        fparams[0] = FrameType.TOP;

        String err = null;
        TypeSignature replace;
        FrameElement[] actualParams = pushElePopOps(null, fparams);
        switch (actualParams[0].getType())
        {
            case NULL:
                throw new FrameException("Dereferencing null pointer.");
            case UNINITIALIZED_THIS:
                ClassContext thisCtx = mth.getOwner();
                replace = parseTypeSig("L" + thisCtx + ";");
                if (!thisCtx.equals(initCtx) && !thisCtx.getSuperClass().equals(initCtx))
                    err = "Must call <init> of super class or of this class";
                break;
            case UNINITIALIZED:
                replace = parseClassType(actualParams[0].getStatement() /*NEW <type-sig>*/, 0);
                if (!replace.getClassDescriptor().equals(initCtx.toString()))
                    err = "Must call <init> method of " + replace.getClassDescriptor();
                break;
            default:
                throw new FrameException("Expected: " + FrameType.UNINITIALIZED_THIS + " or " +
                        FrameType.UNINITIALIZED);
        }

        ArrayList<FrameElement> tmpOperand = new ArrayList<>(operandStack);
        FrameElement freplace = new FrameElement(FrameType.OBJECT, replace);
        init(tmpOperand, resolved, actualParams[0], freplace);
        operandStack.clear();
        operandStack.addAll(tmpOperand);
        init(localVariables, resolved, actualParams[0], freplace);

        if (err != null)
            throw new FrameException(err);

        for (int i = 1; i < actualParams.length; i++)
        {
            if (actualParams[i].getType() == FrameType.OBJECT)
                checkIsAssignable(params[i], actualParams[i].getRefSig());
        }
    }

    /**
     * Finds any matching uninitialized frame elements from the list and initializes them.
     * @param eles the frame element list
     * @param resolved the resolved code symbols
     * @param uninit the uninitialized frame element to look for
     * @param init the initialized frame element to replace with
     */
    private void init(List<FrameElement> eles, CodeSymbols resolved, FrameElement uninit, FrameElement init)
    {
        ListIterator<FrameElement> itr = eles.listIterator();
        while (itr.hasNext())
        {
            FrameElement old = itr.next();
            switch (uninit.getType())
            {
                case UNINITIALIZED:
                    if (old.getType() == FrameType.UNINITIALIZED &&
                            uninit.unwrapElement(resolved) == old.unwrapElement(resolved))
                        itr.set(init);
                    break;
                case UNINITIALIZED_THIS:
                    if (old.getType() == FrameType.UNINITIALIZED_THIS)
                        itr.set(init);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Pops a few operands, then pushes an operand. Note that the order of these operands is in reverse order,
     * i.e. the last element refers to the first element popped/pushed.
     * @param push the element to be pushed.
     * @param pop the expected frame types to be popped.
     * @return the frame elements popped.
     * @throws FrameException if the type-check fails.
     */
    private FrameElement[] pushElePopOps(FrameElement push, FrameType... pop) throws FrameException
    {
        try
        {
            return pushPopOps(null, pop);
        }
        finally
        {
            if (push != null)
            {
                operandStack.push(push);
                if (push.getType().getSize() == 2)
                    operandStack.push(new FrameElement(FrameType.TOP, true));
            }
        }
    }

    /**
     * Pops a few operands, then pushes an operand. Note that the order of these operands is in reverse order,
     * i.e. the last element refers to the first element popped/pushed.
     * @param push the element type to be pushed.
     * @param pop the expected frame types to be popped.
     * @return the frame elements popped.
     * @throws FrameException if the type-check fails.
     */
    private FrameElement[] pushPopOps(FrameType push, FrameType... pop) throws FrameException
    {
        FrameException e = null;
        FrameElement[] eles = new FrameElement[pop.length];

        int i = pop.length;
        while (i --> 0)
        {
            try
            {
                eles[i] = popOp(pop[i]);
            }
            catch (FrameException ex)
            {
                if (e == null)
                    e = ex;
                else
                    e.addSuppressed(ex);
                if (operandStack.isEmpty())
                    break;
            }
        }

        if (push == FrameType.OBJECT)
        {
            if (e != null)
                pushOp(new FrameElement(FrameType.NULL));
        }
        else if (push != null)
        {
            pushOp(new FrameElement(push));
            if (push.getSize() == 2)
                pushOp(new FrameElement(FrameType.TOP, true));
        }

        if (e != null)
            throw e;
        return eles;
    }

    /**
     * Pops the number of frame values from the operation stack without any type verification. Note that this will
     * return the elements in REVERSE order, i.e. the first element of this array is the last element popped. If
     * there are less than enough operands left on the operand stack, this will place in TOP frames
     * @param popCount the number of elements to pop.
     * @return the popped elements in REVERSE order.
     */
    private FrameElement[] popNoCheck(int popCount)
    {
        FrameElement[] popped = new FrameElement[popCount];
        int i = popped.length;
        while (!operandStack.isEmpty() && i --> 0)
            popped[i] = operandStack.pop();
        while (i --> 0)
            popped[i] = new FrameElement(FrameType.TOP);
        return popped;
    }

    /**
     * Checks that the stack has the specified number of elements remaining.
     * @param size the size of the stack to guarantee
     * @throws FrameException if a stack underflow occurs.
     */
    private void checkStackUnderflow(int size) throws FrameException
    {
        if (operandStack.size() < size)
            throw new FrameException("Stack underflow.");
    }

    /**
     * This will pop an operand with the type specified. This method guarantees that the frame element WILL BE
     * popped from the operand stack regardless of any errors that might occur.
     * @param type the type required for frame element.
     * @return the frame element guaranteed to be of the right type.
     * @throws FrameException if the type-check fails.
     */
    private FrameElement popOp(FrameType type) throws FrameException
    {
        FrameElement ret;
        try
        {
            checkOpType(type);
        }
        finally
        {
            if (type.getSize() == 2)
                operandStack.pop();
            ret = operandStack.pop();
        }
        return ret;
    }

    /**
     * Executes a duplication of an element. If there is enough elements in the stack, this guarantees that the
     * operation will be performed (even in the event of some errors).
     * @param dupSize the number of elements to duplicate.
     * @param spacing the number of elements to separate the duplicate from the original.
     * @throws FrameException if doing so would clobber 2-block elements or if a stack underflow would occur.
     */
    private void dup(int dupSize, int spacing) throws FrameException
    {
        checkStackUnderflow(dupSize + spacing);
        FrameElement popped[] = popNoCheck(dupSize + spacing);

        boolean clobbered = false;
        if (popped[spacing].isBlock())
        {
            clobbered = true;
            popped[spacing].unblock();
        }

        if (popped[0].isBlock())
        {
            clobbered = true;
            popped[0].unblock();
        }

        //Push dup elements below our spacer, then push everything again.
        for (int i = 0; i < dupSize; i++)
            pushOp(popped[i + spacing]);
        for (FrameElement e : popped)
            pushOp(e);

        if (clobbered)
            throw new FrameException("Clobbering a computation type 2 element.");
    }

    /**
     * This will push the specified frame element to the top of the operand stack.
     * @param ele the frame element to add.
     */
    private void pushOp(FrameElement ele)
    {
        operandStack.push(ele);
        maxStack = Math.max(maxStack, operandStack.size());
    }

    /**
     * Checks that the top operand is of the correct type. For special 2 size operands, the top-most MUST be of type
     * TOP and the second top-most MUST be of the specified type. If any conditions fail, an exception will be thrown.
     * @param type the frame type required.
     * @throws FrameException if the type-check fails.
     */
    private void checkOpType(FrameType type) throws FrameException
    {
        checkStackUnderflow(type.getSize());

        FrameElement top = operandStack.peek();
        if (type.getSize() == 2)
        {
            if (!top.isBlock() || top.getType() != FrameType.TOP)
                throw new FrameException("Expected: (" + type + ", blocked uninitialized).");
            FrameElement tmp = operandStack.poll();
            top = operandStack.peek();
            operandStack.push(tmp);
        }

        if (top.isBlock())
        {
            top.unblock();
            throw new FrameException("Clobbering a computation type 2 element.");
        }

        if (type == FrameType.TOP)
            return;

        if (type != top.getType())
        {
            if (type == FrameType.OBJECT && top.getType() == FrameType.NULL)
                return;
            throw new FrameException("Expected: " + type + ". Actual: " + top.getType());
        }
    }

    /**
     * Allocates the space for this new variable.
     * @param index the index of the variable.
     * @param ele the frame element to allocate with.
     */
    private void allocateVar(int index, FrameElement ele)
    {
        int newSize = index + ele.getType().getSize();
        while (localVariables.size() < newSize)
            pushVar();
        localVariables.set(index, ele);
        if (ele.getType().getSize() == 2)
            localVariables.set(index + 1, new FrameElement(FrameType.TOP, true));
    }

    /**
     * Checks that the variable specified is the right type. For special 2 size operands, the next slot MUST be of type
     * TOP and the index specified MUST be of the specified type. The specified type must be a queryable type, i.e.
     * has a value that has a meaningful value, not NULL or TOP. If any conditions fail, an exception will be thrown.
     * @param index the index of the variable.
     * @param type the frame type required.
     * @throws FrameException if the type-check fails.
     * @throws IllegalArgumentException if the type passed is not a valid queryable type.
     * @return the frame element representing the variable.
     */
    private FrameElement checkVarType(int index, FrameType type) throws FrameException
    {
        if (type == FrameType.TOP || type == FrameType.NULL)
            throw new IllegalArgumentException();

        if (index >= localVariables.size())
            throw new FrameException("Illegal variable.");

        if (type.getSize() == 2)
        {
            if (index + 1 >= localVariables.size())
                throw new FrameException("Expected: (" + type + ", blocked uninitialized).");

            FrameElement block = localVariables.get(index + 1);
            if (!block.isBlock() || block.getType() != FrameType.TOP)
                throw new FrameException("Expected: (" + type + ", blocked uninitialized).");
        }

        FrameElement ele = localVariables.get(index);
        if (type != ele.getType())
        {
            if (type == FrameType.OBJECT && ele.getType() == FrameType.NULL)
                return ele;
            throw new FrameException("Expected: " + type + ".");
        }
        return ele;
    }

    /**
     * Verifies an array load.
     * @param type the array element type to test stack for.
     * @throws FrameException if the type-check fails.
     * @throws IllegalArgumentException if the type passed is not an array type.
     */
    private void checkArrayLoad(TypeSort type) throws FrameException
    {
        String msg = "Not an array type of type " + type.toString().toLowerCase() + ".";
        FrameType ftype = getFrameType(type);
        FrameElement[] popped = pushPopOps(ftype, FrameType.OBJECT, FrameType.INTEGER);

        TypeSignature sig;
        try
        {
            sig = nonNull(popped[0]).getRefSig();

            if (sig.getSort() != TypeSort.ARRAY)
                throw new FrameException(msg);

            TypeSort ele = (sig.getDimensions() == 1) ? sig.getComponentType().getSort() : TypeSort.OBJECT;
            if (ele != type)
                throw new FrameException(msg);
            if (ftype == FrameType.OBJECT)
                pushOp(new FrameElement(FrameType.OBJECT, sig.getElementType()));
        }
        catch(FrameException e)
        {
            if (ftype == FrameType.OBJECT)
                pushOp(new FrameElement(FrameType.NULL));
            throw e;
        }
    }

    /**
     * Verifies an array store.
     * @param type the array element type to test stack for.
     * @throws FrameException if the type-check fails.
     * @throws IllegalArgumentException if the type passed is not an array type.
     */
    private void checkArrayStore(TypeSort type) throws FrameException
    {
        String msg = "Not an array type of type " + type.toString().toLowerCase() + ".";

        FrameType ftype = getFrameType(type);
        FrameElement[] popped = pushPopOps(null, FrameType.OBJECT, FrameType.INTEGER, ftype);
        TypeSignature sig = nonNull(popped[0]).getRefSig();
        if (sig.getSort() != TypeSort.ARRAY)
            throw new FrameException(msg);

        TypeSort ele = (sig.getDimensions() == 1) ? sig.getComponentType().getSort() : TypeSort.OBJECT;
        if (ele != type)
            throw new FrameException(msg);

        if (ftype == FrameType.OBJECT)
        {
            if (popped[2].getType() == FrameType.NULL)
                return;
            checkIsAssignable(sig.getElementType(), popped[2].getRefSig());
        }
    }

    /**
     * Obtains the respective frame type from an array type.
     * @param type the primitive type to look up.
     * @return the respective frame type.
     * @throws IllegalArgumentException if the type passed is not an array type.
     */
    private FrameType getFrameType(TypeSort type)
    {
        switch (type)
        {
            case METHOD:
            case VOID:
                throw new IllegalArgumentException("Expected array type.");
            case OBJECT:
                return FrameType.OBJECT;
            case FLOAT:
                return FrameType.FLOAT;
            case DOUBLE:
                return FrameType.DOUBLE;
            case LONG:
                return FrameType.LONG;
            default:
                return FrameType.INTEGER;
        }
    }
}
