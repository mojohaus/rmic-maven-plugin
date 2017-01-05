package org.codehaus.mojo.rmic;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.plexus.util.ReflectionUtils.setVariableValueInObject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

/**
 * A test version of the RmiCompiler.
 */
class TestRmiCompiler extends SunRmiCompiler
{
    List<Invocation> invocations = new ArrayList<>();

    /**
     * Sets this compiler as the active one for the specified mojo.
     *
     * @param mojo the mojo to update
     * @throws IllegalAccessException should never be thrown
     */
    void setInMojo( RmicMojo mojo ) throws IllegalAccessException
    {
        setVariableValueInObject( mojo, "rmiCompiler", this );
    }

    /**
     * Returns the single invocation made by this compiler. Fails if there were either more than one or none.
     */
    Invocation getInvocation()
    {
        assertThat( invocations.size(), equalTo( 1 ) );
        return invocations.get( 0 );
    }

    /**
     * Returns the specified invocation made by this compiler. Fails if it does not exist.
     * @param i the index of the invocation to retrieve
     */
    Invocation getInvocation( int i )
    {
        assertThat( invocations.size(), greaterThan( i ) );
        return invocations.get( i );
    }

    @Override
    protected void compileInProcess( String[] args, CompilerConfiguration config )
            throws CompilerException
    {
        invocations.add( new Invocation( args ) );
    }
}
