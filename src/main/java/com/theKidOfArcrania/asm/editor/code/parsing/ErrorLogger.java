package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * This allows a client to listen to any errors emitted when a body of code is parsed with {@link CodeTokenReader}.
 * @author Henry Wang
 */
public interface ErrorLogger
{
    /**
     * Logs an error with this error logger.
     * @param description the error message description
     * @param highlight specifies the position that this error is highlighting, can be null.
     */
    void logError(String description, Range highlight);

    /**
     * Logs a warning with this error logger.
     * @param description the warning message description
     * @param highlight specifies the position that this warning is highlighting, can be null.
     */
    void logWarning(String description, Range highlight);
}
