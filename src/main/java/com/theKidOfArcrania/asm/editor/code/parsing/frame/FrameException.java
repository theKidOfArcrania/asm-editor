package com.theKidOfArcrania.asm.editor.code.parsing.frame;

/**
 * This is an exception that is thrown whenever a illegal stack operation has been performed. The associated message
 * will explain the error message.
 * @author Henry Wang
 */
public class FrameException extends Exception
{
    private static final long serialVersionUID = 4961800575541837476L;

    /**
     * Constructs a new FrameException with a message field.
     * @param msg the message with this stack exception.
     */
    public FrameException(String msg)
    {
        super(msg);
    }
}
