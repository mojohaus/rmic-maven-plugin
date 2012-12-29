package org.codehaus.mojo.rmic;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;

import javax.print.attribute.standard.MediaSize.ISO;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.util.Os;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class SunRmiCompilerTest
{
    private SunRmiCompiler rmiCompiler;

    @Captor
    private ArgumentCaptor<String[]> captor;

    @Before
    public void init()
    {
        SunRmiCompiler compiler = new SunRmiCompiler()
        {
            void compileInProcess( String[] args, org.codehaus.plexus.compiler.CompilerConfiguration config )
                throws org.codehaus.plexus.compiler.CompilerException
            {
                // do nothing, just verify arguments
            };
        };
        compiler.setLog( mock( Log.class ) );
        rmiCompiler = spy( compiler );

        MockitoAnnotations.initMocks( this );
    }

    @Test
    public void testClasspathSingleEntry()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        final String classpathEntry0 = new File( "target/classes" ).getAbsolutePath(); 
        config.addClasspathEntry( classpathEntry0 );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-classpath", classpathEntry0, "-d", "target/rmi-classes" }, captor.getValue() );

    }

    @Test
    public void testClasspathMultipeEntries()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        final String classpathEntry0 = new File( "target/classes" ).getAbsolutePath(); 
        config.addClasspathEntry( classpathEntry0 );
        final String classpathEntry1 = new File( "target/generated-classes/foobar" ).getAbsolutePath(); 
        config.addClasspathEntry( classpathEntry1 );

        // execute
        rmiCompiler.execute( config );

        // verify
        String classpath;
        if( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            classpath = classpathEntry0 + ';' + classpathEntry1;
        }
        else
        {
            classpath = classpathEntry0 + ':' + classpathEntry1;
        }
        
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-classpath", classpath, "-d", "target/rmi-classes" }, captor.getValue() );

    }

    @Test
    public void testVersion()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setVersion( "compat" );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-vcompat" }, captor.getValue() );

    }

    @Test
    public void testIiop()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setIiop( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-iiop" }, captor.getValue() );
    }

    @Test
    public void testIiopAndPoa()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setIiop( true );
        config.setPoa( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-iiop", "-poa" }, captor.getValue() );
    }

    @Test
    public void testIiopAndPoaAndNoLocalStubs()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setIiop( true );
        config.setPoa( true );
        config.setNoLocalStubs( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-iiop", "-poa", "-nolocalstubs" },
                           captor.getValue() );
    }

    @Test( expected = RmiCompilerException.class )
    public void testPoaWithoutIiop()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setPoa( true );

        // execute
        rmiCompiler.execute( config );
    }

    @Test
    public void testIdl()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setIdl( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-idl" }, captor.getValue() );
    }

    @Test
    public void testIdlAndNoValueMethods()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setIdl( true );
        config.setNoValueMethods( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-idl", "-noValueMethods" }, captor.getValue() );
    }

    @Test
    public void testKeep()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setKeep( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-keep" }, captor.getValue() );
    }

    @Test
    public void testVerbose()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setVerbose( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-verbose" }, captor.getValue() );
    }

    @Test
    public void testNoWarn()
        throws Exception
    {
        // prepare
        RmiCompilerConfiguration config = new RmiCompilerConfiguration();
        config.setOutputLocation( "target/rmi-classes" );
        config.setNowarn( true );

        // execute
        rmiCompiler.execute( config );

        // verify
        verify( rmiCompiler ).compileInProcess( captor.capture(), isNull( CompilerConfiguration.class ) );
        assertArrayEquals( new String[] { "-d", "target/rmi-classes", "-nowarn" }, captor.getValue() );
    }

}
