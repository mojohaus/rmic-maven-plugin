package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2004-2012, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generic super class of rmi compiler mojos.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class AbstractRmiMojo
        extends AbstractMojo
        implements RmicConfig
{
    private static final String STUB_CLASS_PATTERN = "**/*_Stub.class";
    // ----------------------------------------------------------------------
    // Configurable parameters
    // ----------------------------------------------------------------------

    /**
     * A <code>List</code> of <code>Source</code> configurations to compile.
     *
     * @parameter
     */
    private List sources;

    /**
     * A list of inclusions when searching for classes to compile.
     *
     * @parameter
     */
    protected Set includes;

    /**
     * A list of exclusions when searching for classes to compile.
     *
     * @parameter
     */
    protected Set excludes;

    /**
     * The id of the rmi compiler to use.
     *
     * @parameter default-value="sun"
     */
    protected String compilerId;

    /**
     * The version of the rmi protocol to which the stubs should be compiled. Valid values include 1.1, 1.2, compat. See
     * the rmic documentation for more information. If nothing is specified the underlying rmi compiler will
     * choose the default value.  For example, in sun jdk 1.5 the default is 1.2.
     *
     * @parameter
     */
    private String version;

    /**
     * Create stubs for IIOP.
     *
     * @parameter default-value="false"
     */
    private boolean iiop;

    /**
     * Do not create stubs optimized for same process.
     *
     * @parameter
     */
    private boolean noLocalStubs;

    /**
     * Create IDL.
     *
     * @parameter default-value="false"
     */
    private boolean idl;

    /**
     * Do not generate methods for valuetypes.
     *
     * @parameter
     */
    private boolean noValueMethods;

    /**
     * Do not delete intermediate generated source files.
     *
     * @parameter default-value="false"
     */
    private boolean keep;

    /**
     * Turn off rmic warnings.
     *
     * @parameter
     */
    private boolean nowarn;

    /**
     * Enable poa generation.
     *
     * @parameter default-value="false"
     */
    private boolean poa;

    /**
     * Enable verbose output.
     *
     * @parameter default-value="false"
     */
    private boolean verbose;

    /**
     * Time in milliseconds between automatic recompilations. A value of 0 means that up to date rmic output classes
     * will not be recompiled until the source classes change.
     *
     * @parameter default-value=0
     */
    private int staleMillis;

    // ----------------------------------------------------------------------
    // Constant parameters
    // ----------------------------------------------------------------------

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /**
     * The interface between ths class and the rest of the world - unit tests replace the default implementation.
     */
    private DependenciesFacade dependencies = new DependenciesFacadeImpl();

    /**
     * The default implementation of the dependencies.
     */
    private static final DependenciesFacade DEPENDENCIES_FACADE = new DependenciesFacadeImpl();

    /**
     * Creates the abstract class using a production implementation of the dependencies.
     */
    protected AbstractRmiMojo()
    {
        this( DEPENDENCIES_FACADE );
    }


    /**
     * Creates the abstract class using the specified implementation of the dependencies.
     *
     * @param dependencies the implementation of the dependencies to use.
     */
    AbstractRmiMojo( DependenciesFacade dependencies )
    {
        this.dependencies = dependencies;
    }

    /**
     * Get the list of sub-configurations.
     */
    List getSources()
    {
        return sources;
    }

    /**
     * Get the list of elements to add to the classpath of rmic
     *
     * @return list of classpath elements
     */
    public abstract List getProjectClasspathElements();

    /**
     * Get the directory where rmic generated class files are written.
     *
     * @return the directory
     */
    public abstract File getOutputDirectory();

    /**
     * Get the directory where Remote impl classes are located.
     *
     * @return path to compiled classes
     */
    public abstract File getClassesDirectory();

    /**
     * Main mojo execution.
     *
     * @throws MojoExecutionException if there is a problem executing the mojo.
     */
    public void execute() throws MojoExecutionException
    {
        if ( sources == null || sources.isEmpty() )
        {
            sources = Arrays.asList( new Source[]{new Source()} );
        }

        for ( Iterator i = sources.iterator(); i.hasNext(); )
        {
            doExecute( (Source) i.next() );
        }
    }

    private void doExecute( Source source ) throws MojoExecutionException
    {
        source.setRmiMojo( this );

        RmiCompiler rmiCompiler = new SunRmiCompiler();
        rmiCompiler.setLog( getLog() );

        if ( !getOutputDirectory().isDirectory() )
        {
            if ( !getOutputDirectory().mkdirs() )
            {
                throw new MojoExecutionException( "Could not make output directory: " + "'"
                        + getOutputDirectory().getAbsolutePath() + "'." );
            }
        }

        try
        {
            // Get the list of classes to compile
            List remoteClassesToCompile = getRemoteClasses( source );

            if ( remoteClassesToCompile.size() == 0 )
            {
                getLog().info( "No out of date rmi classes to process." );
                return;
            }

            getLog().info( "Compiling " + remoteClassesToCompile.size() + " remote classes" );
            rmiCompiler.execute( this, source, remoteClassesToCompile );
        }
        catch ( RmiCompilerException e )
        {
            throw new MojoExecutionException( "Error while executing the RMI compiler.", e );
        }
    }

    /**
     * Get the list of elements to add to the classpath of rmic
     *
     * @return list of classpath elements
     */
    public List getRmicClasspathElements()
    {
        List classpathElements = getProjectClasspathElements();

        if ( !classpathElements.contains( getClassesDirectory().getAbsolutePath() ) )
        {
            classpathElements.add( getClassesDirectory().getAbsolutePath() );
        }

        return classpathElements;
    }

    /**
     * Search the input directory for classes to compile.
     *
     * @param source
     * @return a list of class names to rmic
     */
    public List getRemoteClasses( Source source )
    {
        List remoteClasses = new ArrayList();

        try
        {
            // Set up the classloader
            List classpathList = generateUrlCompileClasspath();
            URL[] classpathUrls = new URL[classpathList.size()];
            classpathUrls[0] = getClassesDirectory().toURI().toURL();
            classpathUrls = (URL[]) classpathList.toArray( classpathUrls );
            dependencies.defineUrlClassLoader( classpathUrls );

            // Scan for remote classes

            SourceInclusionScanner scanner = createScanner( source.getIncludes(), getExcludes( source ) );
            scanner.addSourceMapping( new SuffixMapping( ".class", "_Stub.class" ) );
            Collection staleRemoteClasses = scanner.getIncludedSources( getClassesDirectory(), getOutputDirectory() );

            for ( Iterator iter = staleRemoteClasses.iterator(); iter.hasNext(); )
            {
                String className = getClassName( (File) iter.next() );
                Class candidateClass = dependencies.loadClass( className );
                if ( isRemoteRmiClass( candidateClass ) )
                {
                    remoteClasses.add( className );
                }
            }

            // Check for classes in a classpath jar
            for ( Iterator iter = source.getIncludes().iterator(); iter.hasNext(); )
            {
                String include = (String) iter.next();
                File includeFile = new File( getClassesDirectory(), include );
                if ( ( include.indexOf( "*" ) != -1 ) || dependencies.fileExists( includeFile ) )
                {
                    continue;
                }
                // We have found a class that is not in the classes dir.
                remoteClasses.add( fileToClassName( include ) );
            }
        }
        catch ( Exception e )
        {
            getLog().warn( "Problem while scanning for classes: " + e );
        }
        return remoteClasses;
    }

    private SourceInclusionScanner createScanner( Set includes, Set excludes )
    {
        return dependencies.createSourceInclusionScanner( staleMillis, includes, excludes );
    }

    private String getClassName( File remoteClassFile )
    {
        URI relativeURI = getClassesDirectory().toURI().relativize( remoteClassFile.toURI() );
        return fileToClassName( relativeURI.toString() );
    }

    private Set getExcludes( Source source )
    {
        Set excludes = source.getExcludes();
        excludes.add( STUB_CLASS_PATTERN );
        return excludes;
    }

    private static String fileToClassName( String classFileName )
    {
        return StringUtils.replace( StringUtils.replace( classFileName, ".class", "" ), "/", "." );
    }

    // Check that each class implements java.rmi.Remote, ignore interfaces unless in IIOP mode
    private boolean isRemoteRmiClass( Class remoteClass )
    {
        return java.rmi.Remote.class.isAssignableFrom( remoteClass ) && ( !remoteClass.isInterface() || isIiop() );
    }

    /**
     * Returns a list of URL objects that represent the classpath elements. This is useful for using a URLClassLoader
     *
     * @return list of url classpath elements
     */
    protected List generateUrlCompileClasspath()
            throws MojoExecutionException
    {
        List rmiCompileClasspath = new ArrayList();
        try
        {
            rmiCompileClasspath.add( getClassesDirectory().toURI().toURL() );
            Iterator iter = getRmicClasspathElements().iterator();
            while ( iter.hasNext() )
            {
                URL pathUrl = new File( (String) iter.next() ).toURI().toURL();
                rmiCompileClasspath.add( pathUrl );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new MojoExecutionException( "Problem while generating classpath: " + e.getMessage() );
        }
        return rmiCompileClasspath;
    }

    public String getCompilerId()
    {
        return compilerId;
    }

    public boolean isIiop()
    {
        return iiop;
    }

    public boolean isPoa()
    {
        return poa;
    }

    public boolean isIdl()
    {
        return idl;
    }

    public boolean isKeep()
    {
        return keep;
    }

    public String getVersion()
    {
        return version;
    }

    public boolean isNowarn()
    {
        return nowarn;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public boolean isNoLocalStubs()
    {
        return noLocalStubs;
    }

    public boolean isNoValueMethods()
    {
        return noValueMethods;
    }

    /**
     * An interface for dependencies on the file system and related mojo base classes.
     */
    interface DependenciesFacade
    {
        boolean fileExists( File includeFile );

        SourceInclusionScanner createSourceInclusionScanner( int staleMillis, Set includes, Set excludes );

        Class loadClass( String className ) throws ClassNotFoundException;

        void defineUrlClassLoader( URL[] classpathUrls );
    }

    /**
     * Standard file system and mojo dependencies.
     */
    static class DependenciesFacadeImpl implements DependenciesFacade
    {
        private static URLClassLoader loader;

        public Class loadClass( String className ) throws ClassNotFoundException
        {
            return loader.loadClass( className );
        }

        public void defineUrlClassLoader( URL[] classpathUrls )
        {
            loader = new URLClassLoader( classpathUrls );
        }

        public boolean fileExists( File includeFile )
        {
            return includeFile.exists();
        }

        public SourceInclusionScanner createSourceInclusionScanner( int staleMillis, Set includes, Set excludes )
        {
            return new StaleSourceScanner( staleMillis, includes, excludes );
        }
    }
}
