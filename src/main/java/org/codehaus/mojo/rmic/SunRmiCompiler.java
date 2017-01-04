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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
class SunRmiCompiler
    implements RmiCompiler
{
    private static final String EOL = System.getProperty( "line.separator" );

    /**
     * The name of the class to use for rmi compilation.
     */
    private static final String RMIC_CLASSNAME = "sun.rmi.rmic.Main";

    private Log logger;

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
            compileInProcess( args, null );
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

    public void setLog( Log log )
    {
        logger = log;
    }

    public Log getLog()
    {
        return logger;
    }

    protected void compileInProcess( String[] args, CompilerConfiguration config )
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
            // releaseMainClass( mainClass, config );
            thread.setContextClassLoader( contextClassLoader );
        }
    }

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
        catch ( NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }
    }

    private Class<?> createMainClass()
        throws CompilerException
    {
        try
        {
            // look whether Main is on Maven's classpath
            return SunRmiCompiler.class.getClassLoader().loadClass( SunRmiCompiler.RMIC_CLASSNAME );
        }
        catch ( ClassNotFoundException ex )
        {
            // ok
        }

        final File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );
        if ( !toolsJar.exists() )
        {
            throw new CompilerException( "tools.jar not found: " + toolsJar );
        }

        try
        {
            final ClassLoader javacClassLoader =
                new URLClassLoader( new URL[] { toolsJar.toURI().toURL() }, SunRmiCompiler.class.getClassLoader() );

            final Thread thread = Thread.currentThread();
            final ClassLoader contextClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader( javacClassLoader );
            try
            {
                return javacClassLoader.loadClass( SunRmiCompiler.RMIC_CLASSNAME );
            }
            finally
            {
                thread.setContextClassLoader( contextClassLoader );
            }
        }
        catch ( MalformedURLException ex )
        {
            throw new CompilerException(
                                         "Could not convert the file reference to tools.jar to a URL, path to tools.jar: '"
                                             + toolsJar.getAbsolutePath() + "'.", ex );
        }
        catch ( ClassNotFoundException ex )
        {
            throw new CompilerException( "Unable to locate the Rmi Compiler in:" + EOL + "  " + toolsJar + EOL
                + "Please ensure you are using JDK 1.4 or above and" + EOL + "not a JRE (the " + RMIC_CLASSNAME
                + " class is required)." + EOL + "In most cases you can change the location of your Java" + EOL
                + "installation by setting the JAVA_HOME environment variable.", ex );
        }
    }
    
    private static String fileToClassName( String classFileName )
    {
        return StringUtils.replace( StringUtils.replace( classFileName, ".class", "" ), File.separator, "." );
    }
}
