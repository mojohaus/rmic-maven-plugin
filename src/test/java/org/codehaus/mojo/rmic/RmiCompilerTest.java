package org.codehaus.mojo.rmic;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.meterware.simplestub.Stub.createStub;
import static org.codehaus.mojo.rmic.ArgumentSequenceInvocationMatcher.hasArgument;
import static org.codehaus.mojo.rmic.ArgumentSequenceInvocationMatcher.hasArgumentSequence;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;

public class RmiCompilerTest
{
    private static final String OUTPUT_LOCATION = "target/rmi-classes";
    private static final String CLASSPATH_ENTRY_1 = new File( "target/classes" ).getAbsolutePath();
    private static final String CLASSPATH_ENTRY_2 = new File( "target/generated-classes/foobar" ).getAbsolutePath();
    private static final String PATH_SEPARATOR = System.getProperty( "path.separator" );

    private final RmiCompilerConfiguration config = new RmiCompilerConfiguration();
    private final TestRmiCompiler testRmiCompiler = new TestRmiCompiler();

    @Before
    public void init() throws Exception
    {
        config.setOutputLocation( OUTPUT_LOCATION );
        testRmiCompiler.setLog( createStub( Log.class ) );
    }

    @Test
    public void whenCompilerInvoked_specifyOutputLocation()
            throws Exception
    {
        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgumentSequence( "-d", OUTPUT_LOCATION ) );
    }

    @Test
    public void whenSingleClasspathEntry_defineClasspathArgument()
            throws Exception
    {
        config.addClasspathEntry( CLASSPATH_ENTRY_1 );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgumentSequence( "-classpath", CLASSPATH_ENTRY_1 ) );
    }

    @Test
    public void whenMultipleClasspathEntries_defineClasspathArgument()
            throws Exception
    {
        config.addClasspathEntry( CLASSPATH_ENTRY_1 );
        config.addClasspathEntry( CLASSPATH_ENTRY_2 );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(),
                hasArgumentSequence( "-classpath", CLASSPATH_ENTRY_1 + PATH_SEPARATOR + CLASSPATH_ENTRY_2 ) );
    }

    @Test
    public void whenVersionSpecified_addToCommandLine()
            throws Exception
    {
        config.setVersion( "compat" );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-vcompat" ) );
    }

    @Test
    public void whenIiopSpecified_addCommandSwitch()
            throws Exception
    {
        config.setIiop( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-iiop" ) );
    }

    @Test
    public void whenIiopAndPoaBothSpecified_addCommandSwitches()
            throws Exception
    {
        config.setIiop( true );
        config.setPoa( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), both( hasArgument( "-iiop" ) ).and( hasArgument( "-poa" ) ) );
    }

    @Test
    public void whenIiopAndPoaAndNoLocalStubsAllSpecified_addCommandSwitches()
            throws Exception
    {
        config.setIiop( true );
        config.setPoa( true );
        config.setNoLocalStubs( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(),
                    allOf( hasArgument( "-iiop" ), hasArgument( "-poa" ), hasArgument( "-nolocalstubs" ) ) );
    }

    @Test(expected = RmiCompilerException.class)
    public void whenPoaSpecifiedWithoutIiop_throwException()
            throws Exception
    {
        config.setPoa( true );

        testRmiCompiler.execute( config );
    }

    @Test
    public void whenIdlSpecified_addCommandSwitch()
            throws Exception
    {
        config.setIdl( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-idl" ) );
    }

    @Test
    public void whenIdlAndNoValueSpecified_addCommandSwitches()
            throws Exception
    {
        config.setIdl( true );
        config.setNoValueMethods( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), allOf( hasArgument( "-idl" ), hasArgument( "-noValueMethods" ) ) );
    }

    @Test
    public void whenKeepSpecified_addCommandSwitch()
            throws Exception
    {
        config.setKeep( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-keep" ) );
    }

    @Test
    public void whenVerboseSpecified_addCommandSwitch()
            throws Exception
    {
        config.setVerbose( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-verbose" ) );
    }

    @Test
    public void whenNoWarnSpecified_addCommandSwitch()
            throws Exception
    {
        config.setNowarn( true );

        testRmiCompiler.execute( config );

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-nowarn" ) );
    }

}
