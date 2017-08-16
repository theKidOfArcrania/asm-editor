package com.theKidOfArcrania.asm.editor.code.parsing;

import com.theKidOfArcrania.asm.editor.context.TypeSignature;
import com.theKidOfArcrania.asm.editor.context.TypeSort;

import static com.theKidOfArcrania.asm.editor.code.parsing.Range.*;
import static com.theKidOfArcrania.asm.editor.context.ClassContext.verifyClassNameFormat;
import static com.theKidOfArcrania.asm.editor.context.TypeSignature.parseTypeSig;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;

/**
 * Represents the basic parameter types
 * @author Henry
 */
public enum BasicParamType implements ParamType
{
    INTEGER("integer number", TokenType.INTEGER),
    LONG("long integer number", TokenType.LONG),
    FLOAT("floating number", TokenType.FLOAT),
    DOUBLE("double-precision floating number", TokenType.DOUBLE),
    STRING("string", TokenType.STRING){
        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            return reader.getTokenValue() != null;
        }
    }, METHOD_SIGNATURE("method signature", TokenType.TYPE_SIGNATURE) {
        @Override
        public boolean matches(CodeTokenReader reader)
        {
            return super.matches(reader) && reader.getToken().contains("(");
        }

        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            return super.matches(reader) && validTypeSig(reader, true);
        }
    }, FIELD_SIGNATURE("type signature", TokenType.TYPE_SIGNATURE) {
        @Override
        public boolean matches(CodeTokenReader reader)
        {
            return super.matches(reader) && !reader.getToken().contains("(");
        }

        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            return super.matches(reader) && validTypeSig(reader, false);
        }
    }, ARRAY_SIGNATURE("array descriptor", TokenType.TYPE_SIGNATURE) {
        @Override
        public boolean matches(CodeTokenReader reader)
        {
            return super.matches(reader) && !reader.getToken().contains("(") && reader.getToken().contains("[");
        }

        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            if (!super.matches(reader))
                return false;

            String token = (String)reader.getTokenValue();
            TypeSignature sig = parseTypeSig(token);
            if (sig == null)
            {
                reader.error("Cannot resolve type signature '" + token + "'.", reader.getTokenPos());
                return false;
            }

            if (sig.getSort() != TypeSort.ARRAY)
            {
                reader.errorExpected(getName());
                return false;
            }
            return true;
        }
    }, CLASS_NAME("class identifier", TokenType.IDENTIFIER) {
        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            if (!super.checkToken(reader))
                return false;
            String className = (String)reader.getTokenValue();
            if (!verifyClassNameFormat(className))
            {
                reader.error("Cannot resolve class name '" + className + "'.", reader.getTokenPos());
                return false;
            }
            return true;
        }
    }, IDENTIFIER("identifier", TokenType.IDENTIFIER) {
        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            if (!super.checkToken(reader))
                return false;
            String token = (String)reader.getTokenValue();

            //Allow the constructor for now because we don't know if this is a method or field identifier.
            if (token.equals("<init>"))
                return true;

            for (int i = 0; i < token.length(); i++)
            {
                if (i == 0 ? !isJavaIdentifierStart(token.charAt(i)) : !isJavaIdentifierPart(token.charAt(i)))
                {
                    reader.error("Illegal character.", characterRange(reader.getLineNumber(), reader
                            .getTokenStartIndex() + i));
                    return false;
                }
            }
            return true;
        }
    }, METHOD_HANDLE("handle", TokenType.HANDLE) {
        @Override
        public boolean checkToken(CodeTokenReader reader)
        {
            if (!super.checkToken(reader))
                return false;
            String token = (String)reader.getTokenValue();
            for (int i = 1; i < token.length(); i++)
            {
                if (i == 1 ? !isJavaIdentifierStart(token.charAt(i)) : !isJavaIdentifierPart(token.charAt(i)))
                {
                    reader.error("Illegal character.", characterRange(reader.getLineNumber(), reader
                            .getTokenStartIndex() + i));
                    return false;
                }
            }
            return true;
        }
    };

    /**
     * Determines whether if this token contains a valid signature (method or field).
     * @param reader the token-reader
     * @param methodType whether to expect a method or field signature.
     * @return true if valid, false if invalid
     */
    private static boolean validTypeSig(CodeTokenReader reader, boolean methodType)
    {
        String token = (String)reader.getTokenValue();
        if (parseTypeSig(token) == null)
        {
            reader.error("Cannot resolve type signature '" + token + "'.", reader.getTokenPos());
            return false;
        }

        boolean methodSig = token.contains("(");
        if (methodType && !methodSig)
            reader.errorExpected("method signature");
        else if (!methodType && methodSig)
            reader.errorExpected("type signature");
        else
            return true;
        return false;
    }

    private final String name;
    private final TokenType reqTokenType;

    /**
     * Constructs a basic param type.
     * @param name the extended user-friendly name.
     * @param reqTokenType the required token type for parameter.
     */
    BasicParamType(String name, TokenType reqTokenType)
    {
        this.name = name;
        this.reqTokenType = reqTokenType;
    }


    @Override
    public boolean matches(CodeTokenReader reader)
    {
        return reader.getTokenType() == reqTokenType;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
