package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.context.MethodContext;
import com.theKidOfArcrania.asm.editor.util.FallibleFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import static com.theKidOfArcrania.asm.editor.code.parsing.Range.characterRange;
import static com.theKidOfArcrania.asm.editor.code.parsing.Range.tokenRange;

/**
 * Reads in token words for each line of code. This splits an existing body of code into lines, and it parses each
 * line individually as needed. This contains token type recognition and it will pre-parse these token types. This
 * will also provide a flexible error logging system whenever an parsing error occurs that should flag the user's
 * attention.
 *
 * A token word is defined as a sequence of word characters delimited by word boundaries (such as a whitespace). This
 * reads in the entire code line by line and looks at each line individually.
 *
 * Note that this is NOT synchronization safe. Specifically, concurrent modifications to the line while parsing a
 * line of code is not allowed. If concurrent parsing needs to be done, what can be suggested is to maintain a backlog
 * of all the change made while parsing.
 *
 * @author Henry Wang
 */
public class CodeTokenReader
{
    private static final Pattern NEW_LINE = Pattern.compile("\r?\n");

    private static final int HEX_RADIX = 16;
    private static final char COMMENT = '#';

    private final CodeSymbols resolved;

    private final ArrayList<String> lines;
    private String line;

    private int lineNum;
    private int colNum;
    private int commentStartInd;

    private int tokenNum;
    private boolean tokenError;
    private final ArrayList<Integer> prevTokens;

    private TokenType tokenType;
    private String token;
    private Object tokenVal;
    private int tokenStartIndex;
    private int tokenEndIndex;

    private boolean firstArgument;
    private boolean hasArgumentSeparator;
    private boolean argumentError;

    private final ArrayList<ErrorLogger> errLogs;


    /**
     * Constructs a CodeTokenReader reading from the specified code body.
     * @param global the global code symbols for this class context.
     * @param context the location where this code originates.
     * @param code the code body to read from.
     */
    public CodeTokenReader(CodeSymbols global, MethodContext context, String code)
    {
        this.resolved = new CodeSymbols(global, context.getOwner());

        lines = new ArrayList<>();
        Collections.addAll(lines, NEW_LINE.split(code));
        line = null;
        lineNum = 0;

        errLogs = new ArrayList<>();

        prevTokens = new ArrayList<>();
        resetLine();
    }

    /**
     * Obtains line at the particular line number
     * @param lineNum line number.
     * @return the line string.
     */
    public String getLine(int lineNum)
    {
        return lines.get(lineNum);
    }

    public String getLine()
    {
        return line;
    }

    public int getLineCount()
    {
        return lines.size();
    }

    /**
     * Moves the token reader to the next line of code.
     * @throws NoSuchElementException if there is no next line.
     */
    public void nextLine()
    {
        if (!hasNextLine())
            throw new NoSuchElementException();
        line = lines.get(lineNum++);
        resetLine();
    }

    /**
     * Resets the current line, as if nothing was ever read on this line.
     */
    public void resetLine()
    {
        colNum = 0;
        commentStartInd = -1;

        tokenStartIndex = -1;
        tokenEndIndex = -1;
        tokenType = null;
        token = null;
        firstArgument = true;

        prevTokens.clear();
        tokenNum = -1;
    }

    /**
     * Inserts a new line at the particular line number. It will automatically update the current line number if
     * inserting this line will shift the current line by one index.
     *
     * @param num 1-based index of the line number.
     * @param line the line to add
     */
    public void insertLine(int num, String line)
    {
        lines.add(num - 1, line);
        if (num <= lineNum)
            lineNum++;
    }

    /**
     * Deletes a line of code from this token reader. If this deletes the current line, this will invalidate any
     * parsing that might have occurred (and also invalidate the current line number) This will automatically update
     * the current line number if deleting this line will shift the current line by one index.
     *
     * @param num 1-based index of the line number.
     */
    public void deleteLine(int num)
    {
        if (lineNum == num)
        {
            resetLine();
            lineNum = 0;
            line = null;
        }
        lines.remove(num);
        if (num < lineNum)
            lineNum--;
    }

    /**
     * Modifies a line of code to a new line. If the currently parsed line is selected, this will automatically
     * invalidate the current token, and will reset as if invoked from {@link #nextLine()}.
     *
     * @param num 1-based index of the line number.
     * @param line the new value to change to.
     */
    public void modifyLine(int num, String line)
    {
        if (lineNum == num)
            resetLine();
        lines.set(num, line);
    }

    /**
     * Adds an error logger to this token reader.
     * @param logger the error logger.
     */
    public void addErrorLogger(ErrorLogger logger)
    {
        errLogs.add(logger);
    }

    /**
     * Removes an error logger from this token reader.
     * @param logger the error logger.
     */
    public void removeErrorLogger(ErrorLogger logger)
    {
        errLogs.remove(logger);
    }

    /**
     * Emits an error to all the error loggers.
     * @param description the error message description
     * @param highlight specifies the position that this error is highlighting, can be null.
     */
    public void error(String description, Range highlight)
    {
        if (tokenNum == prevTokens.size() - 1 || tokenNum == -1) //not visiting
        {
            for (ErrorLogger logger : errLogs)
                logger.logError(description, highlight);
        }
    }

    /**
     * Emits an expected error. This specifically refers to an error where the user fails to provide the correct type
     * of token where needed.
     * @param type the type the user needs to specify.
     */
    public void errorExpected(String type)
    {
        error("Expected: valid " + type + ".", getTokenPos());
    }

    /**
     * Begins parsing the specified line number.
     * @param lineNum the line number to parse.
     */
    public void beginLine(int lineNum)
    {
        line = lines.get(lineNum);
        this.lineNum = lineNum;
        resetLine();
    }

    /**
     * Emits a warning to all the error loggers
     * @param description the warning message description
     * @param highlight specifies the position that this warning is highlighting, can be null.
     */
    public void warning(String description, Range highlight)
    {
        if (tokenNum == prevTokens.size() - 1 || tokenNum == -1) //not visiting
        {
            for (ErrorLogger logger : errLogs)
                logger.logWarning(description, highlight);
        }
    }

    public int getLineNumber()
    {
        return lineNum;
    }

    public int getCommentStartIndex()
    {
        return commentStartInd;
    }

    /**
     * Gets the current token as a string
     * @return the token string value
     * @throws IllegalStateException if no current token is selected.
     */
    public String getToken()
    {
        if (tokenNum == -1)
            throw new IllegalStateException("No current token selected.");
        return token;
    }

    public int getTokenNum()
    {
        return tokenNum;
    }

    public int getTokensRead()
    {
        return prevTokens.size();
    }

    /**
     * Gets the token type that is being parsed (INTEGER, FLOAT, STRING, IDENTIFIER).
     * @return an enum value representing the token type.
     * @throws IllegalStateException if no current token is selected.
     */
    public TokenType getTokenType()
    {
        if (tokenNum == -1)
            throw new IllegalStateException("No current token selected.");
        return tokenType;
    }

    /**
     * Gets the range that specifies the boundaries of this token
     * @return the token boundaries
     * @throws IllegalStateException if no current token is selected.
     */
    public Range getTokenPos()
    {
        if (tokenNum == -1)
            throw new IllegalStateException("No current token selected.");
        return tokenRange(lineNum, getTokenStartIndex(), getTokenEndIndex());
    }

    public int getTokenStartIndex()
    {
        return tokenStartIndex;
    }

    public int getTokenEndIndex()
    {
        return tokenEndIndex;
    }

    /**
     * Parses the token value based on the token type that is being parsed.
     * @return
     */
    public Object getTokenValue()
    {
        if (tokenNum == -1)
            throw new IllegalStateException("No current token selected.");
        return tokenVal;
    }

    public CodeSymbols getResolvedSymbols()
    {
        return resolved;
    }

    /**
     * Determines whether if parsing the token produced an error.
     * @return true if an error occurred, false otherwise.
     */
    public boolean hasTokenError()
    {
        return tokenError;
    }

    /**
     * Checks whether if this code body has a next line to read.
     * @return true if there is a line remaining, false otherwise.
     */
    public boolean hasNextLine()
    {
        return lines.size() > lineNum;
    }

    /**
     * Moves to the next token on this current line. If there are no more tokens on this line, we will return
     * <code>false</code>. By default, this will not allow any commas between tokens. If a comma *may* be expected,
     * then the variant {@link #nextToken(boolean)} should be called.
     *
     * @return true if there is another token, false if no more tokens exist.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    public boolean nextToken()
    {
        return nextToken(false);
    }

    /**
     * Moves to the next token on this current line. If there are no more tokens on this line, we will return
     * <code>false</code>. This allows the client to bypass the comma check if necessary.
     *
     * @param allowComma true to allow commas, false to enforce no commas.
     * @return true if there is another token, false if no more tokens exist.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    public boolean nextToken(boolean allowComma)
    {
        readNextToken();
        if (!allowComma && tokenStartIndex != -1 && hasArgumentSeparator && !argumentError)
        {
            int comma = tokenStartIndex - 1;
            while (line.charAt(comma) != ',')
                comma--;
            error("Unexpected comma.", characterRange(lineNum, comma));
        }
        return tokenStartIndex != -1;
    }

    /**
     * Moves to the next token on this current line. If there are no more tokens on this line, we will return
     * <code>false</code>. Unlike {@link #nextToken()}, this will also search for a preceding comma separator (if
     * this is not the first argument). If one does not exist, it will log an error. This will still return
     * <code>true</code>, as another argument is found, but it will flag the error.
     *
     * @return true if there is another token, false if no more tokens exist.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    public boolean nextArgument()
    {
        if (firstArgument && tokenNum == prevTokens.size() - 1)
        {
            firstArgument = false;
            return nextToken();
        }

        readNextToken();
        if (tokenStartIndex != -1 && !hasArgumentSeparator && tokenNum == prevTokens.size() - 1)
            error("Expected a comma separator.", characterRange(lineNum, tokenStartIndex - 1));
        else if (tokenStartIndex == -1 && hasArgumentSeparator && !argumentError)
        {
            int comma = colNum - 1;
            while (line.charAt(comma) != ',')
                comma--;
            error("Unexpected comma.", characterRange(lineNum, comma));
        }
        return tokenStartIndex != -1;
    }

    /**
     * Visits a previous token. If any parsing error/warning occurs, they will be silently ignored, since they
     * probably have already been emitted once already.
     *
     * @param tokenNum the token index to visit
     */
    public void visitToken(int tokenNum)
    {
        if (lineNum < 1)
            throw new IllegalStateException("Not currently reading a line.");

        colNum = prevTokens.get(tokenNum);
        this.tokenNum = tokenNum - 1;
        readNextToken();
    }

    /**
     * Identifies whether if the current position represents the token starting character. If it is, this will parse
     * the token fully, and change the appropriate indexes to point to the character after the token.
     *
     * @return true if this is a token start, false otherwise.
     */
    private boolean identifyTokenStart()
    {
        char ch = line.charAt(colNum);
        if (ch == '"')
        {
            parseStringToken();
            return true;
        }
        else if (Character.isDigit(ch) || ch == '.' || ch == '+' || ch == '-')
        {
            parseNumber();
            return true;
        }
        else if (Character.isJavaIdentifierStart(ch) || ch == '<')
        {
            tokenType = TokenType.IDENTIFIER;
            parseToken("/>");
            if (token.contains(":"))
            {
                tokenVal = token.substring(0, token.length() - 1);
                verifyLabel();
            }
            else
                tokenVal = token;
            return true;
        }
        else if (ch == '@') //TypeSignature
        {
            tokenType = TokenType.TYPE_SIGNATURE;
            parseToken("[/;()");
            tokenVal = token.substring(1);
            return true;
        }
        else if (ch == '&') //MethodHandle
        {
            tokenType = TokenType.HANDLE;
            parseToken("");
            tokenVal = token.substring(1);
            return true;
        }
        else if (ch == COMMENT) //Comment
        {
            tokenStartIndex = -1;
            tokenEndIndex = -1;
            commentStartInd = colNum;
            return true;
        }
        else if (ch == ',')
        {
            if (hasArgumentSeparator)
            {
                error("Unexpected comma.", characterRange(lineNum, colNum));
                argumentError = true;
            }
            hasArgumentSeparator = true;
            return false;
        }
        else
        {
            if (!Character.isWhitespace(ch))
            {
                error("Illegal character.", characterRange(lineNum, colNum));
                tokenError = true;
            }
            return false;
        }
    }

    /**
     * Increments character position, searching for the next token beginning.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    private void readNextToken()
    {
        if (lineNum < 1)
            throw new IllegalStateException("Not currently reading a line.");

        tokenStartIndex = -1;
        tokenEndIndex = -1;
        hasArgumentSeparator = false;
        argumentError = false;
        tokenError = false;
        while (colNum < line.length())
        {
            if (identifyTokenStart())
                break;
            colNum++;
        }

        if (tokenStartIndex != -1)
        {
            tokenNum++;
            if (tokenNum >= prevTokens.size())
                prevTokens.add(tokenStartIndex);
        }
        else
            tokenNum = -1;
    }

    /**
     * Verifies that this label identifier has the correct syntax. This will emit any errors if necessary.
     */
    private void verifyLabel()
    {
        for (int i = 0; i < token.length(); i++)
        {
            char ch = token.charAt(i);
            if (i == 0 ? !Character.isJavaIdentifierStart(ch) : !Character.isJavaIdentifierPart(ch))
            {
                error("Illegal character.", characterRange(lineNum, tokenStartIndex + i));
                tokenError = true;
                return;
            }
        }
    }

    /**
     * Parses a number value.
     */
    private void parseNumber()
    {
        parseToken(".");
        boolean isLong = token.toUpperCase().endsWith("L");
        boolean isDouble = token.toUpperCase().endsWith("D");
        boolean isFloat = token.toUpperCase().endsWith("F");
        String str = token;
        if (isLong || isDouble || isFloat)
            str = str.substring(0, str.length() - 1);

        FallibleFunction<Number, String> parsing;
        tokenVal = null;
        if (isFloat || isDouble || str.contains("."))
        {
            tokenType = isFloat ? TokenType.FLOAT : TokenType.DOUBLE;
            parsing = isFloat ? Float::parseFloat : Double::parseDouble;
            if (isLong)
            {
                tokenError = true;
                error("Illegal type suffix.", getTokenPos());
            }
        }
        else
        {
            tokenType = isLong ? TokenType.LONG : TokenType.INTEGER;
            parsing = isLong ? Long::decode : Integer::decode;
        }
        tokenVal = FallibleFunction.tryOptional(parsing, str).orElse(null);
        if (tokenVal == null)
            error("Invalid number.", tokenRange(lineNum, tokenStartIndex, tokenEndIndex));
    }

    /**
     * Parses a normal token, consisting of any valid java identifiers and specified special characters. This will stop
     * when it encounters a whitespace or a pound-sign comment (#). This also allows a special case (allowing ':') if
     * the token type is identifier to account for labels. Reaching the end of the colon will then act as a word break.
     * @param specialChars special characters that should also be acceptable.
     */
    private void parseToken(String specialChars)
    {
        tokenStartIndex = colNum;

        StringBuilder sb = new StringBuilder();
        sb.append(line.charAt(colNum));
        colNum++; //ignore the first character
        while (colNum < line.length())
        {
            char ch = line.charAt(colNum);
            if (Character.isWhitespace(ch) || ch == COMMENT || ch == ',')
                break;
            else if (ch == ':' && tokenType == TokenType.IDENTIFIER)
            {
                tokenType = TokenType.LABEL;
                colNum++;
                break;
            }
            else if (!Character.isJavaIdentifierPart(ch) && specialChars.indexOf(ch) == -1)
            {
                tokenError = true;
                error("Illegal character.", characterRange(lineNum, colNum));
            }
            else
                sb.append(ch);
            colNum++;
        }
        tokenEndIndex = colNum;
        token = sb.toString();
    }

    /**
     * Parses a string token that has been demarcated by quotation marks ("). This will ensure that all valid escape
     * codes will be parsed correctly. If at any point the provided string token is malformed, this will log an error
     * and return a null string.
     */
    private void parseStringToken()
    {
        tokenStartIndex = colNum;
        if (line.charAt(colNum) != '"')
            throw new IllegalStateException("Not a string token.");

        StringBuilder ret = new StringBuilder(line.length() - colNum);
        String errorMsg = null;
        boolean quoted = false;
        boolean escaped = false;
        mainLoop: while (++colNum < line.length())
        {
            char c = line.charAt(colNum);
            if (escaped)
            {
                int charSize = 4;
                escaped = false;
                switch (c)
                {
                    case '"': ret.append('"'); break;
                    case '\'': ret.append('\''); break;
                    case '\\': ret.append('\\'); break;
                    case '0': ret.append('\000'); break;
                    case 'n': ret.append('\n'); break;
                    case 'r': ret.append('\r'); break;
                    case 't': ret.append('\t'); break;
                    case 'x':
                        charSize = 2;
                        //fall-through
                    case 'u':
                        if (line.length() - colNum - 1 < charSize) {
                            if (errorMsg == null)
                                errorMsg = "Invalid hexadecimal.";
                            continue mainLoop;
                        }
                        String point = line.substring(colNum + 1, colNum + 1 + charSize).toUpperCase();
                        for (char hex : point.toCharArray())
                        {
                            if (!Character.isLetterOrDigit(hex) || hex > 'F') {
                                if (errorMsg == null)
                                    errorMsg = "Invalid hexadecimal.";
                                continue mainLoop;
                            }
                        }
                        ret.append((char)Integer.parseInt(point, HEX_RADIX));
                        colNum += charSize;
                        break;
                    default:
                        if (errorMsg == null)
                            errorMsg = "Invalid hexadecimal.";
                }
            }
            else if (c == '"')
            {
                quoted = true;
                colNum++;
                break;
            }
            else if (c == '\\')
                escaped = true;
            else
                ret.append(c);
        }
        if (escaped)
            errorMsg = "Unexpected end of input: open escape.";
        else if (!quoted)
            errorMsg = "Unexpected end of input: no end of quote.";

        tokenEndIndex = colNum;
        tokenType = TokenType.STRING;
        token = line.substring(tokenStartIndex, tokenEndIndex);
        if (errorMsg == null)
            tokenVal = ret.toString();
        else
        {
            tokenError = true;
            error(errorMsg, tokenRange(lineNum, tokenStartIndex, colNum));
            tokenVal = null;
        }
    }

}
