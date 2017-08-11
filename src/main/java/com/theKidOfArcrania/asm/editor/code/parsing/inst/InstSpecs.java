package com.theKidOfArcrania.asm.editor.code.parsing.inst;

import com.theKidOfArcrania.asm.editor.code.parsing.*;
import com.theKidOfArcrania.asm.editor.context.*;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.theKidOfArcrania.asm.editor.code.parsing.BasicParamType.*;
import static com.theKidOfArcrania.asm.editor.code.parsing.Range.characterRange;
import static com.theKidOfArcrania.asm.editor.context.ClassContext.findContext;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.isAssignable;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.H_NEWINVOKESPECIAL;

/**
 * Utility class that contains all the valid instruction specifications in standard Java bytecode.
 * @author Henry Wang
 */
public class InstSpecs
{
    private static final MultipleParamType CONSTANTS_VALUE = new MultipleParamType(INTEGER, LONG, FLOAT, DOUBLE,
            STRING, METHOD_SIGNATURE, FIELD_SIGNATURE, METHOD_HANDLE)
    {
        @Override
        public String getName()
        {
            return "constants value";
        }
    };

    public static final InstSpec FIELD_INST_SPEC = new InstSpec(CLASS_NAME, IDENTIFIER,
            BasicParamType.FIELD_SIGNATURE)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            String name = inst.getArgValue(1, String.class);
            if (name.charAt(0) == '<') //Special method initializer
            {
                logger.logError("Illegal character.", characterRange(inst.getArgPos(1).getStart()));
                return false;
            }
            return true;
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            ClassContext thisCtx = resolved.getThisContext();
            String ownerName = inst.getArgValue(0, String.class);
            String name = inst.getArgValue(1, String.class);
            ClassContext owner = findContext(ownerName);
            TypeSignature typeSig = TypeSignature.parseTypeSig(inst.getArgValue(2, String.class));

            if (owner == null)
            {
                logger.logError("Cannot resolve symbol '" + ownerName + "'.", inst.getArgPos(0));
                return false;
            }

            FieldContext fld = owner.findField(name);
            if (fld == null)
            {
                logger.logError("Cannot resolve symbol '" + name + "'.", inst.getArgPos(1));
                return false;
            }

            String unresolved = typeSig.getUnresolvedClassSymbols();
            if (unresolved != null)
            {
                logger.logError("Cannot resolve symbol(s) " + unresolved + ".", inst.getArgPos(2));
                return false;
            }

            if (!fld.getSignature().equals(typeSig))
            {
                logger.logError("Wrong type.", inst.getArgPos(2));
                return false;
            }

            if (!thisCtx.checkAccessClass(owner))
            {
                logger.logError("Cannot access " + (owner.isInterface() ? "interface" : "class") +
                        " '" + owner.getName() + "'.", inst.getArgPos(0));
                return false;
            }

            if (!thisCtx.checkAccessMember(fld))
            {
                logger.logError("Cannot access field '" + name + "'.", inst.getArgPos(1));
                return false;
            }

            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitFieldInsn(inst.getOpcodeNum(), inst.getArgValue(0, String.class), inst.getArgValue(1,
                    String.class), inst.getArgValue(2, String.class));
        }
    };

    public static final InstSpec IINC_INST_SPEC = new InstSpec(INTEGER, INTEGER)
    {

        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            if (inst.getIntArgValue(0) < 0)
            {
                logger.logError("Expected: non-negative variable index", inst.getArgPos(0));
                return false;
            }
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitIincInsn(inst.getIntArgValue(0), inst.getIntArgValue(1));
        }
    };

    public static final InstSpec INT_INST_SPEC = new InstSpec(INTEGER)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            int val = inst.getIntArgValue(0);
            Range range = inst.getArgPos(0);
            switch (inst.getOpcodeNum())
            {
                case Opcodes.BIPUSH:
                    if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE)
                    {
                        logger.logError("Value must be between -128 and 127", range);
                        return false;
                    }
                    break;
                case Opcodes.SIPUSH:
                    if (val < Short.MIN_VALUE || val > Short.MAX_VALUE)
                    {
                        logger.logError("Value must be between -32768 and 32767", range);
                        return false;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid opcode.");
            }
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitIntInsn(inst.getOpcodeNum(), inst.getIntArgValue(0));
        }
    };

    public static final InstSpec INT_NEWARRAY_INST_SPEC = new InstSpec(IDENTIFIER)
    {

        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            ArrayType type = ArrayType.directory.get(inst.getArgValue(0, String.class));
            if (type == null)
            {
                StringBuilder msg = new StringBuilder("Expected: array type of the following: ");
                boolean first = true;
                for (ArrayType t : ArrayType.values())
                {
                    if (first)
                        first = false;
                    else
                        msg.append(", ");
                    msg.append(t.name());
                }
                logger.logError(msg.toString(), inst.getArgPos(0));
                return false;
            }

            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitIntInsn(inst.getOpcodeNum(), ArrayType.directory.get(inst.getArgValue(0, String.class)).opcode);
        }
    };

    public static final InstSpec INVOKE_DYN_INST_SPEC = new InstSpec(IDENTIFIER, METHOD_SIGNATURE, METHOD_HANDLE /*,...*/)
    {
        private static final int OFFSET = 3;

        private final ClassContext CALL_SITE = findContext("java/lang/invoke/CallSite");
        private final MultipleParamType allowedArgs = CONSTANTS_VALUE;

        @Override
        public Argument[] parseInstArgs(CodeTokenReader reader)
        {
            boolean error = false;
            ArrayList<Argument> args = new ArrayList<>();
            Argument[] ret = super.parseInstArgs(reader);
            if (ret == null)
                return null;
            Collections.addAll(args, ret);
            while (reader.nextArgument())
            {
                if (reader.hasTokenError())
                    error = true;
                else if (!allowedArgs.matches(reader))
                {
                    error = true;
                    reader.errorExpected(allowedArgs.getName());
                }
                else if (allowedArgs.checkToken(reader))
                    args.add(new Argument(reader, allowedArgs));
            }
            return error ? null : args.toArray(new Argument[0]);
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            int argc = inst.getArgSize();
            for (int i = OFFSET - 1; i < argc; i++)
                if (!verifyVarSymbols(logger, inst, resolved, i))
                    return false;

            Handle mth = resolved.getHandle(inst.getArgValue(2, String.class));
            if (mth.getTag() != H_NEWINVOKESPECIAL && mth.getTag() != H_INVOKESTATIC)
            {
                logger.logError("Expected: static or <init> method handle.", inst.getArgPos(2));
                return false;
            }

            ArrayList<TypeSignature> actualParams = new ArrayList<>();
            String[] args = {"Ljava/lang/invoke/MethodHandles$Lookup;", "Ljava/lang/String;",
                    "Ljava/lang/invoke/MethodType;"};
            for (String arg : args)
                actualParams.add(TypeSignature.parseTypeSig(arg));
            for (int i = OFFSET; i < inst.getArgSize(); i++)
                actualParams.add(inst.getArgTypeSig(i));

            TypeSignature mthSig = TypeSignature.parseTypeSig(mth.getDesc());
            ClassContext retCtx = findContext(mthSig.getReturnType().getClassDescriptor());
            if (retCtx == null || !CALL_SITE.isAssignableFrom(retCtx))
            {
                logger.logError("Expected: method handle MUST return CallSite.", inst.getArgPos(2));
                return false;
            }

            TypeSignature mthParams[] = mthSig.getParameterTypes();
            if (mthParams.length != actualParams.size())
            {
                logger.logError("Not right number of parameters", inst.getLineRange());
                return false;
            }

            boolean failed = false;
            for (int i = 0; i < mthParams.length; i++)
            {
                TypeSignature actual = actualParams.get(i);
                TypeSignature mthParam = mthParams[i];
                if (mthParam.isWrapper())
                    mthParam = mthParam.unwrap();
                if (!isAssignable(mthParam, actual))
                {
                    logger.logError("Cannot convert " + canonical(actual) + " to " + canonical(mthParam),
                            inst.getLineRange());
                    failed = true;
                }
            }

            return !failed;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {

            Object[] args = new Object[inst.getArgSize() - OFFSET];
            for (int i = 0; i < args.length; i++)
                args[i] = unmarshall(inst, resolved, i + OFFSET);
            writer.visitInvokeDynamicInsn(inst.getArgValue(0, String.class), inst.getArgValue(1, String.class),
                    resolved.getHandle(inst.getArgValue(2, String.class)), args);
        }

        /**
         * Obtains the canonical name of this signature.
         * @param sig the type signature to obtain canonical name.
         * @return the canonical name.
         */
        private String canonical(TypeSignature sig)
        {
            if (sig.isPrimitive())
                sig = sig.wrap();
            return sig.getClassDescriptor().replace('/', '.');
        }
    };

    public static final InstSpec JMP_INST_SPEC = new InstSpec(IDENTIFIER)
    {

        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            String name = inst.getArgValue(0, String.class);
            if (name.charAt(0) == '<') //Special method initializer
            {
                logger.logError("Illegal character.", characterRange(inst.getArgPos(0).getStart()));
                return false;
            }
            return true;
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            return !verifyLabel(logger, inst, resolved, 0);
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitJumpInsn(inst.getOpcodeNum(), resolved.getLabel(inst.getArgValue(0, String.class)));
        }
    };

    public static final InstSpec LDC_INST_SPEC = new InstSpec(CONSTANTS_VALUE)
    {

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            boolean success = verifyVarSymbols(logger, inst, resolved, 0);
            if (success && inst.getArgExactType(0) == BasicParamType.FIELD_SIGNATURE)
            {
                TypeSignature sig = TypeSignature.parseTypeSig(inst.getArgValue(0, String.class));
                if (sig.getSort() != TypeSort.OBJECT && sig.getSort() != TypeSort.ARRAY)
                {
                    logger.logError("Must be object/array class type.", inst.getArgPos(0));
                    return false;
                }
            }
            return success;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitLdcInsn(unmarshall(inst, resolved, 0));
        }
    };

    public static final InstSpec LOOKUP_SWITCH_INST_SPEC = new InstSpec(IDENTIFIER /*,...*/)
    {

        @Override
        public Argument[] parseInstArgs(CodeTokenReader reader)
        {
            boolean error = false;
            ArrayList<Argument> args = new ArrayList<>();
            Argument[] ret = super.parseInstArgs(reader);
            if (ret == null)
                return null;
            Collections.addAll(args, ret);
            while (reader.nextArgument())
            {
                if (error = reader.hasTokenError())
                    break;
                if (!INTEGER.matches(reader))
                {
                    reader.errorExpected(INTEGER.getName());
                    error = true;
                    break;
                }

                if (INTEGER.checkToken(reader))
                {
                    if (error = reader.hasTokenError())
                        break;
                    args.add(new Argument(reader, INTEGER));
                }

                error = parseLabel(reader);
                if (error)
                    break;
                args.add(new Argument(reader, IDENTIFIER));
            }
            if (error)
            {
                //noinspection StatementWithEmptyBody
                while (reader.nextToken(true)) ;
                return null;
            }
            return args.toArray(new Argument[0]);
        }

        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            int argc = inst.getArgSize();
            for (int i = 0; i < argc; i += 2)
            {
                String name = inst.getArgValue(i, String.class);
                if (name.charAt(0) == '<') //Special method initializer
                {
                    logger.logError("Illegal character.", characterRange(inst.getArgPos(i).getStart()));
                    return false;
                }
            }

            for (int i = 3; i < argc; i+= 2)
            {
                if (inst.getIntArgValue(i) <= inst.getIntArgValue(i - 2))
                {
                    logger.logError("Match-label pairs must be in increasing order.",
                            characterRange(inst.getArgPos(i).getStart()));
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            int argc = inst.getArgSize();
            if (argc % 2 == 0)
                throw new IllegalArgumentException();

            for (int i = 0; i < argc; i += 2)
                if (verifyLabel(logger, inst, resolved, i))
                    return false;
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            int argc = inst.getArgSize() - 1;

            Label dflt = resolved.getLabel(inst.getArgValue(0, String.class));
            int[] matches = new int[argc / 2];
            Label[] labels = new Label[argc / 2];
            for (int i = 0; i < matches.length; i++)
            {
                matches[i] = inst.getIntArgValue(i * 2 + 1);
                labels[i] = resolved.getLabel(inst.getArgValue(i * 2 + 2, String.class));
            }
            writer.visitLookupSwitchInsn(dflt, matches, labels);
        }
    };

    public static final InstSpec TABLE_SWITCH_INST_SPEC = new InstSpec(INTEGER, INTEGER, IDENTIFIER /*,...*/)
    {
        private static final int OFFSET = 3;

        @Override
        public Argument[] parseInstArgs(CodeTokenReader reader)
        {
            ArrayList<Argument> args = new ArrayList<>();
            Argument[] ret = super.parseInstArgs(reader);
            if (ret == null)
                return null;
            Collections.addAll(args, ret);

            boolean error = false;
            int min = (Integer)args.get(0).getValue();
            int max = (Integer)args.get(1).getValue();
            if (max < min)
            {
                reader.error("High value must be greater or equal to low value.", args.get(1).getTokenPos());
                return null;
            }


            max -= min;
            for (int i = 0; i <= max; i++)
            {
                error = parseLabel(reader);
                if (error)
                    break;
                args.add(new Argument(reader, IDENTIFIER));
            }
            if (error)
            {
                //noinspection StatementWithEmptyBody
                while (reader.nextToken(true)) ;
                return null;
            }
            return args.toArray(new Argument[0]);
        }

        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            int argc = inst.getArgSize();
            for (int i = OFFSET - 1; i < argc; i++)
            {
                String name = inst.getArgValue(i, String.class);
                if (name.charAt(0) == '<') //Special method initializer
                {
                    logger.logError("Illegal character.", characterRange(inst.getArgPos(i).getStart()));
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            int argc = inst.getArgSize();
            for (int i = 2; i < argc; i++)
                if (verifyLabel(logger, inst, resolved, i))
                    return false;
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            int argc = inst.getArgSize() - OFFSET;

            int min = inst.getIntArgValue(0);
            int max = inst.getIntArgValue(1);
            Label dflt = resolved.getLabel(inst.getArgValue(2, String.class));
            Label[] labels = new Label[argc];
            for (int i = 0; i < argc; i++)
                labels[i] = resolved.getLabel(inst.getArgValue(i + OFFSET, String.class));
            writer.visitTableSwitchInsn(min, max, dflt, labels);
        }
    };

    public static final InstSpec TYPE_INST_SPEC = new InstSpec(FIELD_SIGNATURE)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            TypeSignature sig = TypeSignature.parseTypeSig(inst.getArgValue(0, String.class));
            if (inst.getOpcode() == InstOpcodes.INST_NEW)
            {
                if (sig.getSort() != TypeSort.OBJECT)
                {
                    logger.logError("Must be an object type.", inst.getArgPos(0));
                    return false;
                }
            }
            else if (!sig.isObject())
            {
                logger.logError("Must be an array or object type.", inst.getArgPos(0));
                return false;
            }
            return true;
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            TypeSignature sig = TypeSignature.parseTypeSig(inst.getArgValue(0, String.class));
            String unresolved = sig.getUnresolvedClassSymbols();
            if (unresolved != null)
            {
                logger.logError("Cannot resolve symbol(s) " + unresolved + ".", inst.getArgPos(2));
                return false;
            }
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitTypeInsn(inst.getOpcodeNum(), inst.getArgValue(0, String.class));
        }
    };

    public static final InstSpec METHOD_INST_SPEC = new InstSpec(CLASS_NAME, IDENTIFIER,
            BasicParamType.METHOD_SIGNATURE)
    {

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            ClassContext thisCtx = resolved.getThisContext();
            String name = inst.getArgValue(1, String.class);
            ClassContext owner = findContext(inst.getArgValue(0, String.class));
            TypeSignature typeSig = TypeSignature.parseTypeSig(inst.getArgValue(2, String.class));

            if (owner == null)
                throw new IllegalArgumentException();

            String unresolved = typeSig.getUnresolvedClassSymbols();
            if (unresolved != null)
            {
                logger.logError("Cannot resolve symbol(s) " + unresolved + ".", inst.getArgPos(2));
                return false;
            }

            MethodContext mth = owner.findMethod(name, typeSig);
            if (mth == null)
            {
                logger.logError("Cannot resolve symbol '" + name + "'.", inst.getArgPos(1));
                return false;
            }

            if (!thisCtx.checkAccessClass(owner))
            {
                logger.logError("Cannot access " + (owner.isInterface() ? "interface" : "class") +
                        " '" + owner.getName() + "'.", inst.getArgPos(0));
                return false;
            }

            if (!thisCtx.checkAccessMember(mth))
            {
                logger.logError("Cannot access method '" + name + "'.", inst.getArgPos(1));
                return false;
            }

            if (!owner.isInterface() && inst.getOpcodeNum() == Opcodes.INVOKEINTERFACE)
            {
                logger.logError("invokeinterface expects an interface to invoke on.", inst.getArgPos(0));
                return false;
            }

            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            String clsName = inst.getArgValue(0, String.class);
            ClassContext owner = findContext(clsName);
            writer.visitMethodInsn(inst.getOpcodeNum(), clsName, inst.getArgValue(1, String.class), inst.getArgValue
                    (2, String.class), owner.isInterface());

        }
    };

    public static final InstSpec VAR_INST_SPEC = new InstSpec(INTEGER)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            if (inst.getIntArgValue(0) < 0)
            {
                logger.logError("Expected: non-negative variable index", inst.getArgPos(0));
                return false;
            }
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitVarInsn(inst.getOpcodeNum(), inst.getIntArgValue(0));
        }
    };

    public static final InstSpec MULTIANEW_INST_SPEC = new InstSpec(ARRAY_SIGNATURE, INTEGER)
    {
        @Override
        public boolean verifyParse(ErrorLogger logger, InstStatement inst)
        {
            int dims = inst.getIntArgValue(1);
            TypeSignature sig = TypeSignature.parseTypeSig(inst.getArgValue(0, String.class));

            String unresolved = sig.getUnresolvedClassSymbols();
            if (unresolved != null)
            {
                logger.logError("Cannot resolve symbol(s) " + unresolved + ".", inst.getArgPos(0));
                return false;
            }

            if (dims <= 0)
            {
                logger.logError("Expected: positive number of dimensions", inst.getArgPos(0));
                return false;
            }

            if (dims > sig.getDimensions())
            {
                logger.logError("Expected: dimensions given must be less or equal to the number of dimensions in the" +
                        " type descriptor.", inst.getArgPos(0));
                return false;
            }

            return true;
        }

        @Override
        public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved)
        {
            TypeSignature typeSig = TypeSignature.parseTypeSig(inst.getArgValue(0, String.class));
            String unresolved = typeSig.getUnresolvedClassSymbols();
            if (unresolved != null)
            {
                logger.logError("Cannot resolve symbol(s) " + unresolved + ".", inst.getArgPos(0));
                return false;
            }
            return true;
        }

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitMultiANewArrayInsn(inst.getArgValue(0, String.class), inst.getIntArgValue(1));
        }
    };

    public static final InstSpec ZERO_OP_INST_SPEC = new InstSpec()
    {

        @Override
        public void write(MethodVisitor writer, InstStatement inst, CodeSymbols resolved)
        {
            writer.visitInsn(inst.getOpcodeNum());
        }
    };

    /**
     * This represents all the possible types of primitive arrays that can be created.
     */
    @SuppressWarnings("unused")
    private enum ArrayType
    {
        T_BOOLEAN(Opcodes.T_BOOLEAN), T_CHAR(Opcodes.T_CHAR), T_FLOAT(Opcodes.T_FLOAT), T_DOUBLE(Opcodes.T_DOUBLE),
        T_BYTE(Opcodes.T_BYTE), T_SHORT(Opcodes.T_SHORT), T_INT(Opcodes.T_INT), T_LONG(Opcodes.T_LONG);

        private static final Map<String, ArrayType> directory;

        static
        {
            directory = new HashMap<>();
            for (ArrayType t : ArrayType.values())
                directory.put(t.name(), t);
        }

        private int opcode;

        /**
         * Constructs an array type.
         * @param opcode the opcode corresponding to array-type.
         */
        ArrayType(int opcode)
        {
            this.opcode = opcode;
        }
    }

    /**
     * Verifies that the label is valid.
     * @param logger the error logger.
     * @param inst the instruction being verified
     * @param resolved the resolved symbols
     * @param ind the index of argument
     * @return true if error occurs, false if no error occurs.
     */
    private static boolean verifyLabel(ErrorLogger logger, InstStatement inst, CodeSymbols resolved, int ind)
    {
        String name = inst.getArgValue(ind, String.class);
        if (!resolved.containsLabel(name))
        {
            logger.logError("Cannot resolve label: '" + name + "'.", inst.getArgPos(ind));
            return true;
        }
        return false;
    }

    /**
     * Verifies that certain symbols are loaded (those with variant types). This will emit any errors if necessary.
     * @param logger the error logger
     * @param inst the instruction being verified.
     * @param resolved the resolved symbol
     * @param ind the argument index.
     * @return true if no errors occurred, false if errors occurred.
     */
    private static boolean verifyVarSymbols(ErrorLogger logger, InstStatement inst, CodeSymbols resolved, int ind)
    {
        ParamType type = inst.getArgExactType(ind);
        if (!(type instanceof BasicParamType))
            throw new IllegalArgumentException();
        switch ((BasicParamType)type)
        {
            case METHOD_HANDLE:
                String name = inst.getArgValue(ind, String.class);
                if (!resolved.containsHandle(name))
                {
                    logger.logError("Cannot resolve method handle: '" + name + "'.", inst.getArgPos(ind));
                    return false;
                }
                break;
            case METHOD_SIGNATURE:
            case FIELD_SIGNATURE:
                TypeSignature typeSig = TypeSignature.parseTypeSig(inst.getArgValue(ind, String.class));
                String unresolved = typeSig.getUnresolvedClassSymbols();
                if (unresolved != null)
                {
                    logger.logError("Cannot resolve symbol(s) " + unresolved + ".", inst.getArgPos(ind));
                    return false;
                }
        }
        return true;
    }

    /**
     * Unmarshalls an argument's type signatures and method handle (if needed) from a string.
     *
     * @param inst the instruction to unmarshall
     * @param resolved the resolved symbols
     * @param ind the argument index to unmarshall
     * @return the unmarshalled object.
     */
    private static Object unmarshall(InstStatement inst, CodeSymbols resolved, int ind)
    {
        ParamType type = inst.getArgExactType(ind);
        if (!(type instanceof BasicParamType))
            throw new IllegalArgumentException();
        switch ((BasicParamType)type)
        {
            case FIELD_SIGNATURE:
            case METHOD_SIGNATURE:
                return Type.getType(inst.getArgValue(ind, String.class));
            case METHOD_HANDLE:
                return resolved.getHandle(inst.getArgValue(ind, String.class));
            default:
                return inst.getArgValue(ind);
        }
    }

    /**
     * Parses this label checking whether if an error occurred.
     * @param reader the token reader.
     * @return true if no errors occurred, false if errors occurred.
     */
    private static boolean parseLabel(CodeTokenReader reader)
    {
        if (!reader.nextArgument())
        {
            reader.errorExpected("jump label identifier");
            return true;
        }
        if (reader.hasTokenError())
            return true;
        if (!IDENTIFIER.matches(reader))
        {
            reader.errorExpected("jump label identifier");
            return true;
        }
        return !IDENTIFIER.checkToken(reader);
    }

}
