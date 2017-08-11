package com.theKidOfArcrania.asm.editor.util;

import java.util.*;

/**
 * This particular set implementation combines the O constant time of accessing indexes and searching objects from
 * HashSet and ArrayLists.
 *
 * @author Henry Wang
 */
public class IndexHashSet<T> extends AbstractSet<T>
{
    private HashMap<T, Integer> indexes = new HashMap<>();
    private ArrayList<T> elements = new ArrayList<>();

    @Override
    public boolean add(T element)
    {
        if (indexes.containsKey(element))
            return false;
        indexes.put(element, elements.size());
        elements.add(element);
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this list. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices). If this element
     * already exists, this function will do nothing.
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (<tt>index &lt; 0 || index &gt;= size()</tt>)
     * @return true if an element was added, false if it is not added.
     */
    public boolean add(int index, T element)
    {
        if (indexes.containsKey(element))
            return false;

        elements.add(index, element);
        indexes.put(element, index);

        int size = size();
        for (int i = index; i < size; i++)
            indexes.put(elements.get(i), i);
        return true;
    }

    /**
     * Adds all the elements at the particular index. This will not add any elements that already exist in this index
     * hash-set.
     *
     * @param index the index to add from.
     * @param c     the collection of elements to add.
     * @return true if the list changed as a result of this call.
     */
    public boolean addAll(int index, Collection<? extends T> c)
    {
        ArrayList<T> adding = new ArrayList<>(c.size());
        for (T ele : adding)
        {
            if (!indexes.containsKey(ele))
                adding.add(ele);
        }

        if (adding.isEmpty())
            return false;

        elements.addAll(index, adding);
        int size = size();
        for (int i = index; i < size; i++)
            indexes.put(elements.get(i), i);
        return true;
    }

    public void clear()
    {
        elements.clear();
        indexes.clear();
    }

    @Override
    public boolean contains(Object o)
    {
        return indexes.containsKey(o);
    }

    /**
     * Increases the capacity of the <tt>ArrayList</tt> portion of this <code>IndexHashSet</code> instance, if
     * necessary, to ensure that it can hold at least the number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity)
    {
        elements.ensureCapacity(minCapacity);
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof IndexHashSet && elements.equals(((IndexHashSet) o).elements);
    }

    /**
     * Returns the element at the specified position in this set.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this set
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public T get(int index)
    {
        return elements.get(index);
    }

    @Override
    public int hashCode()
    {
        return elements.hashCode();
    }

    /**
     * Returns the index of the occurrence of the specified element in this list, or -1 if this list does not contain
     * the element. More formally, returns the index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1 if there is no such index.
     *
     * @param o the object to search.
     * @return the index of the object.
     */
    public int indexOf(Object o)
    {
        return indexes.getOrDefault(o, -1);
    }

    @Override
    public Iterator<T> iterator()
    {
        Iterator<T> itr = elements.iterator();
        return new Iterator<T>()
        {
            private T prev;
            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }

            @Override
            public T next()
            {
                return prev = itr.next();
            }

            @Override
            public void remove()
            {
                itr.remove();
                indexes.remove(prev);
            }
        };
    }

    /**
     * Removes an element at the specified index
     *
     * @param index the index to remove element from.
     * @return the element removed
     */
    public T remove(int index)
    {
        T removed = elements.remove(index);
        indexes.remove(removed);
        return removed;
    }

    @Override
    public boolean remove(Object o)
    {
        Integer ind = indexes.remove(o);
        if (ind != null)
        {
            elements.remove(ind);
            return true;
        }
        else
            return false;

    }

    /**
     * Searches for an element within this set that matches the prototype object. Specifically, this will find the
     * actual element <code>e</code> within this list such that
     * <code>prototype==null&nbsp;?&nbsp;e==null : prototype.equals(e)</code>.
     * @param prototype the prototype element to search a match for.
     * @return the element that matches <code>prototype</code> or <code>null</code> if nothing matches.
     */
    @SuppressWarnings("unchecked")
    public <S extends T> S search(S prototype)
    {
        int ind = indexOf(prototype);
        if (ind == -1)
            return null;
        else
            return (S)elements.get(ind);
    }

    /**
     * Replaces the element at the specified position in this list with the specified element. This will not set to
     * the new element if one such element already exists (definition of a set) and it isn't on the index to replace.
     * In this case, it will return <code>null</code>.
     *
     * @param index   index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position, or null if the operation failed.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public T set(int index, T element)
    {
        if (indexes.getOrDefault(element, -1) != index)
            return null;
        T prev = elements.set(index, element);
        indexes.remove(prev);
        indexes.put(element, index);
        return prev;
    }

    @Override
    public int size()
    {
        return elements.size();
    }

    /**
     * Sorts this indexed hash set according to the comparator.
     *
     * @param c the comparator to sort against.
     */
    public void sort(Comparator<? super T> c)
    {
        elements.sort(c);
        for (int i = 0; i < elements.size(); i++)
            indexes.put(elements.get(i), i);
    }

    /**
     * Swaps two indexes.
     * @param a the first index
     * @param b the second index.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public void swap(int a, int b)
    {
        T eleA = elements.get(a);
        T eleB = elements.get(b);
        elements.set(b, eleA);
        elements.set(a, eleB);
        indexes.put(eleA, b);
        indexes.put(eleB, a);
    }

    @Override
    public Object[] toArray()
    {
        return elements.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <T> T[] toArray(T[] a)
    {
        return elements.toArray(a);
    }
}
