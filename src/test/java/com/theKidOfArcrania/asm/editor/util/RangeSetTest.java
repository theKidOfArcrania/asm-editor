package com.theKidOfArcrania.asm.editor.util;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

@SuppressWarnings({"JavaDoc", "MagicNumber"})
public class RangeSetTest
{
    @Test
    public void testAddCombine() throws Exception
    {
        RangeSet<String> eles = new RangeSet<>();
        eles.add(5, 10, "A");
        eles.add(0, 5, "A");
        eles.add(7, 11, "A");
        eles.add(12, 16, "A");
        eles.add(21, 22, "A");
        eles.add(20, 21, "A");
        eles.add(22, 25, "A");

        String[][] items = {{"A"}, {"A"}, {"A"}};
        int[][] ranges = {{0, 11}, {12, 16}, {20, 25}};
        checkData(eles, items, ranges);
    }

    @Test
    public void testLateCombine() throws Exception
    {
        RangeSet<String> eles = new RangeSet<>();
        eles.add(0, 5, "A");
        eles.add(0, 5, "B");
        eles.add(5, 10, "A");
        eles.add(5, 10, "B");
        eles.add(-5, 0, "A");
        eles.add(-5, 0, "B");

        String[][] items = {{"A", "B"}};
        int[][] ranges = {{-5, 10}};
        checkData(eles, items, ranges);
    }

    @Test
    public void testAddMerge() throws Exception
    {
        RangeSet<String> eles = new RangeSet<>();
        eles.add(1, 7, "A");
        eles.add(9, 12, "A");
        eles.add(5, 10, "B");
        eles.add(13, 15, "A");

        String[][] contains = {{"A"}, {"A", "B"}, {"B"}, {"A", "B"}, {"A"}, {"A"}};
        int[][] ranges = {{1, 5}, {5, 7}, {7, 9}, {9, 10}, {10, 12}, {13, 15}};
        checkData(eles, contains, ranges);
    }

    @Test
    public void testAddEmbed() throws Exception
    {
        RangeSet<String> eles = new RangeSet<>();
        eles.add(1, 7, "A");
        eles.add(2, 3, "B");
    }

    @Test
    public void testRemove() throws Exception
    {
        RangeSet<String> eles = new RangeSet<>();
        eles.add(1, 7, "A");
        eles.add(5, 10, "B");
        eles.add(9, 12, "A");
        eles.remove("A");

        String[][] contains = {{"B"}};
        int[][] ranges = {{5, 10}};
        checkData(eles, contains, ranges);
    }

    @Test
    public void testRetainRange() throws Exception
    {
        RangeSet<String> eles = new RangeSet<>();
        eles.add(1, 7, "A");
        eles.add(9, 12, "A");
        eles.add(5, 10, "B");
        eles.add(13, 15, "A");
        eles.retainRange(5, 10);

        String[][] contains = {{"A", "B"}, {"B"}, {"A", "B"}};
        int[][] ranges = {{5, 7}, {7, 9}, {9, 10}};
        checkData(eles, contains, ranges);
    }

    private <T> void checkData(RangeSet<? extends T> eles, T contains[][], int[][] ranges)
    {
        class Range
        {
            private final int from;
            private final int to;

            public Range(int from, int to)
            {
                this.from = from;
                this.to = to;
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Range range = (Range) o;

                return from == range.from && to == range.to;
            }

            @Override
            public int hashCode()
            {
                int result = from;
                result = 31 * result + to;
                return result;
            }

            @Override
            public String toString()
            {
                return "[" + from + ", " + to + "]";
            }
        }

        assumeTrue(contains.length == ranges.length);

        try
        {
            int ind = 0;
            for (RangeSet<? extends T>.RangeElement ele : eles)
            {
                if (ind >= contains.length)
                    fail("Out of range check");
                Range range = new Range(ranges[ind][0], ranges[ind][1]);
                assertEquals(range, new Range(ele.getFrom(), ele.getTo()));
                Set<? extends T> actual = ele.getItems();
                for (T e : contains[ind])
                    if (!actual.contains(e))
                        fail(range + " does not contain " + e);
                ind++;
            }
        }
        catch (AssertionError e)
        {
            System.err.println(eles);
            throw e;
        }
    }
}