package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2004, Codehaus.org
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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.StringUtils;

/**
 * Compiles rmi stubs and skeleton classes from a remote implementation class.
 *
 * @goal rmic
 * @phase process-classes
 * @requiresDependencyResolution
 * @description Enhances the application data objects.
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class RmicMojo
    extends AbstractRmiMojo
    implements RmicConfig
{
    // ----------------------------------------------------------------------
    // Configurable parameters
    // ----------------------------------------------------------------------

    /**
     * Time in milliseconds between automatic recompilations.  A value of 0 means that
     * up to date rmic output classes will not be recompiled until the source classes change.
     *
     * @parameter default-value=0
     */
    private int staleMillis;

    // ----------------------------------------------------------------------
    // Constant parameters
    // ----------------------------------------------------------------------

    /**
     * @component org.apache.maven.plugin.rmic.RmiCompilerManager
     */
    private RmiCompilerManager rmiCompilerManager;

    /**
     * List of remote classes to compile.
     */
    private List remoteClasses = new ArrayList();

    /**
     * List of remote classes to compile.
     */
    private List rmicClasspathElements = new ArrayList();

    public List getRemoteClasses()
    {
        return remoteClasses;
    }

    public List getRmicClasspathElements()
    {
        return rmicClasspathElements;
    }

    /**
     * Main mojo execution.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( this.includes == null )
        {
            this.includes = Collections.singleton( "**/*" );
        }

        if ( this.excludes == null )
        {
            this.excludes = Collections.EMPTY_SET;
        }

        RmiCompiler rmiCompiler;

        try
        {
            rmiCompiler = rmiCompilerManager.getRmiCompiler( compilerId );
        }
        catch ( NoSuchRmiCompilerException e )
        {
            throw new MojoExecutionException( "No such RMI compiler installed '" + compilerId + "'." );
        }

        if ( !getOutputDirectory().isDirectory() )
        {
            if ( !getOutputDirectory().mkdirs() )
            {
                throw new MojoExecutionException( "Could not make output directory: " + "'" +
                    getOutputDirectory().getAbsolutePath() + "'." );
            }
        }

        try
        {
            // Initialize the rmic classpath
            rmicClasspathElements.add( this.getClassesDirectory().getAbsolutePath() );
            rmicClasspathElements.addAll( projectCompileClasspathElements );

            // Get the list of classes to compile
            this.remoteClasses = this.scanForRemoteClasses();

            if ( remoteClasses.size() == 0 )
            {
                getLog().info( "No out of date rmi classes to process." );
                return;
            }

            getLog().info( "Compiling " + remoteClasses.size() + " remote classes" );
            rmiCompiler.execute( this );
        }
        catch ( RmiCompilerException e )
        {
            throw new MojoExecutionException( "Error while executing the RMI compiler.", e );
        }
    }

    public List scanForRemoteClasses()
    {
        List remoteClasses = new ArrayList();

        try
        {
            // Set up the classloader
            List classpathList = generateUrlCompileClasspath();
            URL[] classpathUrls = new URL[classpathList.size()];
            classpathUrls[0] = getClassesDirectory().toURL();
            classpathUrls = (URL[]) classpathList.toArray( classpathUrls );
            URLClassLoader loader = new URLClassLoader( classpathUrls );

            // Scan for remote classes
            SourceInclusionScanner scanner = new StaleSourceScanner( staleMillis, this.includes, this.excludes );
            scanner.addSourceMapping( new SuffixMapping( ".class", "_Stub.class" ) );
            Collection staleRemoteClasses = scanner.getIncludedSources( getClassesDirectory(), getOutputDirectory() );

            for ( Iterator iter = staleRemoteClasses.iterator(); iter.hasNext(); )
            {
                // Get the classname and load the class
                File remoteClassFile = (File) iter.next();
                URI relativeURI = getClassesDirectory().toURI().relativize( remoteClassFile.toURI() );
                String className = StringUtils.replace(StringUtils.replace(relativeURI.toString(), ".class", "" ),
                                                       "/", "." );
                Class remoteClass = loader.loadClass( className );

                // Check that each class implement java.rmi.Remote
                if ( java.rmi.Remote.class.isAssignableFrom( remoteClass ) && ( !remoteClass.isInterface() ) )
                {
                    remoteClasses.add( className );
                }
            }
            
            // Check for classes in a classpath jar
            for ( Iterator iter = includes.iterator(); iter.hasNext(); )
            {
                String include = (String) iter.next();
                File includeFile = new File(getClassesDirectory(), include);
                if ( include.contains( "*" ) || includeFile.exists() )
                {
                    continue;
                }
                // We have found a class that is not in the classes dir.
                remoteClasses.add( include.replace( ".class", "").replace( "/", "." ) );
            }
        }
        catch ( Exception e )
        {
            getLog().warn( "Problem while scanning for classes: " + e );
            getLog().debug( e );
        }
        return remoteClasses;
    }

    /**
     * Returns a list of URL objects that represent the classpath elements.
     *
     * @return
     */
    protected List generateUrlCompileClasspath()
        throws MojoExecutionException
    {
        List rmiCompileClasspath = new ArrayList();
        try
        {
            rmiCompileClasspath.add( getClassesDirectory().toURL() );
            Iterator iter = projectCompileClasspathElements.iterator();
            while ( iter.hasNext() )
            {
                URL pathUrl = new File( (String) iter.next() ).toURL();
                rmiCompileClasspath.add( pathUrl );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Problem while generating classpath" );
        }
        return rmiCompileClasspath;
    }
}
