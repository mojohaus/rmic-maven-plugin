package org.apache.maven.plugin.rmic;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SunRmiCompiler
    extends AbstractLogEnabled
    implements RmiCompiler
{
    // ----------------------------------------------------------------------
    // RmiCompiler Implementation
    // ----------------------------------------------------------------------

    public void execute( File[] path, List remoteClasses, File outputClasses )
        throws RmiCompilerException
    {
        String[] classes = {
            "sun.rmi.rmic.newrmic.Main",
            "sun.rmi.rmic.Main",
        };

        List elements = new ArrayList();

        elements.addAll( Arrays.asList( path ) );

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );

        if ( toolsJar.isFile() )
        {
            elements.add( toolsJar );
        }

        URL[] classpath = new URL[ elements.size() ];

        for ( int i = 0; i < elements.size(); i++ )
        {
            classpath[ i ] = fileToURL( (File) elements.get( i ) );
        }

        ClassLoader classLoader = new URLClassLoader( classpath, ClassLoader.getSystemClassLoader() );

        Class clazz = null;

        for ( int i = 0; i < classes.length; i++ )
        {
            String className = classes[ i ];

            clazz = findClass( classLoader, className );

            if ( clazz != null )
            {
                break;
            }
        }

        if ( clazz == null )
        {
            getLogger().info( "Looked for these classes:" );

            for ( int i = 0; i < classes.length; i++ )
            {
                String className = classes[ i ];

                getLogger().info( " * " + className );
            }

            getLogger().info( "With this classpath:" );

            for ( int it = 0; it < classpath.length; it++ )
            {
                URL url = classpath[ it ];

                getLogger().info( " * " + url.toExternalForm() );
            }

            throw new RmiCompilerException( "Could not find any of the classes required for executing rmic." );
        }

        String c = "";

        for ( int i = 0; i < path.length; i++ )
        {
            File file = path[ i ];

            c += file.getAbsolutePath() + ":";
        }

        List arguments = new ArrayList();

        arguments.add( "-classpath" );

        arguments.add( c );

        arguments.add( "-d" );

        arguments.add( outputClasses.getAbsolutePath() );

        if ( getLogger().isDebugEnabled() )
        {
            arguments.add( "-verbose" );
        }

        for ( Iterator it = remoteClasses.iterator(); it.hasNext(); )
        {
            String remoteClass = (String) it.next();

            arguments.add( remoteClass );
        }

        String[] args = (String[]) arguments.toArray( new String[ arguments.size() ] );

        getLogger().info( "rmic arguments: " );

        for ( int i = 0; i < args.length; i++ )
        {
            String arg = args[ i ];

            getLogger().info( arg );
        }

        executeMain( clazz, args );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void executeMain( Class clazz, String[] args )
        throws RmiCompilerException
    {
        Method method;

        try
        {
            method = clazz.getMethod( "main", new Class[] { String[].class } );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RmiCompilerException( "Internal error, could not find the main() " +
                                            "in the class " + clazz.getName() + "." );
        }

        try
        {
            method.invoke( null, new Object[] { args } );
        }
        catch ( IllegalAccessException e )
        {
            throw new RmiCompilerException( "Error while executing rmic.", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RmiCompilerException( "Error while executing rmic.", e );
        }
    }

    public static URL fileToURL( File file )
        throws RmiCompilerException
    {
        try
        {
            return file.toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new RmiCompilerException( "Could not make a URL out of the class path element " +
                                            "'" + file.toString() + "'." );
        }
    }

    private Class findClass( ClassLoader classLoader, String className )
    {
        try
        {
            return classLoader.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }
}
