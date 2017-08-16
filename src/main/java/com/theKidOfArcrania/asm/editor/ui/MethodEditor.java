package com.theKidOfArcrania.asm.editor.ui;

import com.theKidOfArcrania.asm.editor.code.highlight.*;
import com.theKidOfArcrania.asm.editor.code.parsing.CodeParser;
import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import com.theKidOfArcrania.asm.editor.code.parsing.Range;
import com.theKidOfArcrania.asm.editor.context.ClassContext;
import com.theKidOfArcrania.asm.editor.context.MethodContext;
import com.theKidOfArcrania.asm.editor.util.RangeSet;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.theKidOfArcrania.asm.editor.context.ClassContext.getInternalName;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.parseTypeSig;
import static java.lang.String.join;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.joining;
import static javafx.stage.Screen.getScreensForRectangle;
import static org.fxmisc.richtext.MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN;

/**
 * This class represents the user interface for the user to modify a method's code contents.
 * @author Henry Wang
 */
public class MethodEditor extends StackPane
{
    /**
     * This class is used to help view the method editor by itself.
     */
    public static class MethodEditorViewer extends Application
    {

        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;

        @Override
        public void start(Stage primaryStage) throws Exception
        {
            ClassContext classContext = ClassContext.findContext(getInternalName(MethodEditorViewer.class));
            MethodContext mthContext = classContext.findMethod("start",
                    parseTypeSig("(Ljavafx/stage/Stage;)V"), false);
            String code = mthContext.readCode(new CodeSymbols(null, classContext));

            MethodEditor editor = new MethodEditor(null, mthContext, code);
            StackPane root = new StackPane(editor);
            StackPane.setMargin(editor, new Insets(10));
            root.getStyleClass().add("main-dialog");

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add("com/theKidOfArcrania/asm/editor/ui/style.css");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    /**
     * Represents all the syntax stylizing for a particular line.
     */
    private class LineStyles
    {
        /**
         * Represents a single line syling data.
         */
        private class LineStyle
        {
            private final RangeSet<HighlightMark<?>> markers;
            private boolean modified;
            private int guard;
            //tag

            /**
             * Constructs a new line style
             */
            public LineStyle()
            {
                markers = new RangeSet<>();
                modified = true;
                guard = Integer.MAX_VALUE;
            }
        }

        private final ArrayList<LineStyle> lines;

        /**
         * Creates a new line styles
         */
        public LineStyles()
        {
            lines = new ArrayList<>();
        }

        /**
         * Adds a style marker to this line. If this exceeds the guard length, the values will clamp out.
         * @param lineNum the line number
         * @param from the starting range
         * @param to the ending range
         * @param style the marker object to add.
         */
        public void addMarker(int lineNum, int from, int to, HighlightMark<?> style)
        {
            LineStyle line = lines.get(lineNum - 1);
            int guard = line.guard;

            if (to <= 0 || from >= guard)
                return;
            if (from < 0)
                from = 0;
            if (to > guard)
                to = guard;
            if (line.markers.add(from, to, style))
                line.modified = true;
        }

        /**
         * Obtains a list of all line markers at a position
         * @param lineNum the line number
         * @param colNum the column number or -1 if to obtain all highlight markers.
         * @return the set of markers.
         */
        public Set<HighlightMark<?>> getMarkersAt(int lineNum, int colNum)
        {
            if (lineNum > lines.size() || lineNum <= 0)
                return new HashSet<>();
            if (colNum == -1)
            {
                HashSet<HighlightMark<?>> markers = new HashSet<>();
                for (RangeSet<HighlightMark<?>>.RangeElement e : lines.get(lineNum - 1).markers)
                    markers.addAll(e.getItems());
                return markers;
            }
            return lines.get(lineNum - 1).markers.get(colNum);
        }

        /**
         * Clears all styles for a particular line.
         * @param lineNum the line number
         */
        public void clearStyles(int lineNum)
        {
            LineStyle line = lines.get(lineNum - 1);
            line.markers.clear();
            line.modified = true;
        }

        /**
         * Deletes the line at the specified line number.
         * @param lineNum the line number.
         */
        public void deleteLine(int lineNum)
        {
            lines.remove(lineNum - 1);
        }

        /**
         * Adds the guard length for a line. This signifies where the line ends. Any indexes beyond this line will be
         * clamped at the line length.
         * @param lineNum the line number to set
         * @param length the length of the line
         */
        public void guardLine(int lineNum, int length)
        {
            LineStyle line = lines.get(lineNum - 1);
            line.guard = length;
            line.markers.retainRange(0, length);
        }

        /**
         * Inserts a new blank line with initially no styling.
         * @param lineNum the line number to insert at.
         */
        public void insertLine(int lineNum)
        {
            lines.add(lineNum - 1, new LineStyle());
        }

        /**
         * Removes a particular type of style marker from a particular line.
         *
         * @param lineNum the line number
         * @param markerType the marker type to remove
         */
        public void removeMarker(int lineNum, Enum<?> markerType)
        {
            LineStyle line = lines.get(lineNum - 1);
            if (line.markers.removeIf(h -> h.getType().equals(markerType)))
                line.modified = true;
        }

        /**
         * Applies all pending marker styles to the code area.
         */
        public void applyStyles()
        {
            //Compute real-time line offsets.
            String[] rawLines = codeArea.getText().split("\n");
            int off = 0;
            for (int i = 0; i < rawLines.length; i++)
            {
                LineStyle line = lines.get(i);
                RangeSet<HighlightMark<?>> markers = line.markers;
                if (line.modified && line.guard > 0)
                {
                    int last = 0;
                    StyleSpansBuilder<Collection<String>> ssb = new StyleSpansBuilder<>();

                    String tags = markers.stream().map(ele -> {
                        String prefix = ele.getFrom() + "-" + ele.getTo() + ": [";
                        return ele.getItems()
                                .stream()
                                .map(mark -> mark.getType() + (mark instanceof Tag ? " (" +
                                        ((Tag) mark).getTagDescription() + ")" : ""))
                                .collect(joining(", ", prefix, "]"));
                    }).collect(joining(", ", "[", "]"));
                    //System.out.println(i + 1 + " (+ " + off + "): " + tags);
                    for (RangeSet<HighlightMark<?>>.RangeElement ele : markers)
                    {
                        if (last < ele.getFrom())
                            ssb.add(Collections.emptyList(), ele.getFrom() - last);

                        last = ele.getTo();
                        ssb.add(ele.getItems().stream().map(HighlightMark::getType).map(Enum::toString)
                                        .collect(Collectors.toSet()), last - ele.getFrom());
                    }
                    if (last < line.guard)
                        ssb.add(Collections.emptyList(), line.guard - last);
                    codeArea.setStyleSpans(off, ssb.create());
                    line.modified = false;
                }
                off += rawLines[i].length() + 1;
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

    private static final Duration PARSE_DELAY = ofMillis(300);
    private static final int MOVE_TOOLTIP_RANGE = 10;

    private final List<Tag> highlightTags;
    private final List<Syntax> highlightSyntaxes;
    private final LineStyles styles;

    private final ArrayList<Integer> linePos;
    private final CodeParser parser;
    private final CodeArea codeArea;
    private final Tooltip tagMsg;
    private final ExecutorService executor;

    private int moveCount = 10;

    /**
     * Constructs a new method editor object.
     * @param global the global code symbols for this method
     * @param mth the associated method context to load code from
     * @param code the associated code from this method.
     */
    public MethodEditor(CodeSymbols global, MethodContext mth, String code)
    {
        getStylesheets().add("com/theKidOfArcrania/asm/editor/ui/syntax-def.css");
        getStyleClass().add("method-editor");

        highlightSyntaxes = new ArrayList<>();
        highlightTags = new ArrayList<>();
        styles = new LineStyles();

        executor = Executors.newFixedThreadPool(1, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        parser = new CodeParser(global, mth, "", new Highlighter()
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
        styles.insertLine(1);

        tagMsg = new Tooltip();
        tagMsg.setWrapText(true);
        //tagMsg.getStyleClass().add("tag-label");

        codeArea = new CodeArea();
        codeArea.plainTextChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .reduceSuccessions((Supplier<ArrayList<PlainTextChange>>) ArrayList::new,
                        MethodEditor::combineEdits, PARSE_DELAY)
                .mapToTask(this::computeChanges)
                .awaitLatest(codeArea.richChanges())
                .filterMap(t -> {
                    if (t.isSuccess())
                        return Optional.of(styles);
                    else
                    {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(LineStyles::applyStyles);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea)); //TODO: line number factory + tag id.

        codeArea.setMouseOverTextDelay(ofMillis(200));
        codeArea.addEventHandler(MOUSE_OVER_TEXT_BEGIN, e -> {
            int chIdx = e.getCharacterIndex();
            Point2D pos = e.getScreenPosition();

            int lineNum = searchLine(chIdx);
            int colNum = chIdx - linePos.get(lineNum - 1);

            showTagMsgs(pos, lineNum, colNum);
        });

        codeArea.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (moveCount == -1)
                return;
            moveCount++;
            if (moveCount >= MOVE_TOOLTIP_RANGE)
            {
                tagMsg.hide();
                moveCount = -1;
            }
        });


        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        getChildren().addAll(scroll);

        codeArea.insertText(0, code);
    }

    /**
     * Shows all the tag messages at a particular location.
     * @param pos position of the cursor.
     * @param lineNum the line number corresponding to cursor
     * @param colNum the column number corresponding to cursor, or -1 if at beginning of line.
     */
    private void showTagMsgs(Point2D pos, int lineNum, int colNum)
    {
        ArrayList<String> lines = new ArrayList<>();
        for (HighlightMark<?> mark : styles.getMarkersAt(lineNum, colNum))
        {
            if (mark instanceof Tag)
                lines.add(((Tag) mark).getTagDescription());
        }
        if (!lines.isEmpty())
        {
            double xPos = pos.getX();
            double yPos = pos.getY() + 5;
            Rectangle2D bounds = getScreensForRectangle(xPos, yPos, 0, 0).get(0).getVisualBounds();

            tagMsg.setText(join("\n", lines));
            tagMsg.setMaxWidth(bounds.getWidth());

            xPos -= tagMsg.prefWidth(-1) / 2;
            tagMsg.show(codeArea, xPos, yPos);
            moveCount = 0;
        }
    }

    /**
     * Computes all the changes that has been made to this editor and re-parses the appropriate lines. This will
     * queue the actual computation for a later time and will return this task's status.
     * @param changes the changes that has been made to the editor to be processed.
     * @return a task describing all the l
     */
    private Task<Void> computeChanges(List<PlainTextChange> changes)
    {

        Task<Void> task = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                highlightSyntaxes.clear();
                highlightTags.clear();
                updateChanges(changes);
                processLineStyles();
                return null;
            }
        };
        executor.execute(task);
        return task;
    }

    /**
     * This processes the resulting line styles (syntax highlighting and tags) that have been emitted by our code
     * parser into our line styles object.
     */
    @SuppressWarnings("unchecked")
    private void processLineStyles()
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
        for (int i = 0; i < lines.length; i++)
            styles.guardLine(i + 1, lines[i].length());

        boolean[] invalidated = new boolean[lines.length];
        for (HighlightMark mark : markList)
        {
            Range span = mark.getSpan();
            int startLine = span.getStart().getLineNumber();
            int endLine = span.getEnd().getLineNumber();
            for (int line = startLine; line <= endLine; line++)
            {
                if (!invalidated[line - 1])
                {
                    for (TagType type : TagType.values())
                        styles.removeMarker(line, type);
                    invalidated[line - 1] = true;
                }
                int start = line == startLine ? span.getStart().getColumnNumber() : 0;
                int end = line == endLine ? span.getEnd().getColumnNumber() : lines[line - 1].length();
                styles.addMarker(line, start, end, mark);
            }
        }
    }

    /**
     * Updates all the changes listed, re-parses the affected lines, and reanalyzes the code for symbolic errors.
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

        for (int i = 1; i <= parser.getLineCount(); i++)
        {
            if (parser.isLineDirty(i))
                styles.clearStyles(i);
        }

//        System.out.println("***");
//        for (int i = 0; i < parser.getLineCount(); i++)
//            System.out.println(parser.getLine(i + 1));
//        System.out.println("---");

        parser.reparse(false);
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
            styles.insertLine(firstLineNum + i);
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
                styles.deleteLine(firstLineNum + 1);
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

        while (low < high)
        {
            int mid = (high + low) / 2;
            int cmp = pos - linePos.get(mid);
            if (cmp > 0)
                low = mid + 1;
            else if (cmp < 0)
                high = mid - 1;
            else //if (cmp == 0)
                return mid + 1;
        }
        return pos >= linePos.get(high) ? high + 1 : high;
    }
}
