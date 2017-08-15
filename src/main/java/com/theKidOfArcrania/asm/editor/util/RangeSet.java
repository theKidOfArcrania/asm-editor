package com.theKidOfArcrania.asm.editor.util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a set of items that span over a range.
 * @author Henry Wang
 */
public class RangeSet<T> implements Iterable<RangeSet<T>.RangeElement>
{
    /**
     * This represents a range that it's elements span. This contains two states <tt>to</tt> and <tt>from</tt> that
     * correspond to the two end-points of this range. Within this range, it contains the said elements.
     */
    public class RangeElement implements Cloneable
    {
        private final Set<T> items;
        private int from;
        private int to;

        /**
         * Creates a new range element.
         * @param from the starting range.
         * @param to the ending range.
         * @param items the list of items to add.
         */
        private RangeElement(int from, int to, Collection<? extends T> items)
        {
            this.items = new HashSet<>(items);
            this.from = from;
            this.to = to;
        }

        /**
         * Obtains a copy of this range element, such that any subsequent edits to this copy will not reflect the
         * parent object and vice versa.
         * @return a copy of the range element
         */
        public RangeElement copy()
        {
            return new RangeElement(from, to, items);
        }

        public Set<T> getItems()
        {
            return new HashSet<>(items);
        }

        public int getFrom()
        {
            return from;
        }

        public int getTo()
        {
            return to;
        }

        @Override
        public String toString()
        {
            return from + "-" + to + ": " + items;
        }
    }

    private final ArrayList<RangeElement> eles;

    /**
     * Creates a new range list.
     */
    public RangeSet()
    {
        this.eles = new ArrayList<>();
    }

    /**
     * Unions this item at this specified range.
     * @param from the starting range
     * @param to the ending range
     * @param item the item to add.
     * @throws IllegalArgumentException if from is greater than to.
     * @return if the set changed as a result of this add.
     */
    public boolean add(int from, int to, T item)
    {
        return addAll(from, to, Collections.singletonList(item));
    }

    /**
     * Unions this series of items at this specified range.
     * @param from the starting range
     * @param to the ending range
     * @param items the item to add.
     * @throws IllegalArgumentException if from is greater than to.
     * @return if the set changed as a result of this add.
     */
    public boolean addAll(int from, int to, Collection<? extends T> items)
    {
        if (from == to || items.isEmpty())
            return false;
        if (from > to)
            throw new IllegalArgumentException("`from` must be less than or equal to `to`.");

        RangeElement ele = new RangeElement(from, to, items);
        if (eles.isEmpty())
        {
            eles.add(ele);
            return true;
        }

        int ind = binarySearch(from);
        if (ind == -1)
            ind = combineRange(0, ele);
        while (ele.from < ele.to && ind < eles.size())
        {
            ind = mergeRange(ind, ele);
            ind = combineRange(ind, ele);
        }
        consolidate();
        return true;
    }

    /**
     * Clears all the range elements from this range set.
     */
    public void clear()
    {
        eles.clear();
    }

    /**
     * Removes all elements that meet a specified condition
     * @param test condition by which to remove elements.
     * @return true if the list has been changed as a result of this removal.
     */
    public boolean removeIf(Predicate<T> test)
    {
        boolean removed = false;
        Iterator<RangeElement> itr = eles.iterator();
        RangeElement prev = null;
        while (itr.hasNext())
        {
            RangeElement ele = itr.next();
            removed |= ele.items.removeIf(test);
            if (ele.items.isEmpty())
                itr.remove();
            else
            {
                if (prev != null && prev.to == ele.from && prev.items.equals(ele.items))
                {
                    prev.to = ele.to;
                    itr.remove();
                }
                else
                    prev = ele;
            }
        }
        return removed;
    }

    /**
     * Removes the element from across the entire range. This will remove any blank ranges and consolidate
     * consecutive ranges containing the same elements
     * @param item the item to remove.
     * @return true if this range list has changed as a result of this removal.
     */
    public boolean remove(T item)
    {
        boolean removed = false;
        Iterator<RangeElement> itr = eles.iterator();
        RangeElement prev = null;
        while (itr.hasNext())
        {
            RangeElement ele = itr.next();
            removed |= ele.items.remove(item);
            if (ele.items.isEmpty())
                itr.remove();
            else
            {
                if (prev != null && prev.to == ele.from && prev.items.equals(ele.items))
                {
                    prev.to = ele.to;
                    itr.remove();
                }
                else
                    prev = ele;
            }
        }
        return removed;
    }

    /**
     * Retains only range elements that are within the specified range.
     * @param from starting range point
     * @param to ending range point
     */
    public void retainRange(int from, int to)
    {
        if (eles.isEmpty())
            return;
        while (eles.get(0).to <= from)
            eles.remove(0);
        if (eles.isEmpty())
            return;
        if (eles.get(0).from < from)
            eles.get(0).from = from;

        int i = eles.size() - 1;
        while (i >= 0 && eles.get(i).from >= to)
            eles.remove(i--);
        if (!eles.isEmpty() && eles.get(i).to > to)
            eles.get(i).to = to;
    }

    /**
     * Creates a sequential stream from the associated {@link #spliterator()} method.
     * @return the stream.
     */
    public Stream<RangeElement> stream()
    {
        return StreamSupport.stream(spliterator(), false);
    }


    /**
     * Creates a parallel stream from the associated {@link #spliterator()} method.
     * @return the stream.
     */
    public Stream<RangeElement> parallelStream()
    {
        return StreamSupport.stream(spliterator(), true);
    }



    /**
     * Obtains a weakly bound iterator snapshot of the ranges listed in this range list. This may skip a few ranges,
     * known as "holes" if there are no items in those ranges.
     * @return an iterator to iterate through all the ranges.
     */
    @Override
    public Iterator<RangeElement> iterator()
    {
        List<RangeElement> list = new ArrayList<>();
        for (RangeElement ele : eles)
        {
            RangeElement copy = ele.copy();
            list.add(copy);
        }
        return list.iterator();
    }

    @SuppressWarnings({"SuspiciousToArrayCall", "unchecked"})
    @Override
    public Spliterator<RangeElement> spliterator()
    {
        List<RangeElement> list = new ArrayList<>();
        for (RangeElement ele : eles)
        {
            RangeElement copy = ele.copy();
            list.add(copy);
        }
        return Arrays.spliterator((RangeElement[])list.toArray(new Object[0]));
    }

    @Override
    public String toString()
    {
        return eles.toString();
    }

    /**
     * Consolidates all contigous ranges that contain the same elements.
     */
    private void consolidate()
    {
        RangeElement last = null;
        ArrayList<RangeElement> tmp = new ArrayList<>();
        for (RangeElement e : eles)
        {
            if (last == null)
                tmp.add(e);
            else if (last.to >= e.from && last.items.equals(e.items))
            {
                last.to = e.to;
                e = last;
            }
            else
                tmp.add(e);
            last = e;
        }
        eles.clear();
        eles.addAll(tmp);
    }

    /**
     * Merges a range element with the specified range element. This will insert/remove the existing range elements as
     * needed to make room for this range.
     * @param ind the index of the range to merge with.
     * @param insert the range element to perform merge with.
     * @return the next index to check merge with after the needed inserts.
     */
    private int mergeRange(int ind, RangeElement insert)
    {
        if (insert.from == insert.to)
            return ind;

        RangeElement merged = eles.get(ind);
        if (merged.items.containsAll(insert.items))
        {
            //No need to merge.
            insert.from = Math.max(insert.from, merged.to);
            return ind + 1;
        }

        if (merged.from < insert.from)
        {
            if (merged.to <= insert.from)
                return ind + 1; //averted merge.
            eles.add(ind++, new RangeElement(merged.from, insert.from, merged.items));
            merged.from = insert.from;
        }
        //assert merged.from == insert.from
        if (insert.to < merged.to)
            eles.add(++ind, new RangeElement(insert.to, merged.to, merged.items));
        insert.from = merged.to = Math.min(merged.to, insert.to);
        merged.items.addAll(insert.items);
        return ind + 1;
    }

    /**
     * Combines the specified range element. This differs from {@link #mergeRange(int, RangeElement)} because this
     * function assumes that the low end of this range element DOES NOT intersect with any existing ranges. This will
     * combine until it detects a range element that it will clobber.
     *
     * @param ind the index of the nearest range that might intersect this range. This can be a number >= size if
     *            there are no ranges to check.
     * @param insert the range element to perform combination with.
     * @return the index of the range element to detect possible merging with, after any inserts.
     */
    private int combineRange(int ind, RangeElement insert)
    {
        if (insert.from == insert.to)
            return ind;

        if (ind >= eles.size())
        {
            eles.add(new RangeElement(insert.from, insert.to, insert.items));
            return ind;
        }

        RangeElement evade = eles.get(ind);
        if (evade.from == insert.from)
            return ind;

        //assert evade.from > insert.from
        int combineTo = Math.min(evade.from, insert.to);
        eles.add(ind++, new RangeElement(insert.from, combineTo, insert.items));
        insert.from = combineTo;
        return ind;
    }

    /**
     * Locates the index of the highest range element that is under this number point. This is implemented by a
     * modified binary search. If this point is lower than the lowest range element, this will return -1.
     * @param point the number to search
     * @return the index referring to the matched range element.
     */
    private int binarySearch(int point)
    {
        if (eles.isEmpty())
            return -1;

        int low = 0;
        int high = eles.size() - 1;

        while (low < high)
        {
            int mid = (low + high) / 2;
            int cmp = point - eles.get(mid).from;
            if (cmp > 0)
                low = mid + 1;
            else if (cmp < 0)
                high = mid - 1;
            else //if (cmp == 0)
                return mid;
        }

        if (high < 0)
            return -1;
        return point >= eles.get(high).from ? high : high - 1;
    }
}
