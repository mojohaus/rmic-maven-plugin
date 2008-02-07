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
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public void execute( RmicConfig rmiConfig )
        throws RmiCompilerException
    {
        // ----------------------------------------------------------------------
        // Construct the RMI Compiler's class path.
        // ----------------------------------------------------------------------

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );
        
        URLClassLoader classLoader = null;
        try 
        {
            URL [] classpathUrls = { toolsJar.toURL() };
            classLoader = new URLClassLoader(classpathUrls, null);
        }
        catch ( MalformedURLException e )
        {
            throw new RmiCompilerException("Unable to resolve tools.jar: " + toolsJar);
        }

        Class clazz = null;

        // ----------------------------------------------------------------------
        // Try to load the rmic class
        // ----------------------------------------------------------------------

        String[] classes = { "sun.rmi.rmic.Main", };

        for ( int i = 0; i < classes.length; i++ )
        {
            String className = classes[i];

            try
            {
                clazz = classLoader.loadClass( className );

                break;
            }
            catch ( ClassNotFoundException e )
            {
                // continue
            }
        }

        if ( clazz == null )
        {
            getLogger().info( "Looked for these classes:" );

            for ( int i = 0; i < classes.length; i++ )
            {
                String className = classes[i];

                getLogger().info( " * " + className );
            }

            getLogger().info( "Within this classpath:" );

            for ( int it = 0; it < classLoader.getURLs().length; it++ )
            {
                URL url = classLoader.getURLs()[it];

                getLogger().info( " * " + url.toExternalForm() );
            }

            throw new RmiCompilerException( "Could not find any of the classes required for executing rmic." );
        }

        // ----------------------------------------------------------------------
        // Build the argument list
        // ----------------------------------------------------------------------

        List arguments = new ArrayList();

        List classpathList = rmiConfig.getRmicClasspathElements();
        if ( classpathList.size() > 0 )
        {
            StringBuffer classpath = new StringBuffer();

            for ( int i = 0; i < classpathList.size(); i++ )
            {
                classpath.append( classpathList.get( i ) + File.pathSeparator );
            }

            arguments.add( "-classpath" );

            arguments.add( classpath.toString() );
        }

        arguments.add( "-d" );

        arguments.add( rmiConfig.getOutputDirectory().getAbsolutePath() );
        
        if ( rmiConfig.getVersion() != null )
        {
            arguments.add( "-v" + rmiConfig.getVersion() );
        }
        
        if ( rmiConfig.isIiop() )
        {
            arguments.add( "-iiop" );
            
            if ( rmiConfig.isNoLocalStubs() )
            {
                arguments.add( "-nolocalstubs" );
            }
        }
        
        if ( rmiConfig.isIdl() )
        {
            arguments.add( "-idl" );
            
            if (rmiConfig.isNoValueMethods())
            {
                arguments.add( "-noValueMethods" );
            }
        }
        
        if ( rmiConfig.isKeep() )
        {
            arguments.add( "-keep" );
        }
        
        if ( getLogger().isDebugEnabled() || rmiConfig.isVerbose() )
        {
            arguments.add( "-verbose" );
        }
        else if ( rmiConfig.isNowarn() )
        {
            arguments.add( "-nowarn" );
        }

        for ( Iterator it = rmiConfig.getRemoteClasses().iterator(); it.hasNext(); )
        {
            String remoteClass = (String) it.next();

            arguments.add( remoteClass );
        }

        String[] args = (String[]) arguments.toArray( new String[arguments.size()] );

        if ( getLogger().isInfoEnabled() )
        {
            getLogger().debug( "rmic arguments: " );

            for ( int i = 0; i < args.length; i++ )
            {
                String arg = args[i];

                getLogger().debug( arg );
            }
        }

        // ----------------------------------------------------------------------
        // Execute it
        // ----------------------------------------------------------------------

        executeMain( clazz, args );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void executeMain( Class clazz, String[] args )
        throws RmiCompilerException
    {
        Method compile;

        Object main;

        try
        {
            Constructor constructor = clazz.getConstructor( new Class[] { OutputStream.class, String.class } );

            main = constructor.newInstance( new Object[] { System.out, "rmic" } );

            compile = clazz.getMethod( "compile", new Class[] { String[].class } );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RmiCompilerException( "Error while initializing rmic.", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RmiCompilerException( "Error while initializing rmic.", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RmiCompilerException( "Error while initializing rmic.", e );
        }
        catch ( InstantiationException e )
        {
            throw new RmiCompilerException( "Error while initializing rmic.", e );
        }

        try
        {
            compile.invoke( main, new Object[] { args } );
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
            throw new RmiCompilerException( "Could not make a URL out of the class path element " + "'" +
                file.toString() + "'." );
        }
    }
}
