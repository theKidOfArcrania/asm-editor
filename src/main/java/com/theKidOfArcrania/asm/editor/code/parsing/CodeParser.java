package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.code.highlight.Highlighter;
import com.theKidOfArcrania.asm.editor.code.highlight.Syntax;
import com.theKidOfArcrania.asm.editor.code.highlight.SyntaxType;
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
    }

    /**
     * Inserts a new line of code at the particular line number. This new line will be marked dirty, but will not be
     * automatically parsed until a call to {@link #reparse()}.
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
     * to {@link #reparse()}.
     *
     * @param lineNum the 1-based line number.
     * @param line the line to modify to
     */
    public void modifyLine(int lineNum, String line)
    {
        reader.modifyLine(lineNum - 1, line);
        parsedCode.set(lineNum - 1, DIRTY_STATEMENT);
        highlighter.invalidLine(lineNum);
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
     * Reparses all the lines of dirty code.
     */
    public void reparse()
    {
        for (int i = 0; i < parsedCode.size(); i++)
        {
            if (parsedCode.get(i) == DIRTY_STATEMENT)
            {
                try
                {
                    reader.beginLine(i + 1);
                    parseLine();
                }
                catch (RuntimeException e)
                {
                    //Better error logging.
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Parse the currently selected line in the reader.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    private void parseLine()
    {
        if (reader.getLineNumber() < 1)
            throw new IllegalStateException("Not currently reading a line.");

        int lineInd = reader.getLineNumber() - 1;
        if (!reader.nextToken())
            parsedCode.set(lineInd, new EmptyStatement());
        else if (reader.hasTokenError())
        {
            parsedCode.set(lineInd, INVALID_STATEMENT);
            return;
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
            parsedCode.set(lineInd, line == null ? INVALID_STATEMENT : line);
        }

        //Syntax highlighting.
        parseSyntaxHighlight();
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