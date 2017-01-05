package org.codehaus.mojo.rmic;

import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a single invocation of the rmic compiler.
 */
class Invocation
{
    private List<String> arguments;

    Invocation( String[] arguments )
    {
        this.arguments = Arrays.asList( arguments );
    }

    @SuppressWarnings( "unchecked" )
    List<String> getArguments()
    {
        return Collections.unmodifiableList( arguments );
    }

    public String toString()
    {
        return "Invocation{" +
                "arguments=" + arguments +
                '}';
    }
}
