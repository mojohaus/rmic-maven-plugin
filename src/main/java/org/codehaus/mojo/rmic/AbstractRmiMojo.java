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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
{
    private static final String STUB_CLASS_PATTERN = "**/*_Stub.class";
    // ----------------------------------------------------------------------
    // Configurable parameters
    // ----------------------------------------------------------------------

    private Source source;
    
    /**
     * A <code>List</code> of <code>Source</code> configurations to compile.
     *
     * @parameter
     */
    private List<Source> sources;

    /**
     * A list of inclusions when searching for classes to compile.
     *
     * @parameter
     */
    protected Set<String> includes;

    /**
     * A list of exclusions when searching for classes to compile.
     *
     * @parameter
     */
    protected Set<String> excludes;

    /**
     * The id of the rmi compiler to use.
     *
     * @parameter default-value="sun"
     */
    protected String compilerId;

    // @todo change this to a Map<String, RmiCompiler>, so you can choose one by settings compilerId
    private RmiCompiler rmiCompiler = new SunRmiCompiler();

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
    List<Source> getSources()
    {
        return sources;
    }

    /**
     * Get the list of elements to add to the classpath of rmic
     *
     * @return list of classpath elements
     */
    public abstract List<String> getProjectClasspathElements();

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
        if( source != null && sources != null)
        {
            throw new MojoExecutionException( "Either use sources or only the sourceflags, don't combine them." );
        }
        if ( sources == null || sources.isEmpty() )
        {
            sources = Collections.singletonList( getSource() );
        }

        for ( Source source : sources )
        {
            doExecute( source );
        }
    }

    private void doExecute( Source source ) throws MojoExecutionException
    {
        rmiCompiler.setLog( getLog() );

        if ( source.isVerbose() )
        {
            getLog().debug( source.toString() );
        }


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
            Set<File> remoteClassesToCompile = getRemoteClasses( source );

            if ( remoteClassesToCompile.size() == 0 )
            {
                getLog().info( "No out of date rmi classes to process." );
                return;
            }

            getLog().info( "Compiling " + remoteClassesToCompile.size() + " remote classes" );
            
            RmiCompilerConfiguration config = new RmiCompilerConfiguration();
            config.setClasspathEntries( getRmicClasspathElements() );
            config.addSourceLocation( getClassesDirectory().getPath() );
            config.setSourceFiles( remoteClassesToCompile );
            config.setIdl( source.isIdl() );
            config.setIiop( source.isIiop() );
            config.setKeep( source.isKeep() );
            config.setNoLocalStubs( source.isNoLocalStubs() );
            config.setNoValueMethods( source.isNoValueMethods() );
            config.setNowarn( source.isNowarn() );
            config.setOutputLocation( getOutputDirectory().getAbsolutePath() );
            config.setPoa( source.isPoa() );
            config.setVerbose( source.isVerbose() );
            config.setVersion( source.getVersion() );
            
            rmiCompiler.execute( config );
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
    public List<String> getRmicClasspathElements()
    {
        List<String> classpathElements = getProjectClasspathElements();

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
    public Set<File> getRemoteClasses( Source source )
    {
        Set<File> remoteClasses = new HashSet<File>();

        try
        {
            // Set up the classloader
            List<URL> classpathList = generateUrlCompileClasspath();
            URL[] classpathUrls = new URL[classpathList.size()];
            classpathUrls[0] = getClassesDirectory().toURI().toURL();
            classpathUrls = (URL[]) classpathList.toArray( classpathUrls );
            dependencies.defineUrlClassLoader( classpathUrls );

            // Scan for remote classes

            SourceInclusionScanner scanner = createScanner( source.getIncludes(), getExcludes( source ) );
            scanner.addSourceMapping( new SuffixMapping( ".class", "_Stub.class" ) );
            
            Collection<File> staleRemoteClasses = scanner.getIncludedSources( getClassesDirectory(), getOutputDirectory() );

            for ( File file : staleRemoteClasses )
            {
                URI relativeURI = getClassesDirectory().toURI().relativize( file.toURI() );
                String className = fileToClassName( relativeURI.toString() );
                Class<?> candidateClass = dependencies.loadClass( className );
                if ( isRemoteRmiClass( candidateClass, source.isIiop() ) )
                {
                    // file is absolute, we need relative files
                    remoteClasses.add( new File( relativeURI.toString() ) );
                }
            }

            // Check for classes in a classpath jar
            for ( String include : source.getIncludes() )
            {
                File includeFile = new File( getClassesDirectory(), include );
                if ( ( include.indexOf( "*" ) != -1 ) || dependencies.fileExists( includeFile ) )
                {
                    continue;
                }
                // We have found a class that is not in the classes dir.
                remoteClasses.add( includeFile );
            }
        }
        catch ( Exception e )
        {
            getLog().warn( "Problem while scanning for classes: " + e );
        }
        return remoteClasses;
    }

    private SourceInclusionScanner createScanner( Set<String> includes, Set<String> excludes )
    {
        return dependencies.createSourceInclusionScanner( staleMillis, includes, excludes );
    }

    private Set<String> getExcludes( Source source )
    {
        Set<String> excludes = source.getExcludes();
        excludes.add( STUB_CLASS_PATTERN );
        return excludes;
    }

    private static String fileToClassName( String classFileName )
    {
        return StringUtils.replace( StringUtils.replace( classFileName, ".class", "" ), "/", "." );
    }

    // Check that each class implements java.rmi.Remote, ignore interfaces unless in IIOP mode
    private boolean isRemoteRmiClass( Class<?> remoteClass, boolean isIiop )
    {
        return java.rmi.Remote.class.isAssignableFrom( remoteClass ) && ( !remoteClass.isInterface() || isIiop );
    }

    /**
     * Returns a list of URL objects that represent the classpath elements. This is useful for using a URLClassLoader
     *
     * @return list of url classpath elements
     */
    protected List<URL> generateUrlCompileClasspath()
            throws MojoExecutionException
    {
        List<URL> rmiCompileClasspath = new ArrayList<URL>();
        try
        {
            rmiCompileClasspath.add( getClassesDirectory().toURI().toURL() );
            for ( String classpathElement : getRmicClasspathElements() )
            {
                URL pathUrl = new File( classpathElement ).toURI().toURL();
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

    public Source getSource()
    {
        if( source == null )
        {
            source = new Source();
        }
        return source;
    }
    
    // Plexus trick: if there's a setter for a @parameter, use the setter
    public void setIncludes( Set<String> includes )
    {
        getSource().setIncludes( includes );
    }

    public void setExcludes( Set<String> excludes )
    {
        getSource().setExcludes( excludes );
    }

    public void setVersion( String version )
    {
        getSource().setVersion( version );
    }

    public void setIiop( Boolean iiop )
    {
        getSource().setIiop( iiop );
    }

    public void setNoLocalStubs( Boolean noLocalStubs )
    {
        getSource().setNoLocalStubs( noLocalStubs );
    }

    public void setIdl( Boolean idl )
    {
        getSource().setIdl( idl );
    }

    public void setNoValueMethods( Boolean noValueMethods )
    {
        getSource().setNoValueMethods( noValueMethods );
    }

    public void setKeep( Boolean keep )
    {
        getSource().setKeep( keep );
    }

    public void setNowarn( Boolean nowarn )
    {
        getSource().setNowarn( nowarn );
    }

    public void setPoa( Boolean poa )
    {
        getSource().setPoa( poa );
    }

    public void setVerbose( Boolean verbose )
    {
        getSource().setVerbose( verbose );
    }
    
    
    /**
     * An interface for dependencies on the file system and related mojo base classes.
     */
    interface DependenciesFacade
    {
        boolean fileExists( File includeFile );

        SourceInclusionScanner createSourceInclusionScanner( int staleMillis, Set<String> includes, Set<String> excludes );

        Class<?> loadClass( String className ) throws ClassNotFoundException;

        void defineUrlClassLoader( URL[] classpathUrls );
    }

    /**
     * Standard file system and mojo dependencies.
     */
    static class DependenciesFacadeImpl implements DependenciesFacade
    {
        private static URLClassLoader loader;

        public Class<?> loadClass( String className ) throws ClassNotFoundException
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

        public SourceInclusionScanner createSourceInclusionScanner( int staleMillis, Set<String> includes, Set<String> excludes )
        {
            return new StaleSourceScanner( staleMillis, includes, excludes );
        }
    }
}
