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
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SunRmiCompiler implements RmiCompiler
{
    /**
     * The name of the class to use for rmi compilation.
     */
    public static final String RMIC_CLASSNAME = "sun.rmi.rmic.Main";

    /* A facade to enable unit testing to control classloading. */
    private static ClassLoaderFacade classLoaderFacade = new ClassLoaderFacadeImpl();
    private Log logger;

    /**
     * Specifies the implementation of the classloader facade to use
     *
     * @param classLoaderFacade a wrapper for class loading.
     */
    static void setClassLoaderFacade( ClassLoaderFacade classLoaderFacade )
    {
        SunRmiCompiler.classLoaderFacade = classLoaderFacade;
    }

    /**
     * Execute the compiler
     *
     * @param mojo
     * @param rmiConfig        The config object
     * @param classesToCompile The list of classes to rmi compile
     * @throws RmiCompilerException if there is a problem during compile
     */
    public void execute( AbstractRmiMojo mojo, RmicConfig rmiConfig, List classesToCompile )
            throws RmiCompilerException
    {
        // ----------------------------------------------------------------------
        // Construct the RMI Compiler's class path.
        // ----------------------------------------------------------------------

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );

        try
        {
            URL[] classpathUrls = {toolsJar.toURI().toURL()};
            classLoaderFacade.prependUrls( classpathUrls );
        }
        catch ( MalformedURLException e )
        {
            throw new RmiCompilerException( "Unable to resolve tools.jar: " + toolsJar );
        }


        // ----------------------------------------------------------------------
        // Try to load the rmic class
        // ----------------------------------------------------------------------

        Class rmicMainClass = loadRmicClass();

        // ----------------------------------------------------------------------
        // Build the argument list
        // ----------------------------------------------------------------------

        List arguments = new ArrayList();

        List classpathList = mojo.getRmicClasspathElements();
        if ( classpathList.size() > 0 )
        {
            arguments.add( "-classpath" );
            arguments.add( buildClasspath( classpathList ) );
        }

        arguments.add( "-d" );
        arguments.add( mojo.getOutputDirectory().getAbsolutePath() );

        if ( rmiConfig.getVersion() != null )
        {
            arguments.add( "-v" + rmiConfig.getVersion() );
        }

        if ( rmiConfig.isIiop() )
        {
            arguments.add( "-iiop" );

            if ( rmiConfig.isPoa() )
            {
                arguments.add( "-poa" );
            }
            if ( rmiConfig.isNoLocalStubs() )
            {
                arguments.add( "-nolocalstubs" );
            }
        }
        else
        {
            if ( rmiConfig.isPoa() )
            {
                throw new RmiCompilerException( "IIOP must be enabled in order to use the POA option");
            }

        }

        if ( rmiConfig.isIdl() )
        {
            arguments.add( "-idl" );
            if ( rmiConfig.isNoValueMethods() )
            {
                arguments.add( "-noValueMethods" );
            }
        }

        if ( rmiConfig.isKeep() )
        {
            arguments.add( "-keep" );
        }

        if ( getLog().isDebugEnabled() || rmiConfig.isVerbose() )
        {
            arguments.add( "-verbose" );
        }
        else if ( rmiConfig.isNowarn() )
        {
            arguments.add( "-nowarn" );
        }

        for ( Iterator it = classesToCompile.iterator(); it.hasNext(); )
        {
            String remoteClass = (String) it.next();

            arguments.add( remoteClass );
        }

        String[] args = (String[]) arguments.toArray( new String[arguments.size()] );

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "rmic arguments: " );

            for ( int i = 0; i < args.length; i++ )
            {
                getLog().debug( args[i] );
            }
        }

        executeMain( rmicMainClass, args );
    }

    private String buildClasspath( List classpathList )
    {
        StringBuffer classpath = new StringBuffer( classpathList.get(0).toString() );
        for ( int i = 1; i < classpathList.size(); i++ )
        {
            classpath.append( File.pathSeparator ).append( classpathList.get( i ) );
        }

        return classpath.toString();
    }

    public void setLog( Log log )
    {
        logger = log;
    }

    public Log getLog()
    {
        return logger;
    }

    private Class loadRmicClass() throws RmiCompilerException
    {
        try
        {
            return classLoaderFacade.loadClass( RMIC_CLASSNAME );
        }
        catch ( ClassNotFoundException e )
        {
            getLog().warn( "Could not find rmi compiler: " + RMIC_CLASSNAME );
            getLog().info( "Within this classpath:" );
            logClasspath( getLog(), classLoaderFacade.getUrls() );
            throw new RmiCompilerException( "Could not find " + RMIC_CLASSNAME + " on the classpath." );
        }
    }

    private void logClasspath( Log logger, URL[] urls )
    {
        for ( int it = 0; it < urls.length; ++it )
        {
            logger.info( " * " + urls[it].toExternalForm() );
        }
    }

    /**
     * @param rmicMainClass The class to use to run the rmic
     * @param args          Arguments to be passed to rmic
     * @throws RmiCompilerException If there is a problem during the compile
     */
    private void executeMain( Class rmicMainClass, String[] args )
            throws RmiCompilerException
    {
        try
        {
            Constructor constructor = rmicMainClass.getConstructor( new Class[]{OutputStream.class, String.class} );
            Object main = constructor.newInstance( new Object[]{System.out, "rmic"} );
            Method compile = rmicMainClass.getMethod( "compile", new Class[]{String[].class} );
            compile.invoke( main, new Object[]{args} );
        }
        catch ( Exception e )
        {
            throw new RmiCompilerException( "Error while executing rmic.", e );
        }
    }

    /**
     * An interface for dependencies on classloading - enables unit testing.
     */
    static interface ClassLoaderFacade
    {
        Class loadClass( String classname ) throws ClassNotFoundException;

        void prependUrls( URL[] classpathUrls );

        URL[] getUrls();
    }

    /**
     * The production implementation of the classloader dependencies.
     */
    static class ClassLoaderFacadeImpl implements ClassLoaderFacade
    {
        private URLClassLoader classLoader = null;

        public Class loadClass( String classname ) throws ClassNotFoundException
        {
            return classLoader.loadClass( classname );
        }

        public void prependUrls( URL[] classpathUrls )
        {
            classLoader = new URLClassLoader( classpathUrls, null );
        }

        public URL[] getUrls()
        {
            return classLoader.getURLs();
        }
    }
}
