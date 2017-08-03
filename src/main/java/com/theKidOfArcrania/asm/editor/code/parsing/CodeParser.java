package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.code.highlight.*;
import com.theKidOfArcrania.asm.editor.context.MethodContext;

import java.util.ArrayList;
import java.util.EnumMap;

import static com.theKidOfArcrania.asm.editor.code.parsing.Range.characterRange;
import static com.theKidOfArcrania.asm.editor.code.parsing.Range.tokenRange;

/**
 * This parses the code using {@link CodeTokenReader} as the parser, and allows for continuous checks if necessary.
 * This will parse the code into a series of {@link CodeStatement} objects. This also provides a list of errors that
 * might have occurred while parsing.
 *
 * @author Henry Wang
 */
public class CodeParser
{
    public static final CodeStatement INVALID_STATEMENT = new EmptyStatement();
    public static final CodeStatement DIRTY_STATEMENT = new EmptyStatement();

    private static final EnumMap<TokenType, SyntaxType> syntaxScheme;

    static
    {
        syntaxScheme = new EnumMap<>(TokenType.class);
        syntaxScheme.put(TokenType.INTEGER, SyntaxType.NUMBER);
        syntaxScheme.put(TokenType.LONG, SyntaxType.NUMBER);
        syntaxScheme.put(TokenType.FLOAT, SyntaxType.NUMBER);
        syntaxScheme.put(TokenType.DOUBLE, SyntaxType.NUMBER);
        syntaxScheme.put(TokenType.STRING, SyntaxType.STRING);
        syntaxScheme.put(TokenType.TYPE_SIGNATURE, SyntaxType.SIGNATURE);
        syntaxScheme.put(TokenType.IDENTIFIER, SyntaxType.IDENTIFIER);
        syntaxScheme.put(TokenType.HANDLE, SyntaxType.HANDLE);
    }

    private final CodeTokenReader reader;
    private final ArrayList<CodeStatement> parsedCode;
    private final Highlighter highlighter;

    /**
     * Constructs a CodeParser from the specified code body.
     * @param global the global code symbols for this class context.
     * @param context the location where this code originates.
     * @param code the code body to read from.
     * @param highlighter the highlighter used to highlight syntax and tags.
     */
    public CodeParser(CodeSymbols global, MethodContext context, String code, Highlighter highlighter)
    {
        reader = new CodeTokenReader(global, context, code);

        this.highlighter = highlighter;

        int lines = reader.getLineCount();
        parsedCode = new ArrayList<>(lines);
        for (int i = 0; i < lines; i++)
        {
            parsedCode.add(DIRTY_STATEMENT);
            reader.nextLine();
            parseLine();
        }

        reader.addErrorLogger(new ErrorLogger()
        {
            @Override
            public void logError(String description, Range highlight)
            {
                highlighter.insertTag(new Tag(TagType.ERROR_TAG, highlight, description));
            }

            @Override
            public void logWarning(String description, Range highlight)
            {
                highlighter.insertTag(new Tag(TagType.WARNING_TAG, highlight, description));
            }
        });
    }

    /**
     * Inserts a new line of code at the particular line number. This new line will be marked dirty, but will not be
     * automatically parsed until a call to {@link #reparse(boolean)}.
     *
     * @param lineNum the 1-based line number.
     * @param line the line to insert.
     */
    public void insertLine(int lineNum, String line)
    {
        reader.insertLine(lineNum - 1, line);
        parsedCode.add(lineNum - 1, DIRTY_STATEMENT);
    }

    /**
     * Modifies a line of code. This will mark the current line as dirty, but will not reparse the code until a call
     * to {@link #reparse(boolean)}.
     *
     * @param lineNum the 1-based line number.
     * @param line the line to modify to
     */
    public void modifyLine(int lineNum, String line)
    {
        reader.modifyLine(lineNum - 1, line);
        parsedCode.set(lineNum - 1, DIRTY_STATEMENT);
        highlighter.invalidateLine(lineNum);
    }

    /**
     * This deletes a line of code. This will invoke {@link CodeStatement#reset()} on the parsed statement (if it was
     * parsed) in order to reset any symbols that were added.
     * @param lineNum the line number to remove.
     */
    public void deleteLine(int lineNum)
    {
        reader.deleteLine(lineNum);
        parsedCode.remove(lineNum).reset();
    }

    /**
     * Re-parses all the lines of dirty code. This may emit any parsing errors if encountered. By definition this
     * function is successful if and only if every single line is parsed, and is not left dirty or invalid.
     * @param parseInvalid determines whether to reparse any invalid lines.
     * @return true if re-parse was successful, false if some errors occurred while re-parsing.
     */
    public boolean reparse(boolean parseInvalid)
    {
        boolean success = true;
        for (int i = 0; i < parsedCode.size(); i++)
        {
            boolean invalid = parsedCode.get(i) == INVALID_STATEMENT;
            boolean dirty = parsedCode.get(i) == DIRTY_STATEMENT;

            if (dirty || invalid && parseInvalid)
            {
                try
                {
                    reader.beginLine(i + 1);
                    success &= parseLine();
                }
                catch (RuntimeException e)
                {
                    //Better error logging.
                    reader.error("Error occurred while parsing line: " + e.toString() + ".",
                            Range.tokenRange(i + 1, 0, reader.getLine().length()));
                    e.printStackTrace();
                    success = false;
                }
            }
            else if (invalid)
                success = false;
        }
        return success;
    }

    /**
     * Ensures that all the symbols referred to by the code are resolved. This should be faster than the parsing
     * time, so this will be called on each parsed statement each time.
     * @return true if resolution was successful, false if it failed.
     */
    public boolean resolveSymbols()
    {
        boolean success = true;
        for (int i = 0; i < parsedCode.size(); i++)
        {
            CodeStatement s = parsedCode.get(i);
            if (s != INVALID_STATEMENT && s != DIRTY_STATEMENT)
            {
                highlighter.invalidateTags(i + 1);
                success &= s.resolveSymbols();
            }
        }
        return success;
    }

    /**
     * Determines whether if a line is dirty. A line is defined as dirty if it has been modified since the last time
     * it was parsed.
     * @param line the 1-based line number
     * @return true if dirty, false if not dirty.
     */
    public boolean isLineDirty(int line)
    {
        return parsedCode.get(line - 1) == DIRTY_STATEMENT;
    }

    /**
     * Determines whether if a line is malformed. A line is malformed a parsing error occurred the last time it was
     * parsed.
     * @param line the 1-based line number.
     * @return true if malformed, false if not malformed.
     */
    public boolean isLineMalformed(int line)
    {
        return parsedCode.get(line - 1) == INVALID_STATEMENT;
    }

    /**
     * Parse the currently selected line in the reader.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     * @return true the parsing line was successful, false if an error occurred.
     */
    private boolean parseLine()
    {
        if (reader.getLineNumber() < 1)
            throw new IllegalStateException("Not currently reading a line.");

        boolean success = true;
        int lineInd = reader.getLineNumber() - 1;
        if (!reader.nextToken())
            parsedCode.set(lineInd, new EmptyStatement());
        else if (reader.hasTokenError())
        {
            parsedCode.set(lineInd, INVALID_STATEMENT);
            return false;
        }
        else if (reader.getTokenType() != TokenType.IDENTIFIER)
        {
            CodeStatement line;
            switch (reader.getTokenType())
            {
                case IDENTIFIER:
                    String token = reader.getToken();
                    if (token.startsWith("$"))
                        line = DirStatement.parseStatement(this, reader);
                    else
                        line = InstStatement.parseStatement(reader);
                    break;
                case LABEL:
                    line = LabelStatement.parseStatement(reader);
                    break;
                default:
                    reader.errorExpected("label, instruction, or directive");
                    line = null;
            }
            success = line != null;
            parsedCode.set(lineInd, success ? line : INVALID_STATEMENT);
        }

        //Syntax highlighting.
        parseSyntaxHighlight();
        return success;
    }

    /**
     * Parses all the syntax highlights of the current line.
     */
    private void parseSyntaxHighlight()
    {
        int prevEnd = -1;
        reader.visitToken(0);
        for (int i = 0; i < reader.getTokensRead(); i++)
        {
            SyntaxType type = null;
            if (i == 0)
            {
                String token = reader.getToken();
                if (token.startsWith("$"))
                    type = SyntaxType.DIRECTIVE;
                else if (token.endsWith(":"))
                    type = SyntaxType.LABEL;
                else
                    type = SyntaxType.INSTRUCTION;
            }
            else
            {
                if (reader.getTokenType() != null)
                    type = syntaxScheme.get(reader.getTokenType());
                String line = reader.getLine();
                int start = prevEnd;
                while ((start = line.indexOf(',', start)) < reader.getTokenStartIndex())
                    highlighter.insertSyntax(new Syntax(SyntaxType.COMMA, characterRange(reader.getLineNumber(), start)));
            }

            if (type != null)
                highlighter.insertSyntax(new Syntax(type, reader.getTokenPos()));

            prevEnd = reader.getTokenEndIndex();
            reader.nextToken(true);
        }

        int len = reader.getLine().length();
        int commentStart = reader.getCommentStartIndex();
        if (commentStart != -1)
            highlighter.insertSyntax(new Syntax(SyntaxType.COMMENT,  tokenRange(reader.getLineNumber(), commentStart, len)));
    }
}
