package com.theKidOfArcrania.asm.editor.code.parsing.frame;


import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstStatement;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstOpcodes;
import com.theKidOfArcrania.asm.editor.context.ClassContext;
import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import com.theKidOfArcrania.asm.editor.context.TypeSort;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;

import static com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameType.OBJECT;
import static com.theKidOfArcrania.asm.editor.code.parsing.frame.FrameType.UNINITIALIZED;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.parseTypeSig;

/**
 * Represents a single frame element.
 * @author Henry Wang
 */
public class FrameElement
{
    private static final FrameType[] nameMarkers = new FrameType[Byte.MAX_VALUE];

    static
    {
        for (FrameType s : FrameType.values())
            nameMarkers[s.getMarker()] = s;
    }

    /**
     * Parses the frame descriptor and returns the list of frame elements.
     * @param desc the descriptor to test against
     * @return the {@code FrameElement} corresponding to this frame descriptor.
     */
    public static FrameElement[] parseFrameDesc(String desc)
    {
        if (desc == null || desc.isEmpty())
            return new FrameElement[0];

        ArrayList<FrameElement> eles = new ArrayList<>();
        char[] buff = desc.toCharArray();
        loop: for (int i = 0; i < buff.length; i++)
        {
            FrameType type = nameMarkers[buff[i]];
            if (type == null)
                return null;

            int start = i;
            switch (type)
            {
                case ARRAY:
                    i = findArrNext(buff, start);
                    type = OBJECT;
                    break;
                case OBJECT:
                case UNINITIALIZED:
                    i = findObjNext(buff, start);
                    break;
                default:
                    eles.add(new FrameElement(type));
                    continue loop;
            }

            if (i == -1)
                return null;
            String name = new String(buff, start, i - start);
            if (type == UNINITIALIZED)
                eles.add(new FrameElement(type, name));
            else
                eles.add(new FrameElement(type, parseTypeSig(name)));
            i--;
        }
        return eles.toArray(new FrameElement[0]);
    }

    /**
     * Finds the end of this array signature. If it fails, it will return <code>-1</code>.
     * @param buff the character buffer
     * @param start the offset to search from.
     * @return the first index that is not part of this array signature, or -1 if failed.
     */
    private static int findArrNext(char[] buff, int start)
    {
        while (start < buff.length)
        {
            switch (buff[start])
            {
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 'Z':
                    return start + 1;
                case '[':
                    break;
                case 'L':
                    return findObjNext(buff, start);
                default:
                    return -1;
            }
            start++;
        }
        return -1;
    }

    /**
     * Finds the end of this object signature. If it fails, it will return <code>-1</code>.
     * @param buff the character buffer
     * @param start the offset to search from.
     * @return the first index that is not part of this object signature, or -1 if failed.
     */
    private static int findObjNext(char[] buff, int start)
    {

        int i = start;
        while (i < buff.length)
        {
            if (buff[i++] == ';')
            {
                String desc = new String(buff, start + 1, i - start - 2);
                return ClassContext.verifyClassNameFormat(desc) ? i : -1;
            }
        }
        return -1;
    }

    private final FrameType type;
    private TypeSignature refSig;
    private boolean block;

    private String label;
    private InstStatement inst;

    /**
     * Creates a frame element for following frame types: TOP, INTEGER, LONG, FLOAT, DOUBLE, NULL, and
     * UNINITIALIZED_THIS.
     * @param type the frame type to create.
     * @throws IllegalArgumentException if a frame element of UNINITIALIZED or OBJECT is attempted to be created.
     */
    public FrameElement(FrameType type)
    {
        if (type == FrameType.UNINITIALIZED)
            throw new IllegalArgumentException("Expected label argument.");
        if (type == OBJECT)
            throw new IllegalArgumentException("Expected object type.");
        this.type = type;
    }

    /**
     * Creates a frame element that is blocked together with a size-2 block.
     * @param type must be TOP.
     * @param block must be true.
     * @throws IllegalArgumentException if type is not TOP or block is not true.
     */
    public FrameElement(FrameType type, boolean block)
    {
        if (!block)
            throw new IllegalArgumentException("block MUST be true.");
        if (type != FrameType.TOP)
            throw new IllegalArgumentException("block MUST be TOP.");
        this.type = type;
        this.block = true;
    }

    /**
     * Creates a UNINITIALIZED frame element from a label string.
     * @param type must be UNINITIALIZED.
     * @param label the label associated with the NEW instruction that created this object.
     * @throws IllegalArgumentException if type is not UNINITIALIZED.
     */
    public FrameElement(FrameType type, String label)
    {
        if (type != FrameType.UNINITIALIZED)
            throw new IllegalArgumentException("Must be UNINITIALIZED frame-type.");
        this.type = type;
        this.label = label;
    }

    /**
     * Creates a UNINITIALIZED frame element from an instruction.
     * @param type must be UNINITIALIZED.
     * @param inst the instruction associated with the NEW instruction that created this object.
     * @throws IllegalArgumentException if type is not UNINITIALIZED, or if inst is not the NEW opcode.
     */
    public FrameElement(FrameType type, InstStatement inst)
    {
        if (type != FrameType.UNINITIALIZED)
            throw new IllegalArgumentException("Must be UNINITIALIZED frame-type.");
        if (inst.getOpcode() != InstOpcodes.INST_NEW)
            throw new IllegalArgumentException("Statement must be a NEW instruction");
        this.type = type;
        this.inst = inst;
    }

    /**
     * Creates a OBJECT frame element.
     * @param type must be OBJECT.
     * @param refSig the object type expected, must be an ARRAY or OBJECT sort.
     * @throws IllegalArgumentException if type is not OBJECT.
     * @throws IllegalArgumentException if the refSig is a METHOD or a primitive type-signature
     */
    public FrameElement(FrameType type, TypeSignature refSig)
    {
        if (type != OBJECT)
            throw new IllegalArgumentException("Must be OBJECT frame-type.");
        if (refSig.getSort() != TypeSort.ARRAY && refSig.getSort() != TypeSort.OBJECT)
            throw new IllegalArgumentException("Reference type must be an ARRAY or OBJECT");
        this.type = type;
        this.refSig = refSig;
    }

    public boolean isBlock()
    {
        return block;
    }

    /**
     * Removes the flag indicating this as a blocked segment.
     */
    public void unblock()
    {
        this.block = false;
    }

    public FrameType getType()
    {
        return type;
    }

    public String getLabelName()
    {
        if (type != FrameType.UNINITIALIZED)
            throw new IllegalArgumentException("Not an UNINITIALIZED frame-type.");
        return label;
    }

    public InstStatement getStatement()
    {
        if (type != FrameType.UNINITIALIZED)
            throw new IllegalArgumentException("Not an UNINITIALIZED frame-type.");
        return inst;
    }

    public TypeSignature getRefSig()
    {
        if (type != OBJECT)
            throw new IllegalArgumentException("Not an OBJECT frame-type.");
        return refSig;
    }

    /**
     * Resolves a label from its code symbols.
     * @param resolved the resolved code symbols
     * @return the resolved label.
     */
    public Label resolveLabel(CodeSymbols resolved)
    {
        if (type != FrameType.UNINITIALIZED)
            throw new IllegalArgumentException("Not an UNINITIALIZED frame-type.");
        return label == null ? resolved.findStatementLabel(inst) : resolved.getLabel(label);
    }

    /**
     * Unwraps this frame element into it's object component type, i.e. the value that should be passed to
     * {@link MethodVisitor#visitFrame(int, int, Object[], int, Object[])}.
     * @param resolved the resolved code symbols
     * @return the object form of this element
     */
    public Object unwrapElement(CodeSymbols resolved)
    {
        switch (type)
        {
            case UNINITIALIZED:
                return resolveLabel(resolved);
            case OBJECT:
                TypeSignature sig = getRefSig();
                return sig.getSort() == TypeSort.ARRAY ? sig.toString() : sig.getClassDescriptor();
            default:
                return type.getOpcode();
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 13 + type.hashCode();
        switch (type)
        {
            case OBJECT:
            case ARRAY:
                return hash * 31 + refSig.hashCode();
            case UNINITIALIZED:
                return hash * 31 + (label == null ? inst.hashCode() : label.hashCode());
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj.getClass() != getClass())
            return false;
        FrameElement e = (FrameElement)obj;
        if (type != e.type)
            return false;

        switch (type)
        {
            case OBJECT:
            case ARRAY:
                return refSig.equals(e.refSig);
            case UNINITIALIZED:
                return label.equals(e.label) && inst.equals(e.inst);
            default:
                return true;
        }
    }
}
