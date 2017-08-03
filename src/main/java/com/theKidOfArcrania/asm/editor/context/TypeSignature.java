package com.theKidOfArcrania.asm.editor.context;


import java.util.*;
import java.util.stream.Collectors;

import static com.theKidOfArcrania.asm.editor.context.TypeSort.*;

/**
 * Represents any type that can be expressed as an internal descriptor used to describe class types and field/method
 * signatures.
 * @author Henry Wang
 */
public class TypeSignature
{

    public static TypeSignature BOOLEAN_TYPE = new TypeSignature(BOOLEAN);
    public static TypeSignature BYTE_TYPE = new TypeSignature(BYTE);
    public static TypeSignature CHAR_TYPE = new TypeSignature(CHAR);
    public static TypeSignature DOUBLE_TYPE = new TypeSignature(DOUBLE);
    public static TypeSignature FLOAT_TYPE = new TypeSignature(FLOAT);
    public static TypeSignature INTEGER_TYPE = new TypeSignature(INTEGER);
    public static TypeSignature LONG_TYPE = new TypeSignature(LONG);
    public static TypeSignature SHORT_TYPE = new TypeSignature(SHORT);
    public static TypeSignature VOID_TYPE = new TypeSignature(VOID);

    private static EnumMap<TypeSort, TypeSignature> primTypes;

    static {
        primTypes = new EnumMap<>(TypeSort.class);
        TypeSignature types[] = {BOOLEAN_TYPE, BYTE_TYPE, CHAR_TYPE, DOUBLE_TYPE, FLOAT_TYPE, INTEGER_TYPE,
                LONG_TYPE, SHORT_TYPE, VOID_TYPE};
        for (TypeSignature sig : types)
            primTypes.put(sig.getSort(), sig);
    }

    /**
     * Parses the signature descriptor and returns the resulting type signature.
     * @param descriptor the descriptor to test against
     * @return the {@code TypeSignature} corresponding to this signature descriptor.
     */
    public static TypeSignature parseTypeSig(String descriptor)
    {
        if (descriptor == null || descriptor.isEmpty())
            return null;

        TypeParser parser = new TypeParser(descriptor);
        TypeSort ts = parser.nextTypeSort();

        if (ts == METHOD)
        {
            ArrayList<TypeSignature> params = new ArrayList<>();
            while (!parser.isEndingParameter())
            {
                TypeSignature p = parseElement(parser, parser.nextTypeSort());
                if (p == null || p.getSort() == TypeSort.VOID)
                    return null;
                params.add(p);
            }
            TypeSignature ret = parseElement(parser, parser.nextTypeSort());
            if (ret == null)
                return null;
            if (!parser.isEnding())
                return null;
            return new TypeSignature(params.toArray(new TypeSignature[0]), ret);
        }
        else
        {
            TypeSignature ret = parseElement(parser, ts);
            if (!parser.isEnding())
                return null;
            return ret;
        }
    }

    /**
     * Parsers an element of this type signature.
     * @param parser the parser containing the type descriptor.
     * @param ts the type sort of this element
     * @return the parsed <code>TypeSignature</code> object representing this element.
     */
    private static TypeSignature parseElement(TypeParser parser, TypeSort ts)
    {
        if (ts == null)
            return null;
        if (primTypes.containsKey(ts))
            return primTypes.get(ts);

        switch (ts)
        {
            case ARRAY:
                TypeSignature ele = parseElement(parser, parser.nextTypeSort());
                if (ele == null)
                    return null;
                return new TypeSignature(ele, parser.getDimensions());
            case OBJECT:
                return new TypeSignature(parser.getClassDescriptor());
            default: //METHOD
                return null;
        }
    }


    private final TypeSort sort;

    //Array-types
    private TypeSignature elem;
    private int dim;

    //Object-types
    private String classDescriptor;

    //Method-types
    private TypeSignature[] parameterTypes;
    private TypeSignature returnType;

    /**
     * Constructor for primitive type signatures.
     * @param prim the primitive type
     */
    private TypeSignature(TypeSort prim)
    {
        if (prim == ARRAY || prim == OBJECT || prim == METHOD)
            throw new IllegalArgumentException();
        this.sort = prim;
    }

    /**
     * Constructor for array type signatures.
     * @param elem the tye of the elements for this array type.
     * @param dim the number of dimensions
     */
    private TypeSignature(TypeSignature elem, int dim)
    {
        sort = ARRAY;
        this.elem = elem;
        this.dim = dim;
    }

    /**
     * Constructor for object type signatures.
     * @param classDescript the associated class descriptor.
     */
    private TypeSignature(String classDescript)
    {
        sort = TypeSort.OBJECT;
        this.classDescriptor = classDescript;
    }

    /**
     * Constructor for method type signatures.
     * @param parameterTypes the array of parameter types.
     * @param returnType the return type
     */
    private TypeSignature(TypeSignature parameterTypes[], TypeSignature returnType)
    {
        sort = TypeSort.METHOD;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public TypeSort getSort()
    {
        return sort;
    }

    public TypeSignature getElementType()
    {
        if (sort != ARRAY)
            throw new IllegalStateException("Not an array type.");
        return elem;
    }

    public int getDimensions()
    {
        if (sort != ARRAY)
            throw new IllegalStateException("Not an array type.");
        return dim;
    }

    public String getClassDescriptor()
    {
        if (sort != OBJECT)
            throw new IllegalStateException("Not an object type.");
        return classDescriptor;
    }

    public TypeSignature[] getParameterTypes()
    {
        return parameterTypes.clone();
    }

    public TypeSignature getReturnType()
    {
        return returnType;
    }

    /**
     * Obtains a list of unresolved class symbols. If all classes referenced are resolved, this will return
     * <code>null</code>.
     * @return a string list of unresolved classes, if any.
     */
    public String getUnresolvedClassSymbols()
    {
        switch (getSort())
        {
            case ARRAY:
                return getElementType().getUnresolvedClassSymbols();
            case OBJECT:
                return ClassContext.findContext(getClassDescriptor()) == null ? getClassDescriptor() : null;
            case METHOD:
                TreeSet<String> unresolved = new TreeSet<>();
                String tmp = getReturnType().getUnresolvedClassSymbols();
                if (tmp != null)
                    unresolved.add(tmp);
                for (TypeSignature param : getParameterTypes())
                {
                    tmp = param.getUnresolvedClassSymbols();
                    if (tmp != null)
                        unresolved.add(tmp);
                }
                if (unresolved.isEmpty())
                    return null;
                return unresolved.stream().map(str -> "'" + str + "'").collect(Collectors.joining(", "));
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeSignature that = (TypeSignature) o;

        if (sort != that.sort) return false;
        switch (sort)
        {
            case ARRAY:
                return dim == that.dim && elem.equals(that.elem);
            case OBJECT:
                return classDescriptor.equals(that.classDescriptor);
            case METHOD:
                return Arrays.equals(parameterTypes, that.parameterTypes) && returnType.equals(that.returnType);
            default:
                return true;
        }
    }

    @Override
    public int hashCode()
    {
        int result = 13 + sort.hashCode();
        switch (sort)
        {
            case ARRAY:
                result = 31 * result + elem.hashCode();
                result = 31 * result + dim;
                break;
            case OBJECT:
                result = 31 * result + classDescriptor.hashCode();
            case METHOD:
                result = 31 * result + Arrays.hashCode(parameterTypes);
                result = 31 * result + returnType.hashCode();
        }
        return result;
    }
}