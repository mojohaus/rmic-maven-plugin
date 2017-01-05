package org.codehaus.mojo.rmic;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.List;

/**
 * A hamcrest matcher to test for the presence of sequences of arguments.
 */
class ArgumentSequenceInvocationMatcher extends TypeSafeDiagnosingMatcher<Invocation>
{
    private String[] arguments;

    private ArgumentSequenceInvocationMatcher( String... arguments )
    {
        this.arguments = arguments;
    }

    static ArgumentSequenceInvocationMatcher hasArgument( Object argument1 )
    {
        return new ArgumentSequenceInvocationMatcher( argument1.toString() );
    }

    static ArgumentSequenceInvocationMatcher hasArgumentSequence( Object argument1, Object argument2 )
    {
        return new ArgumentSequenceInvocationMatcher( argument1.toString(), argument2.toString() );
    }

    @Override
    protected boolean matchesSafely( Invocation invocation, Description description )
    {
        if ( hasSequence( invocation ) )
        {
            return true;
        }

        description.appendValueList( "not found in {", ",", "}", invocation.getArguments() );
        return false;
    }

    private boolean hasSequence( Invocation invocation )
    {
        List<String> actual = invocation.getArguments();
        int i = actual.indexOf( arguments[0] );
        if ( i < 0 )
        {
            return false;
        }

        for ( int j = 1; j < arguments.length; j++ )
        {
            if ( ( i + 1 ) >= actual.size() || !arguments[j].equals( actual.get( i + 1 ) ) )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo( Description description )
    {
        if ( arguments.length > 1 )
        {
            description.appendValueList( "sequence of [", ",", "]", arguments );
        }
        else
        {
            description.appendValue( arguments[0] );
        }
    }
}
