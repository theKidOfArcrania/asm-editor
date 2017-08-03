package com.theKidOfArcrania.asm.editor.code.parsing;

/**
 * Represents all the possible token types that can be parsed by the {@link CodeTokenReader}.
 */
public enum TokenType
{
    INTEGER, LONG, FLOAT, DOUBLE, STRING, TYPE_SIGNATURE /*Field signature or method signature*/,
    IDENTIFIER /*Class name or identifier*/, HANDLE, LABEL
}
