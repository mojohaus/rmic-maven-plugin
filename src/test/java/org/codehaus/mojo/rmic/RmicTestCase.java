package org.codehaus.mojo.rmic;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.meterware.simplestub.Stub.createStub;
import static org.codehaus.mojo.rmic.ArgumentSequenceInvocationMatcher.hasArgument;
import static org.codehaus.mojo.rmic.ArgumentSequenceInvocationMatcher.hasArgumentSequence;
import static org.codehaus.plexus.util.ReflectionUtils.setVariableValueInObject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

public class RmicTestCase
{
    private static File DEFAULT_PROJECT_OUTPUT_DIRECTORY = new File( "target/classes" ).getAbsoluteFile();
    private static File DEFAULT_RMIC_OUTPUT_DIRECTORY = new File( "target/rmi-classes" ).getAbsoluteFile();
    private static final ArrayList EMPTY_LIST = new ArrayList();

    private static Set<String> DEFAULT_INCLUDES = newSet( "**/*" );
    private static Set<String> DEFAULT_EXCLUDES = newSet( "**/*_Stub.class" );

    private static Set<String> INCLUDES_A = newSet( "a/b/*" );
    private static Set<String> INCLUDES_B = newSet( "c/d/*" );
    private final TestRmiCompiler testRmiCompiler = new TestRmiCompiler();

    private static Set<String> newSet( String contents )
    {
        HashSet<String> result = new HashSet<>();
        result.add( contents );
        return result;
    }

    private final TestDependencies dependencies = new TestDependencies();
    private final RmicMojo mojo = new RmicMojo( dependencies );
    private final ScannableFileSystem fileSystem = new ScannableFileSystem();

    @Before
    public void setUp() throws Exception
    {
        testRmiCompiler.setInMojo( mojo );
        setSourceDirectory( DEFAULT_PROJECT_OUTPUT_DIRECTORY );
        setTargetDirectory( DEFAULT_RMIC_OUTPUT_DIRECTORY );
        setCompileClasspathElements( EMPTY_LIST );
        mojo.setLog( createStub( Log.class ) );
    }

    @Test
    public void withDefaultConfiguration_shouldProcessRemoteClassesOnly() throws MojoExecutionException
    {
        defineDefaultScan();

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                allOf(
                        hasArgument( "a.b.RemoteClass1" ), hasArgument( "a.b.RemoteClass2" ),
                        not( hasArgument( "a.b.RemoteInterface" ) ), not( hasArgument( "a.b.NonRmicClass" ) ),
                        hasArgumentSequence( "-classpath", DEFAULT_PROJECT_OUTPUT_DIRECTORY ),
                        hasArgumentSequence( "-d", DEFAULT_RMIC_OUTPUT_DIRECTORY )
                )
        );
    }

    private void defineDefaultScan()
    {
        Set<File> scanResults = new HashSet<>();
        scanResults.add( defineNonRemoteClass( "a.b.NonRmicClass" ) );
        scanResults.add( defineRemoteInterface( "a.b.RemoteInterface" ) );
        scanResults.add( defineRemoteClass( "a.b.RemoteClass1" ) );
        scanResults.add( defineRemoteClass( "a.b.RemoteClass2" ) );
        fileSystem.defineExpectedScan( DEFAULT_PROJECT_OUTPUT_DIRECTORY, DEFAULT_INCLUDES, DEFAULT_EXCLUDES,
                scanResults );
    }

    @Test
    public void whenClasspathElementsSet_compilerSpecifiesClasspath() throws MojoExecutionException,
            NoSuchFieldException, IllegalAccessException
    {
        defineDefaultScan();
        setCompileClasspathElements( new ArrayList<>( Arrays.asList( new String[]{"xy"} ) ) );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                hasArgumentSequence( "-classpath", "xy" + File.pathSeparator + DEFAULT_PROJECT_OUTPUT_DIRECTORY ) );
    }

    @Test
    public void withIiopEnabled_shouldProcessRemoteClassesAndInterfaces()
            throws MojoExecutionException, NoSuchFieldException, IllegalAccessException
    {
        defineDefaultScan();

        mojo.setIiop( true );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                allOf( hasArgument( "a.b.RemoteClass1" ), hasArgument( "a.b.RemoteClass2" ),
                        hasArgument( "a.b.RemoteInterface" ), not( hasArgument( "a.b.NonRmicClass" ) )
                ) );
    }

    @Test
    public void withIiopEnabled_maySpecifyNoLocalStubs()
            throws MojoExecutionException, NoSuchFieldException, IllegalAccessException
    {
        defineDefaultScan();
        mojo.setIiop( true );
        mojo.setNoLocalStubs( true );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                both( hasArgument( "-iiop" ) ).and( hasArgument( "-nolocalstubs" ) ) );
    }

    @Test
    public void withIDLSpecifiedSetIDLSwitch() throws Exception
    {
        defineDefaultScan();
        Source source = addNewSource();
        source.setIdl( true );
        source.setNoValueMethods( true );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                both( hasArgument( "-idl" ) ).and( hasArgument( "-noValueMethods" ) ) );
    }

    @Test
    public void withKeepAndWarnSpecifiedSetSwitches() throws Exception
    {
        defineDefaultScan();
        Source source = addNewSource();
        source.setKeep( true );
        source.setNowarn( true );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                both( hasArgument( "-keep" ) ).and( hasArgument( "-nowarn" ) ) );
    }

    @Test(expected = MojoExecutionException.class)
    public void whenMojoOptionsSetWhileSourceSpecified_throwException() throws Exception
    {
        defineDefaultScan();
        mojo.setIiop( true );

        Source source = addNewSource();
        source.setKeep( true );
        source.setNowarn( true );

        mojo.execute();
    }

    @Test
    public void whenMojoOptionsSetWhileSourceSpecified_exceptionContainsSuperflousSwitches() throws Exception
    {
        defineDefaultScan();
        mojo.setIiop( true );
        mojo.setExcludes( new HashSet<>( Arrays.asList( "abc", "cde" ) ) );

        Source source = addNewSource();
        source.setKeep( true );
        source.setNowarn( true );

        try
        {
            mojo.execute();
        }
        catch ( MojoExecutionException e )
        {
            assertThat( e.getMessage(), containsString( "excludes, iiop" ));
        }
    }

    @Test
    public void whenMojoOptionsSetToDefaultWhileSourceSpecified_useSourceOptions() throws Exception
    {
        defineDefaultScan();
        mojo.setIiop( false );

        Source source = addNewSource();
        source.setKeep( true );
        source.setNowarn( true );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                allOf( hasArgument( "-keep" ), hasArgument( "-nowarn" ), not( hasArgument( "-iiop" ) ) ) );
    }

    @Test
    public void withVerboseAndVersionSpecifiedSetSwitches() throws Exception
    {
        defineDefaultScan();
        mojo.setVerbose( true );
        mojo.setVersion( "1.2" );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(),
                both( hasArgument( "-verbose" ) ).and( hasArgument( "-v1.2" ) ) );
    }

    @Test(expected = MojoExecutionException.class)
    public void whenPoaSpecifiedAndNotIiop_throwException() throws Exception
    {
        defineDefaultScan();
        mojo.setPoa( true );

        mojo.execute();
    }

    @Test
    public void whenPoaSpecifiedSetSwitches() throws Exception
    {
        defineDefaultScan();
        mojo.setIiop( true );
        mojo.setPoa( true );

        mojo.execute();

        assertThat( testRmiCompiler.getInvocation(), hasArgument( "-poa" ) );
    }

    @Test
    public void whenTwoSourcesInvokeTwice() throws Exception
    {
        Set<File> scanResults1 = new HashSet<>();
        scanResults1.add( defineNonRemoteClass( "a.b.NonRmicClass" ) );
        scanResults1.add( defineRemoteInterface( "a.b.RemoteInterface" ) );
        scanResults1.add( defineRemoteClass( "a.b.RemoteClass1" ) );
        scanResults1.add( defineRemoteClass( "a.b.RemoteClass2" ) );

        Set<File> scanResults2 = new HashSet<>();
        scanResults2.add( defineNonRemoteClass( "c.d.NonRmicClass" ) );
        scanResults2.add( defineRemoteInterface( "c.d.RemoteInterface" ) );
        scanResults2.add( defineRemoteClass( "c.d.RemoteClass1" ) );
        scanResults2.add( defineRemoteClass( "c.d.RemoteClass2" ) );
        fileSystem.defineExpectedScan( DEFAULT_PROJECT_OUTPUT_DIRECTORY, INCLUDES_A, DEFAULT_EXCLUDES, scanResults1 );
        fileSystem.defineExpectedScan( DEFAULT_PROJECT_OUTPUT_DIRECTORY, INCLUDES_B, DEFAULT_EXCLUDES, scanResults2 );

        Source source1 = addNewSource();
        defineIncludes( source1, INCLUDES_A );
        source1.setIiop( true );

        Source source2 = addNewSource();
        defineIncludes( source2, INCLUDES_B );
        source2.setIiop( true );
        source2.setPoa( true );
        mojo.execute();

        assertThat( testRmiCompiler.getInvocation( 0 ),
                allOf(
                        hasArgument( "a.b.RemoteClass1" ), hasArgument( "a.b.RemoteClass2" ),
                        not( hasArgument( "c.d.RemoteClass1" ) ), not( hasArgument( "-poa" ) )
                ) );

        assertThat( testRmiCompiler.getInvocation( 1 ),
                allOf(
                        hasArgument( "c.d.RemoteClass1" ), hasArgument( "c.d.RemoteClass2" ),
                        not( hasArgument( "a.b.RemoteClass1" ) ), not( hasArgument( "a.b.RemoteClass2" ) ),
                        hasArgument( "-poa" )
                ) );

    }

    // todo test compiler selection

    private File defineNonRemoteClass( String className )
    {
        return defineClassFile( DEFAULT_PROJECT_OUTPUT_DIRECTORY, className, NonRmicClass.class );
    }

    private File defineRemoteClass( String className )
    {
        return defineClassFile( DEFAULT_PROJECT_OUTPUT_DIRECTORY, className, RmicClass.class );
    }

    private File defineRemoteInterface( String className )
    {
        return defineClassFile( DEFAULT_PROJECT_OUTPUT_DIRECTORY, className, RmicInterface.class );
    }

    private File defineClassFile( File root, String className, Class aClass )
    {
        return fileSystem.defineFile( new File( root, toClassFileName( className ) ), aClass );
    }

    private String toClassFileName( String className )
    {
        return className.replace( '.', '/' ) + ".class";
    }

    private Source addNewSource() throws NoSuchFieldException, IllegalAccessException
    {
        List<Source> sources = mojo.getSources();
        if ( sources == null )
        {
            sources = new ArrayList<>();
            setVariableValueInObject( mojo, "sources", sources );
        }
        Source source = new Source();
        sources.add( source );
        return source;
    }

    private void setSourceDirectory( File directory ) throws NoSuchFieldException, IllegalAccessException
    {
        setVariableValueInObject( mojo, "classesDirectory", directory );
    }

    private void defineIncludes( Source config, Set includes ) throws NoSuchFieldException,
            IllegalAccessException
    {
        setVariableValueInObject( config, "includes", includes );
    }

    private void setTargetDirectory( File directory ) throws NoSuchFieldException, IllegalAccessException
    {
        setVariableValueInObject( mojo, "outputDirectory", directory );
    }

    private void setCompileClasspathElements( List elements ) throws NoSuchFieldException, IllegalAccessException
    {
        setVariableValueInObject( mojo, "projectCompileClasspathElements", elements );
    }


    private static class ExpectedScan
    {
        private File root;
        private Set<String> includes;
        private Set<String> excludes;
        private Set<File> results;

        ExpectedScan( File root, Set<String> includes, Set<String> excludes, Set<File> results )
        {
            this.root = root;
            this.includes = includes;
            this.excludes = excludes;
            this.results = results;
        }

        boolean isExpectedScan( File root, Set<String> includes, Set<String> excludes )
        {
            return root.equals( this.root ) && includes.equals( this.includes ) && excludes.equals( this.excludes );
        }
    }

    private class ScannableFileSystem
    {
        Map<File, Object> files = new HashMap<>();
        List<ExpectedScan> scans = new ArrayList<>();

        private Set<File> getFiles( File root, Set<String> includes, Set<String> excludes )
        {
            for ( Object scan1 : scans )
            {
                ExpectedScan scan = (ExpectedScan) scan1;
                if ( scan.isExpectedScan( root, includes, excludes ) )
                {
                    return scan.results;
                }
            }
            fail( "Unexpected call: scanner.getIncludesSources( " + root + ", " + includes + ", " + excludes + ")" );
            return null;
        }


        private Object getFileContents( File file )
        {
            return files.get( file );
        }

        private File defineFile( File file, Object contents )
        {
            files.put( file.getAbsoluteFile(), contents );
            return file;
        }

        void defineExpectedScan( File root, Set<String> includes, Set<String> excludes, Set<File> scanResults )
        {
            scans.add( new ExpectedScan( root, includes, excludes, scanResults ) );
        }
    }

    private class TestScanner implements SourceInclusionScanner
    {
        private Set<String> includes;
        private Set<String> excludes;

        TestScanner( Set<String> includes, Set<String> excludes )
        {
            this.includes = includes;
            this.excludes = excludes;
        }

        public void addSourceMapping( SourceMapping sourceMapping )
        {
        }

        public Set<File> getIncludedSources( File source, File target ) throws InclusionScanException
        {
            return fileSystem.getFiles( source, includes, excludes );
        }
    }

    private class TestDependencies implements AbstractRmiMojo.DependenciesFacade
    {
        private URL[] classpathUrls;

        public boolean fileExists( File includeFile )
        {
            return false;
        }

        public SourceInclusionScanner createSourceInclusionScanner( int staleMillis, Set<String> includes,
                                                                    Set<String> excludes )
        {
            return new TestScanner( includes, excludes );
        }

        public Class loadClass( String className ) throws ClassNotFoundException
        {
            for ( URL classpathUrl : classpathUrls )
            {
                Object object = fileSystem.getFileContents( new File( toFile( classpathUrl ), toClassFileName(
                        className ) ) );
                if ( object instanceof Class )
                {
                    return (Class) object;
                }
            }
            throw new ClassNotFoundException( className );
        }

        private File toFile( URL url )
        {
            return new File( url.getPath() );
        }

        public void defineUrlClassLoader( URL[] classpathUrls )
        {
            this.classpathUrls = classpathUrls;
        }
    }

    private static class NonRmicClass
    {
    }

    private static class RmicClass implements Remote
    {
    }

    interface RmicInterface extends Remote
    {
    }


}
