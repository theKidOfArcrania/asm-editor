package com.theKidOfArcrania.asm.editor.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a list of items over a range.
 * @author Henry Wang
 */
public class RangeList<T> implements Iterable<RangeList<T>.RangeElement>
{
    public class RangeElement implements Cloneable
    {
        private final List<T> items;
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
            this.items = new ArrayList<>(items);
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

        public List<T> getItems()
        {
            return new ArrayList<>(items);
        }

        public int getFrom()
        {
            return from;
        }

        public int getTo()
        {
            return to;
        }
    }

    private ArrayList<RangeElement> eles;

    /**
     * Creates a new range list.
     */
    public RangeList()
    {
        this.eles = new ArrayList<>();
    }

    /**
     * Unions this item at this specified range.
     * @param from the starting range
     * @param to the ending range
     * @param item the item to add.
     * @throws IllegalArgumentException if from is greater than to.
     */
    public void add(int from, int to, T item)
    {
        addAll(from, to, Collections.singletonList(item));
    }

    /**
     * Unions this series of items at this specified range.
     * @param from the starting range
     * @param to the ending range
     * @param items the item to add.
     * @throws IllegalArgumentException if from is greater than to.
     */
    public void addAll(int from, int to, Collection<? extends T> items)
    {
        if (from == to)
            return;
        if (from > to)
            throw new IllegalArgumentException("`from` must be less than or equal to `to`.");

        RangeElement ele = new RangeElement(from, to, items);
        if (eles.isEmpty())
        {
            eles.add(ele);
            return;
        }

        int ind = binarySearch(from);
        if (ind == -1)
            ind = combineRange(0, ele);
        while (ele.from < ele.to && ind < eles.size())
        {
            ind = mergeRange(ind, ele);
            ind = combineRange(ind, ele);
        }
    }

    /**
     * Adds a blank range element.
     * @param from the starting range
     * @param to the ending range.
     */
    public void addBlank(int from, int to)
    {
        addAll(from, to, new ArrayList<>());
    }

    /**
     * Retains only range elements that are within the specified range.
     * @param from starting range point
     * @param to ending range point
     */
    public void retainRange(int from, int to)
    {
        while (eles.get(0).to < from)
            eles.remove(0);
        if (eles.isEmpty())
            return;
        if (eles.get(0).from < from)
            eles.get(0).from = from;

        int i = eles.size() - 1;
        while (i >= 0 && eles.get(i).from > to)
            eles.remove(i--);
        if (!eles.isEmpty() && eles.get(i).to > to)
            eles.get(i).to = to;
    }

    /**
     * Obtains a weakly bound iterator snapshot of the ranges listed in this range list. This may skip a few ranges,
     * known as "holes" if there are no items in those ranges.
     * @return an iterator to iterate through all the ranges.
     */
    @Override
    public Iterator<RangeElement> iterator()
    {
        ArrayList<RangeElement> copy = eles.stream().map(RangeElement::copy).collect(Collectors.toCollection
                (ArrayList::new));
        return copy.iterator();
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
        if (merged.from < insert.from)
        {
            if (merged.to <= insert.from)
                return ind + 1; //averted merge.
            eles.add(ind++, new RangeElement(merged.from, insert.from, merged.items));
            merged.from = insert.from;
        }
        //assert merged.from == insert.from
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
        RangeElement combine = new RangeElement(insert.from, Math.min(evade.from, insert.to), insert.items);
        eles.add(ind++, combine);
        insert.from = combine.to;
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
        if (eles.isEmpty() || point < eles.get(0).from)
            return -1;

        int low = 0;
        int high = eles.size() - 1;

        while (low + 1 < high)
        {
            int mid = (low + high) / 2;
            int cmp = point - eles.get(mid).from;
            if (cmp == 0)
                return mid;
            else if (cmp < 0)
                high = mid - 1;
            else //if (cmp > 0)
                low = mid + 1;
        }

        return point >= eles.get(high).from ? high : low;
    }
}
