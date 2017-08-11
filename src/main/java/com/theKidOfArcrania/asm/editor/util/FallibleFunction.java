package com.theKidOfArcrania.asm.editor.util;

import java.util.Optional;

/**
 * Represents a function that can be "fallible" in that some errors/exceptions can be thrown during execution.
 * @author Henry Wang
 */
@FunctionalInterface
public interface FallibleFunction<R, P>
{
    /**
     * Tries an function in a try-catch block. If this action fails, then we return an empty optional object.
     * @param trying the function to try.
     * @param param parameter to pass to function.
     * @param <R> type of value to return.
     * @param <P> type of parameter to pass.
     * @return the optional construct
     */
    static <R, P> Optional<R> tryOptional(FallibleFunction<R, P> trying, P param)
    {
        try
        {
            return Optional.of(trying.tryApply(param));
        }
        catch (Throwable e)
        {
            return Optional.empty();
        }
    }

    /**
     * The target method to implement. This is called to try a particular action with a parameter and return.
     * @param param the parameter to pass into function
     * @return the return value of the function
     * @throws Throwable if something goes wrong.
     */
    R tryApply(P param) throws Throwable;
}
