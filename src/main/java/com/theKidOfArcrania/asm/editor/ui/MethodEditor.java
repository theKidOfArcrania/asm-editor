package com.theKidOfArcrania.asm.editor.ui;

import com.theKidOfArcrania.asm.editor.code.highlight.HighlightMark;
import com.theKidOfArcrania.asm.editor.code.highlight.Highlighter;
import com.theKidOfArcrania.asm.editor.code.highlight.Syntax;
import com.theKidOfArcrania.asm.editor.code.highlight.Tag;
import com.theKidOfArcrania.asm.editor.code.parsing.CodeParser;
import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import com.theKidOfArcrania.asm.editor.code.parsing.Range;
import com.theKidOfArcrania.asm.editor.context.ClassContext;
import com.theKidOfArcrania.asm.editor.context.MethodContext;
import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import com.theKidOfArcrania.asm.editor.util.RangeList;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * This
 * @author Henry Wang
 */
public class MethodEditor extends StackPane
{
    public static class MethodEditorViewer extends Application
    {
        @Override
        public void start(Stage primaryStage) throws Exception
        {
            ClassContext classContext = ClassContext.findContext("TestClass");
            MethodContext mthContext;
            if (classContext == null)
            {
                classContext = ClassContext.createContext("TestClass", false);
                mthContext = classContext.addMethod(Modifier.PUBLIC, "TestMethod", TypeSignature.parseTypeSig("()V"));
            }
            else
                mthContext = classContext.findMethod("TestMethod", TypeSignature.parseTypeSig("()V"));

            MethodEditor editor = new MethodEditor(null, mthContext, "");

            StackPane root = new StackPane(editor);
            StackPane.setMargin(editor, new Insets(10));

            Scene scene = new Scene(root, 600, 400);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    private class LineStyles
    {
        private ArrayList<RangeList<String>> markers;
        private ArrayList<Integer> guards;
        //private ArrayList<List<String>> tags;

        public LineStyles()
        {
            markers = new ArrayList<>();
            guards = new ArrayList<>();
        }

        public void guardLine(int lineNum, int length)
        {
            initLine(lineNum);
            guards.set(lineNum - 1, length);

            RangeList<String> marker = markers.get(lineNum - 1);
            if (marker != null)
            {
                marker.addBlank(0, length);
                marker.retainRange(0, length);
            }
        }

        public void styleLine(int lineNum, int from, int to, String style)
        {
            initLine(lineNum);

            int guard = guards.get(lineNum - 1);
            RangeList<String> marker = markers.get(lineNum - 1);
            if (marker == null)
            {
                marker = new RangeList<>();
                marker.addBlank(0, guard);
                markers.set(lineNum - 1, marker);
            }

            if (from < 0)
                from = 0;
            if (to > guard)
                to = guard;
            marker.add(from, to, style);
        }

        public void applyStyles()
        {
            for (RangeList<String> marker : markers)
            {
                if (marker != null)
                {
                    for (RangeList<String>.RangeElement ele : marker)
                        codeArea.setStyle(ele.getFrom(), ele.getTo(), ele.getItems());
                }
            }
        }

        private void initLine(int lineNum)
        {
            while (lineNum > markers.size())
            {
                markers.add(null);
                guards.add(Integer.MAX_VALUE);
            }
        }


    }

    /**
     * Combines multiple edits together into an arraylist, squashing any changes if able.
     * @param list the list of changes
     * @param ptc the new change to add.
     * @return the original list of changes
     */
    private static ArrayList<PlainTextChange> combineEdits(ArrayList<PlainTextChange> list, PlainTextChange ptc)
    {
        if (!list.isEmpty())
        {
            int last = list.size() - 1;
            Optional<PlainTextChange> merge = list.get(last).mergeWith(ptc);
            if (merge.isPresent())
            {
                list.remove(last);
                ptc = merge.get();
            }
        }
        list.add(ptc);
        return list;
    }

    public static final Duration PARSE_DELAY = Duration.ofMillis(500);

    private final List<Tag> highlightTags;
    private final List<Syntax> highlightSyntaxes;

    private final ArrayList<Integer> linePos;
    private final CodeParser parser;
    private final CodeArea codeArea;
    private final ExecutorService executor;

    public MethodEditor(CodeSymbols global, MethodContext mth, String code)
    {
        getStylesheets().add("com/theKidOfArcrania/asm/editor/ui/syntax-def.css");

        highlightSyntaxes = new ArrayList<>();
        highlightTags = new ArrayList<>();

        executor = Executors.newSingleThreadExecutor();
        parser = new CodeParser(global, mth, code, new Highlighter()
        {

            @Override
            public void insertTag(Tag tag)
            {
                highlightTags.add(tag);
            }

            @Override
            public void insertSyntax(Syntax syn)
            {
                highlightSyntaxes.add(syn);
            }
        });

        linePos = new ArrayList<>();
        linePos.add(0);

        codeArea = new CodeArea();
        Region scroll = new VirtualizedScrollPane<>(codeArea);
        getChildren().addAll(scroll);

        codeArea.plainTextChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .reduceSuccessions((Supplier<ArrayList<PlainTextChange>>) ArrayList::new,
                        MethodEditor::combineEdits, PARSE_DELAY)
                .mapToTask(this::computeChanges)
                .awaitLatest(codeArea.richChanges())
                .filterMap(t -> {
                    if (t.isSuccess())
                    {
                        return Optional.ofNullable(t.get());
                    }
                    else
                    {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(LineStyles::applyStyles);
    }

    private Task<LineStyles> computeChanges(List<PlainTextChange> changes)
    {

        Task<LineStyles> task = new Task<LineStyles>()
        {
            @Override
            protected LineStyles call() throws Exception
            {
                highlightSyntaxes.clear();
                highlightTags.clear();
                updateChanges(changes);
                return computeLineStyles();
            }
        };
        executor.execute(task);
        return task;
    }

    @SuppressWarnings("unchecked")
    private LineStyles computeLineStyles()
    {

        List<HighlightMark> markList = new ArrayList<>(highlightSyntaxes);
        markList.addAll(highlightTags);

        //Compute real-time line offsets.
        String[] lines = codeArea.getText().split("\n");
        int[] offsets = new int[lines.length];

        int ind = 0;
        for (int i = 0; i < offsets.length; i++)
        {
            offsets[i] = ind;
            ind += lines[i].length() + 1;
        }

        //Compute all the highlighting
        LineStyles styles = new LineStyles();
        for (int i = 0; i < lines.length; i++)
            styles.guardLine(i + 1, lines[i].length());

        for (HighlightMark mark : markList)
        {
            Range span = mark.getSpan();
            int startLine = span.getStart().getLineNumber();
            int endLine = span.getEnd().getLineNumber();
            for (int line = startLine; line <= endLine; line++)
            {
                int start = line == startLine ? span.getStart().getColumnNumber() : 0;
                int end = line == endLine ? span.getEnd().getColumnNumber() : lines[line - 1].length();
                styles.styleLine(line, start, end, mark.getType().toString());
            }
        }
        return styles;
    }



    /**
     * Updates all the changes listed, reparses the affected lines, and reanalyzes the code for symbolic errors.
     * @param changes the text changes made to the code.
     */
    private void updateChanges(List<PlainTextChange> changes)
    {
        for (PlainTextChange change : changes)
        {
            if (!change.getRemoved().isEmpty())
                removeRange(change.getRemoved(), change.getPosition());
            if (!change.getInserted().isEmpty())
                insertRange(change.getInserted(), change.getPosition());
        }
        if (parser.reparse(false))
            parser.resolveSymbols();
    }

    /**
     * Inserts a range of code from a specified position.
     * @param added the text added.
     * @param position the position to start adding from.
     */
    private void insertRange(String added, int position)
    {
        int length = added.length();
        int firstLineNum = searchLine(position);
        String[] lines = added.split("\n", -1);

        //Modify the first line where we start adding stuff.
        int headOffset = position - linePos.get(firstLineNum - 1);
        String firstLine = parser.getLine(firstLineNum);
        String modLine;
        if (headOffset < firstLine.length())
            modLine = firstLine.substring(0, headOffset) + lines[0];
        else
            modLine = firstLine + lines[0];
        parser.modifyLine(firstLineNum, modLine);

        //Add subsequent lines
        int pos = linePos.get(firstLineNum - 1) + modLine.length() + 1;
        for (int i = 1; i < lines.length; i++)
        {
            parser.insertLine(firstLineNum + i, lines[i]);
            linePos.add((firstLineNum - 1) + i, pos);
            pos += lines[i].length() + 1;
        }

        //Modify last line in parser
        String tail = firstLine.substring(headOffset);
        if (!tail.isEmpty())
        {
            int lastLineNum = firstLineNum + lines.length - 1;
            String lastLine = parser.getLine(lastLineNum);
            parser.modifyLine(lastLineNum, lastLine + tail);
        }

        //Move down the line position of subsequent untouched lines.
        for (int i = (firstLineNum - 1) + lines.length; i < linePos.size(); i++)
            linePos.set(i, linePos.get(i) + length);
    }

    /**
     * Removes a range of code from a specified position.
     * @param removed the text removed.
     * @param position the position to start removing from.
     */
    private void removeRange(String removed, int position)
    {
        int length = removed.length();
        int firstLineNum = searchLine(position);
        int removedLines = countLines(removed) - 1;

        //Modify the first line where we start deleting stuff.
        int headOffset = position - linePos.get(firstLineNum - 1);
        String firstLine = parser.getLine(firstLineNum);
        String modLine = firstLine;
        if (headOffset < firstLine.length())
            modLine = firstLine.substring(0, headOffset);

        //Append any trailing text after removal range
        int lastLineNum = firstLineNum + removedLines;
        int tailOffset = (position + length) - linePos.get(lastLineNum - 1);
        String lastLine = parser.getLine(lastLineNum);
        if (tailOffset < lastLine.length())
            modLine += lastLine.substring(tailOffset);

        //Modify the line
        if (!modLine.equals(firstLine))
            parser.modifyLine(firstLineNum, modLine);

        //Delete subsequent lines.
        for (int i = 0; i < removedLines; i++)
        {
            if (linePos.size() >= firstLineNum)
            {
                parser.deleteLine(firstLineNum + 1);
                linePos.remove(firstLineNum);
            }
            else
                System.err.println("Unable to remove line position.");
        }

        //Move up the line position of subsequent untouched lines.
        for (int i = firstLineNum; i < linePos.size(); i++)
            linePos.set(i, linePos.get(i) - length);
    }

    /**
     * Counts the number of lines this string will span.
     * @param str the string to count
     * @return the number of lines.
     */
    private int countLines(String str)
    {
        int lines = 1;
        for (char c : str.toCharArray())
            if (c == '\n')
                lines++;
        return lines;
    }

    /**
     * Using a modified binary search algorithm, searches for the line number of this character position.
     * @param pos the position to search line number
     * @return the respective line number. (1-based)
     */
    private int searchLine(int pos)
    {
        int low = 0;
        int high = linePos.size() - 1;

        while (low + 1 < high)
        {
            int mid = (high + low) / 2;
            int cmp = pos - linePos.get(mid);
            if (cmp == 0)
                return mid + 1;
            else if (cmp < 0)
                high = mid - 1;
            else //if (cmp > 0)
                low = mid + 1;
        }

        return pos >= linePos.get(high) ? high + 1 : low + 1;
    }
}
