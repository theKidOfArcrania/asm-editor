package com.theKidOfArcrania.asm.editor.context;



import com.theKidOfArcrania.asm.editor.code.parsing.FallibleFunction;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Represents a context of a particular class.
 * @author Henry Wang
 */
public class ClassContext
{
    public static final ClassContext OBJECT_CONTEXT;
    private static final HashMap<String, ClassContext> CLASS_CONTEXT_MAP;

    static
    {
        CLASS_CONTEXT_MAP = new HashMap<>();
        OBJECT_CONTEXT = ClassContext.findContextFromClass(Object.class); //Make sure nothing overrides the Object class
    }

    /**
     * Obtains the class internal name for the specified class. This will only work with classes or interfaces.
     * @param cls the class to query.
     * @return the internal name.
     * @throws IllegalArgumentException if the class does not describe a class or interface.
     */
    public static String getInternalName(Class<?> cls)
    {
        if (cls.isPrimitive() || cls.isArray() || cls.isAnnotation())
            throw new IllegalArgumentException("Not a class or interface");
        return cls.getName().replace('.', '/');
    }

    /**
     * Finds the associated class context from this class. If it does not already exist, it will add the class to the
     * list of class contexts.
     * @param cls the class object to search from.
     * @return the class context created/found
     */
    public static ClassContext findContextFromClass(Class<?> cls)
    {
        if (cls == null)
            return null;

        String name = getInternalName(cls);
        return CLASS_CONTEXT_MAP.computeIfAbsent(name, n -> createNewContextFromClass(cls, n));
    }

    /**
     * Finds the associated class context with this name. Note that this will not check whether if the name is in a
     * valid class identifier format. If this is not found, it will first attempt to search for an existing loaded
     * class before returning <code>null</code>.
     * @param name the internal name of the class context.
     * @return the class context if found.
     */
    public static ClassContext findContext(String name)
    {
        ClassContext existing = CLASS_CONTEXT_MAP.get(name);
        if (existing == null)
            return FallibleFunction.tryOptional(Class::forName, name.replace('/', '.'))
                    .map(ClassContext::findContextFromClass).orElse(null);
        return existing;
    }

    /**
     * Creates a blank new class context.
     * @param name the internal name of the class.
     * @param itrf whether if this is an interface.
     * @throws IllegalArgumentException if a super class is passed for interfaces.
     * @throws IllegalStateException if such a class context already exists.
     * @return the create class context.
     */
    public static ClassContext createContext(String name, boolean itrf)
    {
        if (CLASS_CONTEXT_MAP.containsKey(name))
            throw new IllegalArgumentException("Class context '" + name + "' already exists.");
        ClassContext ctx = new ClassContext(name, itrf);
        CLASS_CONTEXT_MAP.put(name, ctx);
        return ctx;
    }

    /**
     * Creates a new class context initializing it with some information.
     * @param name the internal name of the class.
     * @param modifiers the access modifiers if any.
     * @param outer the outer class that this class context is in.
     * @param superClass the super class of this class (must be null for interfaces). If null,
     *              {@link Object} assumed.
     * @param interfaces the number of interfaces that this class explicitly extends/ implements
     * @return the created class context.
     * @throws IllegalArgumentException if a super class is passed for interfaces.
     * @throws IllegalStateException if such a class context already exists.
     */
    public static ClassContext createContext(String name, int modifiers, ClassContext outer, ClassContext superClass,
                                             ClassContext[] interfaces)
    {
        if (CLASS_CONTEXT_MAP.containsKey(name))
            throw new IllegalArgumentException("Class context '" + name + "' already exists.");
        ClassContext ctx = new ClassContext(name, Modifier.isInterface(modifiers));
        ctx.setOuterClass(outer);
        ctx.setModifiers(modifiers);
        ctx.setSuperClass(superClass);
        for (ClassContext itrf : interfaces)
            ctx.addInterface(itrf);

        CLASS_CONTEXT_MAP.put(name, ctx);
        return ctx;
    }

    /**
     * Creates a new class context from an existing class object.
     * @param cls the class object to create from.
     * @param name the internal name of the class.
     * @return a new class-context.
     */
    private static ClassContext createNewContextFromClass(Class<?> cls, String name)
    {
        ClassContext enclosing = findContextFromClass(cls.getEnclosingClass());
        ClassContext superClass = findContextFromClass(cls.getSuperclass());

        ClassContext ctx = new ClassContext(name, cls.isInterface());
        ctx.setOuterClass(enclosing);
        ctx.setModifiers(cls.getModifiers());
        if (superClass != null)
            ctx.setSuperClass(superClass);

        Class[] itrfs = cls.getInterfaces();
        for (Class itrf : itrfs)
            ctx.addInterface(findContextFromClass(itrf));
        return ctx;
    }

    /**
     * Verifies whether the specified class name is in a valid format.
     * @param internalName the internal name of the class.
     * @return true if a valid format, false otherwise.
     */
    public static boolean verifyClassNameFormat(String internalName)
    {
        try
        {
            ensureClassNameFormat(internalName);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    /**
     * Ensures that the specified class name is in a valid format.
     * @param internalName the internal name of the class.
     * @throws IllegalArgumentException if it is not in a valid class format.
     */
    private static void ensureClassNameFormat(String internalName)
    {
        if (internalName.isEmpty())
            throw new IllegalArgumentException("Empty class identifier.");

        char[] name = internalName.toCharArray();
        boolean beginning = true;
        for (char ch : name)
        {
            if (beginning)
            {
                if (Character.isJavaIdentifierStart(ch))
                    beginning = false;
                else if (ch == '/')
                    throw new IllegalArgumentException("Class divider/ slash with no identifier.");
                else
                    throw new IllegalArgumentException("Illegal character '" + ch + "' in class identifier.");
            }
            else if (!Character.isJavaIdentifierPart(ch))
            {
                if (ch == '/')
                    beginning = true;
                else
                    throw new IllegalArgumentException("Illegal character '" + ch + "' in class identifier.");
            }
        }

        if (beginning)
            throw new IllegalArgumentException("Class divider/ slash with no identifier.");
    }

    private String name;
    private int modifiers;
    private ClassContext superClass;
    private ClassContext outer;
    private Set<ClassContext> interfaces;

    private Map<FieldContext, FieldContext> fields;
    private Map<MethodContext, MethodContext> methods;


    /**
     * Creates a class context that is an inner class (can be anonymous)
     * @param name the internal name of the class.
     * @param itrf whether to create an interface or class.
     * @throws IllegalArgumentException if a super class is passed for interfaces, if the super class is an
     * interface, or if one of the interfaces is a class
     */
    private ClassContext(String name, boolean itrf)
    {
        ensureClassNameFormat(name);
        boolean isObject = name.equals("java/lang/Object");
        if (isObject && itrf)
            throw new IllegalArgumentException("Cannot change interface-bit of java.lang.Object");

        this.modifiers = Modifier.PUBLIC;
        if (itrf)
            this.modifiers |= Modifier.INTERFACE;
        this.name = name;
        this.outer = null;
        this.superClass = (itrf || isObject) ? null : OBJECT_CONTEXT;
        this.interfaces = new HashSet<>();

        fields = new HashMap<>();
        methods = new HashMap<>();
    }

    public AccessModifier getAccessModifier()
    {
        return AccessModifier.getAccessModifier(getModifiers());
    }

    public String getName()
    {
        return name;
    }

    /**
     * Renames this class, and performs the respective refactoring in the master list of class contexts.
     * @param name the new internal class name to change it to.
     * @throws IllegalArgumentException if the new name is not in a valid class identifier format.
     * @throws IllegalStateException if this does not exist in the master list of class contexts.
     */
    public void rename(String name)
    {
        ensureClassNameFormat(name);
        if (CLASS_CONTEXT_MAP.remove(this.name) != this)
            throw new IllegalStateException("Not found in master list.");
        CLASS_CONTEXT_MAP.put(this.name = name, this);
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public void setModifiers(int modifiers)
    {
        if (outer == null && (Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers)))
            throw new IllegalArgumentException("Top-level class cannot be protected or private.");
        if (Modifier.isInterface(modifiers) ^ Modifier.isInterface(this.modifiers))
        {
            if (Modifier.isInterface(modifiers) && name.equals("java/lang/Object"))
                throw new IllegalArgumentException("Cannot change interface-bit of java.lang.Object");
            superClass = Modifier.isInterface(modifiers) ? null : OBJECT_CONTEXT;
        }

        this.modifiers = modifiers;
    }

    /**
     * Sets individual bits of the modifier. This means that the user doesn't have to query the entire modifier if
     * only one bit or bits needs to be set. Underneath, this will still call the {@link #setModifiers(int)} method
     * for the necessary checks.
     *
     * @param modifierBitsSet the bits to set
     * @param modifierBits the value to set these bits
     */
    public void setModifierBits(int modifierBitsSet, int modifierBits)
    {
        int modifiers = this.modifiers & ~modifierBitsSet;
        modifiers |= (modifierBitsSet & modifierBits);
        setModifiers(modifiers);
    }

    public ClassContext getSuperClass()
    {
        return superClass;
    }

    /**
     * Sets the super class that this class extends. If <code>superClass</code> is null, the
     * <code>java.lang.Object</code> class context is assumed.
     * @param superClass the new super class to extend from.
     * @throws IllegalStateException if this is called on the <code>java.lang.Object</code> class context, or on an
     * interface.
     * @throws IllegalArgumentException if the passed class context is an interface.
     */
    public void setSuperClass(ClassContext superClass)
    {
        if (name.equals("java/lang/Object"))
            throw new IllegalStateException("Cannot set the super class of java/lang/Object.");
        if (isInterface())
            throw new IllegalStateException("Cannot set the super class of an interface.");
        if (superClass != null && superClass.isInterface())
            throw new IllegalArgumentException("Expected a regular class context (not interface).");

        this.superClass = superClass == null ? OBJECT_CONTEXT : superClass;
    }

    public ClassContext getOuterClass()
    {
        return outer;
    }

    public void setOuterClass(ClassContext outer)
    {
        this.outer = outer;
    }

    /**
     * Adds an interface for this class/interface to implement/extend.
     * @param itrf the interface to add.
     * @return true if this is added, false if not added.
     * @throws IllegalArgumentException if the passed class context is not an interface.
     */
    public boolean addInterface(ClassContext itrf)
    {
        Objects.requireNonNull(itrf);
        if (!itrf.isInterface())
            throw new IllegalArgumentException("Expected an interface.");
        return interfaces.add(itrf);
    }

    /**
     * Removes an interface from this class/interface.
     * @param itrf the interface to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeInterface(ClassContext itrf)
    {
        return interfaces.remove(itrf);
    }

    public Set<ClassContext> getInterfaces()
    {
        return new HashSet<>(interfaces);
    }

    /**
     * Removes all interfaces, if any.
     */
    public void removeAllInterfaces()
    {
        interfaces.clear();
    }

    /**
     * Adds a method to this class/interface. If a new method context is added as a result of this call, it will return
     * the newly created method.
     * @param modifiers the modifiers of new method.
     * @param name the name of the method
     * @param signature the type signature
     * @return the newly created method, if added.
     */
    public MethodContext addMethod(int modifiers, String name, TypeSignature signature)
    {
        MethodContext mth = new MethodContext(this, modifiers, name, signature);
        if (methods.putIfAbsent(mth, mth) == null)
            return mth;
        else
            return null;
    }

    /**
     * Removes a method from this class/interface.
     * @param mth the method to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeMethod(MethodContext mth)
    {
        return methods.remove(mth) != null;
    }

    /**
     * Finds the corresponding method from its name and type signature. If this is not found, it will return null.
     * @param name the name of the method.
     * @param signature the signature of the method.
     * @return a valid method context if found.
     * @throws IllegalArgumentException if an invalid name/signature is given.
     */
    public MethodContext findMethod(String name, TypeSignature signature)
    {
        return methods.get(new MethodContext(this, 0, name, signature));
    }

    /**
     * Renames the specified method into another name. This is a convenience method for
     * {@link #renameMethod(MethodContext, String, TypeSignature)}.
     * @param mth the method to rename.
     * @param newName the new name to set.
     * @throws IllegalArgumentException if the specified method is not owned by this class
     * @throws IllegalArgumentException if method name is not a valid identifier (can be &lt;init&gt; or &lt;
     *   clinit&gt;) or if type signature is not for a method.
     * @return true if rename is successful, otherwise returns false.
     */
    public boolean renameMethod(MethodContext mth, String newName)
    {
        return renameMethod(mth, newName, mth.getSignature());
    }

    /**
     * Renames the specified method into another name and type signature. Note that this can only rename methods that it
     * owns. Any instances of code that references this method context (context only) will automatically be
     * refactored. If the new name and new signature are the same, or if an existing method conflicts with the new
     * name/signature, this will return <code>false</code>. Otherwise, this will return <code>true</code>.
     * @param mth the method to rename.
     * @param newName the new name to set.
     * @param newSignature the new signature to set.
     * @throws IllegalArgumentException if the specified method is not owned by this class.
     * @throws IllegalArgumentException if method name is not a valid identifier (can be &lt;init&gt; or &lt;
     *   clinit&gt;) or if type signature is not for a method.
     * @return true if rename is successful, otherwise returns false.
     */
    public boolean renameMethod(MethodContext mth, String newName, TypeSignature newSignature)
    {
        if (mth.getOwner() != this)
            throw new IllegalArgumentException("Method context does not belong to this class context.");

        MethodContext test = new MethodContext(this, mth.getModifiers(), newName, newSignature);
        if (test.equals(mth))
            return false;
        if (methods.containsKey(test))
            return false;
        methods.remove(mth);
        mth.renameTo(test);
        methods.put(mth, mth);
        return true;
    }

    public Set<MethodContext> getMethods()
    {
        return new HashSet<>(methods.keySet());
    }

    /**
     * Removes all methods, if any.
     */
    public void removeAllMethods()
    {
        methods.clear();
    }

    /**
     * Adds a field to this class/interface. If a new field context is added as a result of this call, it will return
     * the newly created field.
     * @param modifiers the modifiers of new field.
     * @param name the name of the field
     * @param signature the type for the field.
     * @return the newly created field, if added.
     */
    public FieldContext addField(int modifiers, String name, TypeSignature signature)
    {
        FieldContext fld = new FieldContext(this, modifiers, name, signature);
        if (fields.putIfAbsent(fld, fld) == null)
            return fld;
        else
            return null;
    }

    /**
     * Removes a field from this class/interface.
     * @param fld the field to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeField(FieldContext fld)
    {
        return fields.remove(fld) != null;
    }

    /**
     * Finds the corresponding field from its name. If this is not found, it will return null.
     * @param name the name of the field
     * @return a valid field context if found.
     * @throws IllegalArgumentException if an invalid name is given.
     */
    public FieldContext findField(String name)
    {
        return fields.get(new FieldContext(this, 0, name, TypeSignature.BOOLEAN_TYPE));
    }

    /**
     * Renames the specified field into another name. This is a convenience method for
     * {@link #renameField(FieldContext, String, TypeSignature)}.
     * @param fld the field to rename.
     * @param newName the new name to set.
     * @throws IllegalArgumentException if the specified field is not owned by this class.
     * @throws IllegalArgumentException if field name is not a valid identifier or if type signature is not for a field.
     * @return true if rename is successful, otherwise returns false.
     */
    public boolean renameField(FieldContext fld, String newName)
    {
        return renameField(fld, newName, fld.getSignature());
    }

    /**
     * Renames the specified field into another name and type signature. Note that this can only rename fields that it
     * owns. Any instances of code that references this field context (context only) will automatically be refactored.
     * If the new name is the same, or if an existing field conflicts with the new name, this will return
     * <code>false</code>. Otherwise, this will return <code>true</code>.
     * @param fld the method to rename.
     * @param newName the new name to set.
     * @param newSignature the new signature to set.
     * @throws IllegalArgumentException if the specified field is not owned by this class.
     * @throws IllegalArgumentException if field name is not a valid identifier or if type signature is not for a field.
     * @return true if rename is successful, otherwise returns false.
     */
    public boolean renameField(FieldContext fld, String newName, TypeSignature newSignature)
    {
        if (fld.getOwner() != this)
            throw new IllegalArgumentException("Field context does not belong to this class context.");

        FieldContext test = new FieldContext(this, fld.getModifiers(), newName, newSignature);
        if (test.equals(fld))
            return false;
        if (fields.containsKey(test))
            return false;
        fields.remove(fld);
        fld.renameTo(test);
        fields.put(fld, fld);
        return true;
    }

    public Set<FieldContext> getFields()
    {
        return new HashSet<>(fields.keySet());
    }

    /**
     * Removes all fields, if any.
     */
    public void removeAllFields()
    {
        fields.clear();
    }

    /**
     * Obtains the chain of outer classes over this class, if any. This list will always contain this class context.
     * @return an array of the chain of outer classes, starting with this class context.
     */
    public ClassContext[] getOuterClassChain()
    {
        ArrayList<ClassContext> classChain = new ArrayList<>();
        ClassContext tmp = this;
        while(tmp != null)
        {
            classChain.add(tmp);
            tmp = tmp.getOuterClass();
        }
        return classChain.toArray(new ClassContext[0]);
    }

    public boolean isInterface()
    {
        return Modifier.isInterface(modifiers);
    }

    /**
     * Determines whether if the class/interface represented by this class context is the same or the
     * superclass/superinterface of the class/interface represented by the specified class context. This does so
     * similarity to the {@link Class#isAssignableFrom(Class)}.
     * @param other the class context to be checked against.
     * @return a boolean value of whether if the above condition is met.
     */
    public boolean isAssignableFrom(ClassContext other)
    {
        if (this.isInterface())
        {
            HashSet<ClassContext> itrfs = new HashSet<>();
            ClassContext ctx = other;
            while (ctx != null)
            {
                for (ClassContext itrf : ctx.getInterfaces())
                {
                    itrfs.add(itrf);
                    if (itrf.equals(this))
                        return true;
                }
                ctx = other.getSuperClass();
            }

            Queue<ClassContext> untouchedIntefaces = new LinkedList<>();
            untouchedIntefaces.addAll(itrfs);
            while (!untouchedIntefaces.isEmpty())
            {
                ctx = untouchedIntefaces.remove();
                for (ClassContext itrf : ctx.getInterfaces())
                {
                    if (itrfs.add(ctx))
                    {
                        untouchedIntefaces.add(itrf);
                        if (itrf.equals(this))
                            return true;
                    }
                }
            }

            assert !itrfs.contains(this);
            return false;
        }
        else if (!other.isInterface())
        {
            ClassContext ctx = other;
            while (ctx != null)
            {
                if (ctx.equals(this))
                    return true;
                ctx = ctx.getSuperClass();
            }
            return false;
        }
        else
            return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassContext that = (ClassContext) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode()
    {
        return 17 * name.hashCode();
    }

    /**
     * Tests whether if this class can access the other class context.
     * @param other other class to test against
     * @return true if accessible, false if inaccessible.
     */
    public boolean checkAccessClass(ClassContext other)
    {
        ClassContext[] thisChain = getOuterClassChain();
        ClassContext thisTop = thisChain[thisChain.length - 1];
        ClassContext[] otherChain = other.getOuterClassChain();
        ClassContext otherTop = otherChain[otherChain.length - 1];

        return thisTop.equals(otherTop) || checkAccessClass0(other);
    }

    /**
     * Recursive test access class without edge case testing.
     * @param other other class to test against.
     * @return true if accessible, false if inaccessible.
     */
    private boolean checkAccessClass0(ClassContext other)
    {
        ClassContext otherOuter = other.getOuterClass();
        if (otherOuter != null && !checkAccessClass0(otherOuter))
            return false;

        AccessModifier access = other.getAccessModifier();
        return AccessModifier.PUBLIC == access ||
                AccessModifier.PACKAGE_PRIVATE.implies(access) && checkPackageAccess(other) ||
                AccessModifier.PROTECTED.implies(access) && checkProtectedAccess(other);

    }

    /**
     * Tests whether if this class can access the member of another class context.
     * @param member the other class member to test against.
     * @return true if accessible, false if inaccessible.
     */
    public boolean checkAccessMember(MemberContext member)
    {
        ClassContext other = member.getOwner();

        ClassContext[] thisChain = getOuterClassChain();
        ClassContext thisTop = thisChain[thisChain.length - 1];
        ClassContext[] otherChain = other.getOuterClassChain();
        ClassContext otherTop = otherChain[otherChain.length - 1];

        if (thisTop.equals(otherTop))
            return true;

        if (!checkAccessClass(other))
            return false;

        AccessModifier access = member.getAccessModifier();
        return AccessModifier.PUBLIC == access ||
                AccessModifier.PACKAGE_PRIVATE.implies(access) && checkPackageAccess(other) ||
                AccessModifier.PROTECTED.implies(access) && checkProtectedAccess(other);
    }

    /**
     * Tests whether if the other class context is accessible via protected access, on the merit of inheritance only.
     * @param other the class context to test against.
     * @return true if accessible, false if inaccessible.
     */
    public boolean checkProtectedAccess(ClassContext other)
    {
        ClassContext ctx = other;
        while (ctx != null)
        {
            if (ctx.isAssignableFrom(this))
                return true;
            ctx = ctx.getOuterClass();
        }
        return false;
    }

    /**
     * Tests whether if this other class is within the same package.
     * @param other the other class's context.
     * @return true if in same package, false if not.
     * @throws IllegalArgumentException if <code>otherInternal</code> is not in a valid class format
     */
    public boolean checkPackageAccess(ClassContext other)
    {
        int thisPathInd = name.lastIndexOf('/');
        int otherPathInd = other.name.lastIndexOf('/');
        return thisPathInd == otherPathInd && name.substring(0, thisPathInd).equals(other.name.substring(0,
                otherPathInd));
    }
}
