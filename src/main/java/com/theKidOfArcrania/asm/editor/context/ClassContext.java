package com.theKidOfArcrania.asm.editor.context;


import com.theKidOfArcrania.asm.editor.util.IndexHashSet;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.theKidOfArcrania.asm.editor.context.TypeSignature.BOOLEAN_TYPE;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.parseTypeSig;
import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * Represents a context of a particular class.
 * @author Henry Wang
 */
public class ClassContext
{

    /**
     * A class visitor that parses a class-file meta-data, and generates a class context from the resulting class data.
     * @author Henry Wang
     */
    private static class ClassDataParser extends ClassVisitor
    {
        private ClassContext ctx;

        /**
         * Constructs a ClassDataParser.
         */
        public ClassDataParser()
        {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
        {
            //TODO: implement generics (signature)
            if (!ctx.name.equals(name))
                throw new IllegalArgumentException("Name does not match up with class.");

            ctx.resolved = true;
            ctx.setInterface(Modifier.isInterface(access));
            ctx.setModifiers(access);

            if (!ctx.isInterface() && !ctx.getName().equals("java/lang/Object"))
                ctx.setSuperClass(findContext0(superName, true));
            for (String itrf : interfaces)
                ctx.addInterface(findContext0(itrf, true));
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc)
        {
            ClassContext outer = findContext0(owner, true);
            assert outer != null;
            ctx.outer = outer;
            outer.addInnerClass(ctx);

            TypeSignature sig = parseTypeSig(desc);
            if (name != null && sig != null)
                outer.postLoad.add(() -> ctx.outerMethod = outer.findMethod(name, sig, false));
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access)
        {
            if (name.equals(outerName))
            {
                ClassContext inner = findContext0(name, true);
                assert inner != null;
                ctx.addInnerClass(inner);
                inner.setInnerName(innerName);
                inner.setOuterClass(ctx);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            //TODO: annotations
            return null;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
        {
            //TODO: annotations
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
        {
            //TODO: implement default values.
            FieldContext fld = ctx.addField(access, name, parseTypeSig(desc));
            if (Modifier.isStatic(access))
                fld.setDefaultValue(value);
            return null; //TODO: annotations
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            //TODO: implement generics.
            MethodContext mth = ctx.addMethod(access, name, parseTypeSig(desc));
            if (exceptions != null)
            {
                for (String except : exceptions)
                    mth.addException(findContext0(except, true));
            }
            return mth.writeBody();
        }

        @Override
        public void visitEnd()
        {
            List<Runnable> post = new ArrayList<>(ctx.postLoad);
            ctx.postLoad.clear();
            for (Runnable r : post)
                r.run();
        }
    }

    public static final ClassContext OBJECT_CONTEXT;
    private static final HashMap<String, ClassContext> CLASS_CONTEXT_MAP;
    private static final EnumMap<TypeSort, ClassContext> PRIMITIVE_MAP;


    static
    {
        CLASS_CONTEXT_MAP = new HashMap<>();
        OBJECT_CONTEXT = findContext("java/lang/Object"); //Make sure nothing overrides the Object class
        PRIMITIVE_MAP = new EnumMap<>(TypeSort.class);
    }

    /**
     * Obtains the class internal name for the specified class. This will only work with non-primitives.
     * @param cls the class to query.
     * @return the internal name.
     * @throws IllegalArgumentException if the class is a primitive type.
     */
    public static String getInternalName(Class<?> cls)
    {
        if (cls.isPrimitive())
            throw new IllegalArgumentException("Cannot get internal name of primitive.");
        return cls.getName().replace('.', '/');
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
        return findContext0(name, false);
    }

    /**
     * Finds associated class context with this name. If no such class context exist, this will return an unresolved
     * class context. This is used as a placeholder object for bad classes.
     * @param name the name of the class context.
     * @param unresolved whether to return an unresolved class if the class cannot be resolved
     * @return a class context.
     */
    private static ClassContext findContext0(String name, boolean unresolved)
    {
        ensureClassNameFormat(name);
        ClassContext ctx = CLASS_CONTEXT_MAP.computeIfAbsent(name, ClassContext::new);
        if (!ctx.isResolved())
        {
            try
            {
                if (ctx.isArray())
                    loadArrayContext(ctx);
                else
                    loadContextFromClass(ctx, new ClassReader(name));
            }
            catch (IOException e)
            {
                //e.printStackTrace();
                //System.err.println(name + " failed to load.");
                return unresolved ? ctx : null;
            }
        }
        if (!unresolved && !ctx.checkResolved())
            return null;
        return ctx;
    }

    /**
     * Loads an array type class context.
     * @param ctx the class context to load.
     */
    private static void loadArrayContext(ClassContext ctx)
    {
        String compName = ctx.getName().substring(1);
        TypeParser parser = new TypeParser(compName);
        ClassContext comp;
        TypeSort sort = parser.nextTypeSort();
        switch (sort)
        {
            case ARRAY: comp = findContext(compName); break;
            case OBJECT: comp = findContext(compName.substring(1, compName.length() - 1)); break;
            case VOID:
            case METHOD: throw new InternalError();
            default: comp = loadPrimitive(sort);
        }

        if (comp == null)
            return;

        ctx.arrayComponent = comp;
        ctx.superClass = OBJECT_CONTEXT;
        ctx.interfaces.add(findContext("java/io/Serializable"));
        ctx.interfaces.add(findContext("java/lang/Cloneable"));
        ctx.addMethod(PUBLIC, "clone", parseTypeSig("()Ljava/lang/Object;"));
        ctx.resolved = true;
    }

    /**
     * Loads the specified primitive type.
     * @param prim the primitive type to load.
     * @return a class context representing the primitive type.
     */
    private static ClassContext loadPrimitive(TypeSort prim)
    {
        return PRIMITIVE_MAP.computeIfAbsent(prim, ClassContext::new);
    }

    /**
     * Creates a blank new class context. This will mark this class context as resolved.
     * @param name the internal name of the class.
     * @param itrf whether if this is an interface.
     * @throws IllegalArgumentException if a super class is passed for interfaces.
     * @throws IllegalStateException if such a class context already exists.
     * @return the create class context.
     */
    public static ClassContext createContext(String name, boolean itrf)
    {
        if (name.startsWith("["))
            throw new IllegalArgumentException("Cannot create an array class");
        ClassContext ctx = CLASS_CONTEXT_MAP.computeIfAbsent(name, n -> new ClassContext(n, itrf));
        if (ctx.isResolved())
            throw new IllegalArgumentException("Class context '" + name + "' already exists.");
        ctx.setInterface(itrf);
        ctx.resolved = true;
        return ctx;
    }

    /**
     * Creates a new class context initializing it with some information. This will mark this class as resolved.
     * @param name the internal name of the class.
     * @param modifiers the access modifiers if any.
     * @param outer the outer class that this class context is in.
     * @param superClass the super class of this class (must be null for interfaces). If null, and this class context
     *                  is not an interface {@link Object} assumed.
     * @param interfaces the number of interfaces that this class explicitly extends/ implements
     * @return the created class context.
     * @throws IllegalArgumentException if such a class context already exists.
     */
    public static ClassContext createContext(String name, int modifiers, ClassContext outer, ClassContext superClass,
                                             ClassContext[] interfaces)
    {
        if (name.startsWith("["))
            throw new IllegalArgumentException("Cannot create an array class");
        ClassContext ctx = CLASS_CONTEXT_MAP.computeIfAbsent(name, ClassContext::new);
        if (ctx.isResolved())
            throw new IllegalArgumentException("Class context '" + name + "' already exists.");

        ctx.setOuterClass(outer);
        ctx.setModifiers(modifiers);
        ctx.setSuperClass(superClass);
        for (ClassContext itrf : interfaces)
            ctx.addInterface(itrf);
        ctx.resolved = true;
        return ctx;
    }

    /**
     * Loads an existing class context with the contents of a class reader
     * @param ctx the class context object to load to.
     * @param cls the class object to load from.
     * @throws IllegalArgumentException if the name in the class context doesn't match up with the class reader.
     */
    private static void loadContextFromClass(ClassContext ctx, ClassReader cls)
    {
        ClassDataParser parser = new ClassDataParser();
        parser.ctx = ctx;
        cls.accept(parser, ClassReader.SKIP_DEBUG);
    }

    /**
     * Verifies whether the specified class name is in a valid format.
     * @param internalName the internal name of the class.
     * @return true if a valid format, false otherwise.
     */
    public static boolean verifyClassNameFormat(String internalName)
    {
        if (internalName.isEmpty())
            return false;

        if (internalName.startsWith("["))
            return parseTypeSig(internalName) != null;


        char[] name = internalName.toCharArray();
        boolean beginning = true;
        for (char ch : name)
        {
            if (beginning)
            {
                if (!Character.isJavaIdentifierStart(ch))
                    return false;
                beginning = false;
            }
            else if (!Character.isJavaIdentifierPart(ch))
            {
                if (ch != '/')
                    return false;
                beginning = true;
            }
        }

        return !beginning;
    }

    /**
     * Ensures that the specified class name is in a valid format.
     * @param internalName the internal name of the class.
     * @throws IllegalArgumentException if it is not in a valid class format.
     */
    private static void ensureClassNameFormat(String internalName)
    {
        if (!verifyClassNameFormat(internalName))
            throw new IllegalArgumentException("Illegal class format: '" + internalName + "'");
    }

    private boolean array;
    private TypeSort primSort;

    private boolean resolved;
    private boolean fullyResolved;
    private final ArrayList<Runnable> postLoad;

    private String name;
    private int modifiers;

    private ClassContext superClass;
    private final IndexHashSet<ClassContext> interfaces;

    private final IndexHashSet<ClassContext> inners;
    private ClassContext outer;
    private MethodContext outerMethod;
    private String innerName;

    private final IndexHashSet<MemberContext> members;

    private ClassContext arrayComponent;

    /**
     * Creates a primitive class context.
     * @param prim the type of primitive to create.
     */
    private ClassContext(TypeSort prim)
    {
        this.primSort = prim;
        PRIMITIVE_MAP.put(prim, this);

        this.modifiers = Modifier.PUBLIC;
        this.name = null;
        this.outer = null;
        this.superClass = null;
        this.interfaces = new IndexHashSet<>();
        this.inners = new IndexHashSet<>();

        members = new IndexHashSet<>();
        postLoad = new ArrayList<>();
        resolved = true;
    }

    /**
     * Creates a class context initializing as a public top-level class. This will initially mark this class as not
     * resolved.
     * @param name the name of the class to create.
     */
    private ClassContext(String name)
    {
        ensureClassNameFormat(name);
        if (name.startsWith("["))
            array = true;

        CLASS_CONTEXT_MAP.put(name, this);

        this.modifiers = Modifier.PUBLIC;
        this.name = name;
        this.outer = null;
        this.superClass = OBJECT_CONTEXT;
        this.interfaces = new IndexHashSet<>();
        this.inners = new IndexHashSet<>();

        members = new IndexHashSet<>();
        postLoad = new ArrayList<>();
    }

    /**
     * Creates a class context initializing status whether if it is an interface or class. This will initially mark
     * this class as not resolved
     * @param name the internal name of the class.
     * @param itrf whether to create an interface or class.
     * @throws IllegalArgumentException if name is invalid
     */
    private ClassContext(String name, boolean itrf)
    {
        this(name);
        setInterface(itrf);
    }

    /**
     * Checks whether if this class is fully resolved. This will recursively check whether if all the referred
     * classes are also resolved.
     * @return true if fully resolved, false if some classes have not been resolved.
     */
    public boolean checkResolved()
    {
        return checkResolved0(new HashSet<>());
    }

    /**
     * Recursively checks whether if all its referred classes are resolved as well.
     * @param resolving the list of class contexts in the process of resolving.
     * @return true if fully resolved, false if some classes have not been resolved.
     */
    private boolean checkResolved0(Set<ClassContext> resolving)
    {
        if (resolving.contains(this))
            return true;
        resolving.add(this);
        if (!resolved)
            return false;
        if (fullyResolved)
            return true;

        if (superClass != null && !superClass.checkResolved0(resolving))
            return false;
        for (ClassContext ctx : interfaces)
            if (!ctx.checkResolved0(resolving))
                return false;
        if (outer != null && !outer.checkResolved0(resolving))
            return false;
        for (ClassContext ctx : inners)
            if (!ctx.checkResolved0(resolving))
                return false;
        for (MemberContext mem : members)
        {
            if (mem instanceof MethodContext)
            {
                for (ClassContext exc : ((MethodContext)mem).getExceptions())
                    if (!exc.checkResolved0(resolving))
                        return false;
            }
        }

        fullyResolved = true;
        return true;
        //TODO: resolve annotations as well.
    }

    /**
     * Sets whether if this is an interface or class. This will automatically change the super class to
     * <code>null</code> if turned to interface, or set to <code>java/lang/Object</code> for classes. If
     * <code>itrf</code> is the same as our current status of interface or not, this method will do nothing in effect.
     * @param itrf true to transform into an interface, false to transform into a class
     */
    public void setInterface(boolean itrf)
    {
        if (isInterface() == itrf)
            return;

        setModifierBits(Modifier.INTERFACE, itrf ? Modifier.INTERFACE : 0);
        if (itrf || name.equals("java/lang/Object"))
            superClass = null;
        else
            superClass = OBJECT_CONTEXT;
    }

    /**
     * This only checks whether if this class is resolved (use {@link #checkResolved()} to recursively check for
     * resolution).
     * @return true if resolved, false if not resolved
     */
    public boolean isResolved()
    {
        return resolved;
    }

    public AccessModifier getAccessModifier()
    {
        return AccessModifier.getAccessModifier(getModifiers());
    }

    public ClassContext getArrayComponent()
    {
        if (!array)
            throw new IllegalArgumentException("Not an array class context");
        return arrayComponent;
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
     * <code>java.lang.Object</code> class context is assumed. If this is an interface or java/lang/Object, it will
     * silently ignore the call if passed something other than <code>null</code>.
     * @param superClass the new super class to extend from.
     */
    public void setSuperClass(ClassContext superClass)
    {
        if (name.equals("java/lang/Object") || isInterface())
        {
            if (superClass == null)
                this.superClass = null;
            return;
        }
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

    public Set<ClassContext> getAllInnerClasses()
    {
        HashSet<ClassContext> allInners = new HashSet<>();
        ClassContext top = this;
        while (top.outer != null)
            top = top.outer;

        Queue<ClassContext> traverse = new LinkedList<>();
        while (!traverse.isEmpty())
        {
            ClassContext ctx = traverse.poll();
            for (ClassContext inner : ctx.inners)
                if (allInners.add(inner))
                    traverse.add(inner);
        }
        return allInners;
    }

    public String getInnerName()
    {
        return innerName;
    }

    public void setInnerName(String innerName)
    {
        this.innerName = innerName;
    }

    /**
     * Adds an inner class for this class/interface to implement/extend. Note that this will not refactor any changes
     * to the inner class context. <em>It is the responsibility of the caller to ensure that the correct values get
     * pushed to the inner class.</em>
     * inner class as well.
     * @param inner the inner class to add.
     * @return true if this is added, false if not added.
     */
    public boolean addInnerClass(ClassContext inner)
    {
        Objects.requireNonNull(inner);
        return inners.add(inner);
    }

    /**
     * Removes an inner class from this class/interface. Note that this will not refactor any changes to the inner
     * class context. <em>It is the responsibility of the caller to ensure that the correct values get pushed to
     * the inner class.</em>
     * @param inner the inner class to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeInnerClass(ClassContext inner)
    {
        return inners.remove(inner);
    }

    public Set<ClassContext> getThisInnerClasses()
    {
        return new HashSet<>(inners);
    }

    /**
     * Removes all inner classes, if any. This will not refactor any changes to the inner class contextes. <em>It is
     * the responsibility of the caller to ensure that the correct values get pushed to the inner classes.</em>
     */
    public void removeAllInnerClasses()
    {
        inners.clear();
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
        return members.add(mth) ? mth : null;
    }

    /**
     * Removes a method from this class/interface.
     * @param mth the method to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeMethod(MethodContext mth)
    {
        return members.remove(mth);
    }

    /**
     * Finds the corresponding method from its name and type signature. If this is not found, depending on the
     * <tt>recurse</tt> parameter, this will check it's superinterfaces and superclasses. If it is still not found,
     * it will return null.
     * @param name the name of the method.
     * @param signature the signature of the method.
     * @param recurse true to recurse inherited members.
     * @return a valid method context if found.
     * @throws IllegalArgumentException if an invalid name/signature is given.
     */
    public MethodContext findMethod(String name, TypeSignature signature, boolean recurse)
    {
        MethodContext found = members.search(new MethodContext(this, 0, name, signature));
        if (recurse && found == null)
        {
            List<MethodContext> candidates = new LinkedList<>();
            found = findMethodRecurse(name, signature, candidates, new HashSet<>());
            if (found == null)
            {
                ArrayList<MethodContext> defaults = new ArrayList<>();
                MethodContext abstractMth = null;
                Iterator<MethodContext> itr = candidates.iterator();
                while (itr.hasNext() && abstractMth == null)
                {
                    MethodContext mth = itr.next();
                    if ((mth.getModifiers() & (PUBLIC | ABSTRACT | STATIC)) == PUBLIC)
                        defaults.add(mth);
                    else
                        abstractMth = mth;
                }

                if (defaults.isEmpty())
                    return abstractMth;
                else if (defaults.size() == 1)
                    return defaults.get(0);
                else
                {
                    for (int i = defaults.size() - 1; i >= 0; i--)
                    {
                        MethodContext mthA = defaults.get(i);
                        if (mthA == null)
                            continue;
                        ClassContext itrfA = mthA.getOwner();
                        for (int j = i - 1; j >= 0; j--)
                        {
                            MethodContext mthB = defaults.get(j);
                            if (mthB == null)
                                continue;
                            ClassContext itrfB = mthB.getOwner();
                            if (itrfA.isAssignableFrom(itrfB)) //B overrides A.
                                defaults.set(i, null);
                            else if (itrfB.isAssignableFrom(itrfA)) //A overrides B.
                                defaults.set(j, null);
                        }
                    }

                    for (MethodContext mth : defaults)
                    {
                        if (mth != null)
                            return mth;
                    }
                }
            }
        }
        return found;
    }

    /**
     * Recursively finds the most concrete, direct method with this particular signature and name. This will return
     * the method if a direct superclass has the method. Otherwise, any possible interface candidates will be in the
     * specified set.
     *
     * @param name name of method.
     * @param signature name of signature.
     * @param candidates the list of interface candidates.
     * @param visited the set of visited classes/interfaces.
     * @return the found method context if this was found on a class.
     */
    private MethodContext findMethodRecurse(String name, TypeSignature signature, List<MethodContext> candidates,
                                      HashSet<ClassContext> visited)
    {
        if (visited.contains(this))
            return null;
        visited.add(this);

        MethodContext found = members.search(new MethodContext(this, 0, name, signature));
        if (found != null)
        {
            if (isInterface())
            {
                int mods = found.getModifiers() & (PUBLIC | ABSTRACT | STATIC);
                if (mods == PUBLIC) //default members
                    candidates.add(0, found);
                else if (mods == (PUBLIC | ABSTRACT)) //abstract methods
                    candidates.add(found);
            }
            else //Always return if we find a method in our class.
                return found;
        }

        ClassContext superCtx = superClass;
        if (superCtx != null)
        {
            found = superCtx.findMethodRecurse(name, signature, candidates, visited);
            if (found != null)
                return found;
        }

        for (ClassContext itrf : interfaces)
        {
            found = itrf.findMethodRecurse(name, signature, candidates, visited);
            if (found != null)
                return found;
        }
        return null;
    }

    /**
     * Renames the specified method into another name. This is a convenience method for
     * {@link #renameMethod(MethodContext, String, TypeSignature)}.
     * @param mth the method to rename.
     * @param newName the new name to set.
     * @throws IllegalArgumentException if the specified method is not owned by this class
     * @throws IllegalArgumentException if method name is not a valid identifier (can be &lt;init&gt;) or if type
     * signature is not for a method.
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
     * @throws IllegalArgumentException if method name is not a valid identifier (can be &lt;init&gt;) or if type
     * signature is not for a method.
     * @return true if rename is successful, otherwise returns false.
     */
    public boolean renameMethod(MethodContext mth, String newName, TypeSignature newSignature)
    {
        if (mth.getOwner() != this)
            throw new IllegalArgumentException("Method context does not belong to this class context.");

        MethodContext test = new MethodContext(this, mth.getModifiers(), newName, newSignature);
        if (test.equals(mth))
            return false;
        if (members.contains(test))
            return false;

        int slot = members.indexOf(mth);
        members.set(slot, test);
        mth.renameTo(test);
        members.set(slot, mth);
        return true;
    }

    public List<MethodContext> getMethods()
    {
        ArrayList<MethodContext> methods = new ArrayList<>(members.size());
        for (MemberContext m : members)
        {
            if (m instanceof MethodContext)
                methods.add((MethodContext)m);
        }
        return methods;
    }

    /**
     * Swaps the position of two methods.
     * @param a index of method a.
     * @param b index of method b.
     */
    public void swapMethods(int a, int b)
    {
        members.swap(a, b);
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
        return members.add(fld) ? fld : null;
    }

    /**
     * Removes a field from this class/interface.
     * @param fld the field to remove.
     * @return true if this is removed, false if it is not found.
     */
    public boolean removeField(FieldContext fld)
    {
        return members.remove(fld);
    }

    /**
     * Finds the corresponding field from its name. If this is not found, it will recurse to the superclasses +
     * superinterfaces. If not found, it will return null.
     * @param name the name of the field
     * @return a valid field context if found.
     * @throws IllegalArgumentException if an invalid name is given.
     */
    public FieldContext findField(String name)
    {
        FieldContext found = members.search(new FieldContext(this, 0, name, BOOLEAN_TYPE));
        if (found == null)
        {
            for (ClassContext ctx : interfaces)
            {
                found = ctx.findField(name);
                if (found != null)
                    return found;
            }

            ClassContext superCtx = superClass;
            if (superCtx != null && superCtx != this)
                found = superCtx.findField(name);
        }
        return found;
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
        if (members.contains(test))
            return false;

        int slot = members.indexOf(fld);
        members.set(slot, test);
        fld.renameTo(test);
        members.set(slot, fld);
        return true;
    }

    public List<FieldContext> getFields()
    {
        ArrayList<FieldContext> fields = new ArrayList<>(members.size());
        for (MemberContext m : members)
        {
            if (m instanceof FieldContext)
                fields.add((FieldContext)m);
        }
        return fields;
    }

    public List<MemberContext> getMembers()
    {
        return new ArrayList<>(members);
    }

    /**
     * Removes all the field and method members this class may have.
     */
    public void removeAllMembers()
    {
        members.clear();
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

    public boolean isArray()
    {
        return array;
    }

    public boolean isInterface()
    {
        return Modifier.isInterface(modifiers);
    }

    public boolean isPrimitive()
    {
        return primSort != null;
    }

    public TypeSort getPrimitiveType()
    {
        if (primSort == null)
            throw new IllegalStateException("Not a primitive type.");
        return primSort;
    }

    /**
     * Determines whether if the class/interface represented by this class context is the same or the
     * superclass/superinterface of the class/interface represented by the specified class context. This does so
     * similarity to the {@link Class#isAssignableFrom(Class)}. In other words, this method checks whether if an
     * object of the specified type can be assigned to an object of our type. It is equivalent to this psuedocode:
     * <code>this is_super_of other</code>
     * @param other the class context to be checked against.
     * @return a boolean value of whether if the above condition is met.
     */
    public boolean isAssignableFrom(ClassContext other)
    {
        if (this.getName().equals("java/lang/Object"))
            return true;
        if (this.equals(other))
            return true;
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

            Queue<ClassContext> untouchedInterfaces = new LinkedList<>();
            untouchedInterfaces.addAll(itrfs);
            while (!untouchedInterfaces.isEmpty())
            {
                ctx = untouchedInterfaces.remove();
                for (ClassContext itrf : ctx.getInterfaces())
                {
                    if (itrfs.add(ctx))
                    {
                        untouchedInterfaces.add(itrf);
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
     * Writes the class context into the respective class visitor. Note that some data may be loss from an existing
     * class, specifically, all DEBUG information will be lost. Furthermore, this will default to saving at version 8
     * @param writer the class visitor to write to.
     */
    public void writeClass(ClassVisitor writer)
    {
        writeClass(writer, Opcodes.V1_8);
    }

    /**
     * Writes the class context into the respective class visitor. Note that some data may be loss from an existing
     * class, specifically, all DEBUG information will be lost. This variant of writeClass will allow forcing a
     * particular class version. However, it is up to the client to make sure that this class is actually compliant
     * with that version number.
     * @param writer the class visitor to write to.
     * @param forceVersion the version number to force.
     */
    public void writeClass(ClassVisitor writer, int forceVersion)
    {
        String[] sItrf = interfaces.parallelStream().map(ClassContext::getName).toArray(String[]::new);
        writer.visit(forceVersion, modifiers, name, null, superClass == null ?
                "java/lang/Object" : superClass.name, sItrf);

        //TODO: annotations
        if (outer != null)
            writer.visitOuterClass(outer.name, outerMethod.getName(), outerMethod.getSignature().toString());
        for (ClassContext ctx : getAllInnerClasses())
            writer.visitInnerClass(ctx.name, outerMethod != null ? null : ctx.outer.name, ctx.innerName, ctx.modifiers);

        for (MemberContext mem : members)
        {
            if (mem instanceof MethodContext)
            {
                MethodContext mth = (MethodContext)mem;
                MethodVisitor mthVisitor = writer.visitMethod(mem.getModifiers(), mem.getName(),
                        mem.getSignature().toString(), null,
                        mth.getExceptions().parallelStream().map(ClassContext::getName).toArray(String[]::new));
                if (mthVisitor != null)
                    mth.readBody(mthVisitor);
            }
            else
            {
                FieldContext fld = (FieldContext)mem;
                FieldVisitor fldVisitor = writer.visitField(mem.getModifiers(), mem.getName(),
                        mem.getSignature().toString(), null, fld.getDefaultValue());
                if (fldVisitor != null)
                    fldVisitor.visitEnd();
            }
        }

        writer.visitEnd();
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
        if (other.equals(this))
            return true;

        AccessModifier access = member.getAccessModifier();
        return !access.equals(AccessModifier.PRIVATE) && (AccessModifier.PUBLIC == access ||
                        AccessModifier.PACKAGE_PRIVATE.implies(access) && checkPackageAccess(other) ||
                        AccessModifier.PROTECTED.implies(access) && checkProtectedAccess(other));

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
        if (other.array || array)
            return false;

        int thisPathInd = name.lastIndexOf('/');
        int otherPathInd = other.name.lastIndexOf('/');
        return thisPathInd == otherPathInd && name.substring(0, thisPathInd).equals(other.name.substring(0,
                otherPathInd));
    }

    @Override
    public String toString()
    {
         return name;
    }
}
