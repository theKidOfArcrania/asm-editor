package com.theKidOfArcrania.asm.editor.code.parsing;

import java.io.Serializable;

/**
 * Represents a parameter type. Each parameter type represents a particular syntax/format that when parsed will
 * return a specific type of value represented as a token.
 * @author Henry Wang
 */
public interface ParamType extends Serializable
{
    /**
     * Obtains the exact type of this token. By default, if this token matches up with this parameter type, it will
     * return itself. Otherwise, it will return null. Classes that implement this interface should override this
     * method if it can be more specific as to what parameter type this token is.
     * @param reader the token-reader
     * @return the exact parameter type, if it matches.
     */
    default ParamType getExactType(CodeTokenReader reader)
    {
        if (matches(reader))
            return this;
        else
            return null;
    }

    /**
     * Tests whether if the current token matches up with this particular parameter type. This SHOULD NOT emit any
     * errors while parsing as this may be called multiple times, and on tokens that might not necessarily match.
     * @param reader the token-reader
     * @return true if it matches, false if it does not match.
     */
    boolean matches(CodeTokenReader reader);

    /**
     * Checks whether the current token is a valid token. This is called after we already know that this will match
     * the particular parameter type. This will also emit the appropriate errors if needed.
     * @param reader the token-reader
     * @return true if valid, false if invalid.
     */
    default boolean checkToken(CodeTokenReader reader)
    {
        if (reader.getTokenValue() == null)
        {
            reader.error("Invalid " + getExactType(reader) + ".", reader.getTokenPos());
            return false;
        }
        return true;
    }

    /**
     * Obtains the human-readable name of the parameter type
     * @return the name
     */
    String getName();
}
