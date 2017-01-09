package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2005-2017, Codehaus.org
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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A base class for invocation of rmi compilers whose arguments match those required by the JDK version of rmic.
 */
abstract class AbstractRmiCompiler implements RmiCompiler
{
    private Log logger;

    /* A facade to enable unit testing to control compiler access. */
    private static ClassLoaderFacade classLoaderFacade = new ClassLoaderFacadeImpl();

    public void setLog( Log log )
    {
        logger = log;
    }

    public Log getLog()
    {
        return logger;
    }

    /**
     * Returns the object to use for classloading.
     * @return the appropriate loader facade
     */
    static ClassLoaderFacade getClassLoaderFacade()
    {
        return classLoaderFacade;
    }

    /**
     * Execute the compiler
     *
     * @param rmiConfig The config object
     * @throws RmiCompilerException if there is a problem during compile
     */
    public void execute( RmiCompilerConfiguration rmiConfig )
        throws RmiCompilerException
    {
        // ----------------------------------------------------------------------
        // Build the argument list
        // ----------------------------------------------------------------------

        List<String> arguments = new ArrayList<>();

        List<String> classpathList = rmiConfig.getClasspathEntries();
        if ( classpathList.size() > 0 )
        {
            arguments.add( "-classpath" );
            arguments.add( buildClasspath( classpathList ) );
        }

        arguments.add( "-d" );
        arguments.add( rmiConfig.getOutputLocation() );

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
                throw new RmiCompilerException( "IIOP must be enabled in order to use the POA option" );
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

        for ( File remoteClass : rmiConfig.getSourceFiles() )
        {
            arguments.add( fileToClassName( remoteClass.getPath() ) );
        }

        String[] args = arguments.toArray( new String[arguments.size()] );

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "rmic arguments: " );

            for ( String arg : args )
            {
                getLog().debug( arg );
            }
        }

        try
        {
            compileInProcess( args );
        }
        catch ( CompilerException e )
        {
            throw new RmiCompilerException( e.getMessage(), e );
        }
    }

    private String buildClasspath( List<String> classpathList )
    {
        StringBuilder classpath = new StringBuilder( classpathList.get( 0 ) );
        for ( int i = 1; i < classpathList.size(); i++ )
        {
            classpath.append( File.pathSeparator ).append( classpathList.get( i ) );
        }

        return classpath.toString();
    }

    private static String fileToClassName( String classFileName )
    {
        return StringUtils.replace( StringUtils.replace( classFileName, ".class", "" ), File.separator, "." );
    }

    protected void compileInProcess( String[] args )
        throws CompilerException
    {
        final Class<?> mainClass = createMainClass();
        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader( mainClass.getClassLoader() );
        try
        {
            compileInProcess0( mainClass, args );
        }
        finally
        {
            thread.setContextClassLoader( contextClassLoader );
        }
    }

    protected abstract Class<?> createMainClass()
        throws CompilerException;

    private static void compileInProcess0( Class<?> rmicMainClass, String[] args )
        throws CompilerException
    {
        try
        {
            Constructor<?> constructor = rmicMainClass.getConstructor( OutputStream.class, String.class );

            Object main = constructor.newInstance( System.out, "rmic" );

            Method compile = rmicMainClass.getMethod( "compile", String[].class );

            compile.invoke( main, new Object[] { args } );
        }
        catch ( NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InstantiationException | InvocationTargetException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }
    }

    /**
     * An interface for loading the proper RMI compiler class.
     */
    interface ClassLoaderFacade
    {
        /**
         * Updates the active classloader to include the specified URLs before the original definitions.
         *
         * @param urls a list of URLs to include when searching for classes.
         */
        void prependUrls( URL... urls );

        /**
         * Loads the specified class using the appropriate classloader.
         *
         * @param rmiCompilerClass the name of the class to use for post-processing Java classes for use with Iiop.
         * @throws ClassNotFoundException if the specified class doesn't exist
         * @return the actual compiler class to use
         */
        Class<?> loadClass( String rmiCompilerClass ) throws ClassNotFoundException;
    }

    /**
     * The implementation of ClassLoaderFacade used at runtime.
     */
    private static class ClassLoaderFacadeImpl implements ClassLoaderFacade
    {
        ClassLoader classLoader = getClass().getClassLoader();

        public void prependUrls( URL... urls )
        {
            classLoader = new URLClassLoader( urls, classLoader );
        }

        public Class<?> loadClass( String rmiCompilerClass ) throws ClassNotFoundException
        {
            return classLoader.loadClass( rmiCompilerClass );
        }

    }
}
