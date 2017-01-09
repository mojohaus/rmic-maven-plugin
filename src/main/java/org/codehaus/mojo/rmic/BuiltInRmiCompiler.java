package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2004-2017, Mojohaus.org
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
import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.plexus.compiler.CompilerException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
class BuiltInRmiCompiler extends AbstractRmiCompiler
{
    private static final String EOL = System.getProperty( "line.separator" );

    /**
     * The name of the class to use for rmi compilation.
     */
    private static final String RMIC_CLASSNAME = "sun.rmi.rmic.Main";

    private static final String USE_GLASSFISH_RMIC =
                                    " Built-in RMIC compiler not available in JDK9." +
                                    " Add a dependency on org.glassfish.corba:rmic to the plugin.";

    @Override
    protected Class<?> createMainClass()
        throws CompilerException
    {
        try
        {
            return getClassLoaderFacade().loadClass( BuiltInRmiCompiler.RMIC_CLASSNAME );
        }
        catch ( ClassNotFoundException ignored )
        {
        }

        try
        {
            addToolsJarToPath();
            return getClassLoaderFacade().loadClass( BuiltInRmiCompiler.RMIC_CLASSNAME );
        }
        catch ( Exception ex )
        {
            throw new CompilerException( getSecondTryMessage( ex ) );
        }
    }

    private static String getSecondTryMessage( Exception e ) throws CompilerException
    {
        return builtInCompilerHidden( e ) ? USE_GLASSFISH_RMIC : getRmicCompilerNotAvailableMessage();
    }

    private static boolean builtInCompilerHidden( Exception e )
    {
        return compilerNotFound( e ) && isJigsawPresent();
    }

    private static boolean compilerNotFound( Exception e )
    {
        return e instanceof ClassNotFoundException;
    }

    private static boolean isJigsawPresent()
    {
        return !System.getProperty( "java.version" ).startsWith( "1." );
    }

    private static String getRmicCompilerNotAvailableMessage() throws CompilerException
    {
        return "Unable to locate the Rmi Compiler in:" + EOL + "  " + getToolsJarUrl() + EOL
            + "Please ensure you are using JDK 1.4 or above and" + EOL + "not a JRE (the " + RMIC_CLASSNAME
            + " class is required)." + EOL + "In most cases you can change the location of your Java" + EOL
            + "installation by setting the JAVA_HOME environment variable.";
    }


    private static void addToolsJarToPath() throws MalformedURLException, ClassNotFoundException, CompilerException
    {
        URL toolsJarUrl = getToolsJarUrl();
        getClassLoaderFacade().prependUrls( toolsJarUrl );
    }

    private static URL getToolsJarUrl() throws CompilerException
    {
        File javaHome = new File( System.getProperty( "java.home" ) );
        File toolsJar = new File( javaHome, "../lib/tools.jar" );
        try
        {
            return toolsJar.toURI().toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new CompilerException( "Could not convert the file reference to tools.jar to a URL, path to tools.jar: '"
                                             + toolsJar.getAbsolutePath() + "'.", e );
        }
    }
}
