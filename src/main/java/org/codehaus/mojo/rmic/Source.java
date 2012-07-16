package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2012, Codehaus.org
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of class files to be processed by rmic, along with the desired options.
 */
public class Source implements RmicConfig
{

    private static final String INCLUDE_ALL = "**/*";
    private AbstractRmiMojo mojo;

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
     * @parameter
     */
    private Boolean iiop;

    /**
     * Do not create stubs optimized for same process.
     *
     * @parameter
     */
    private Boolean noLocalStubs;

    /**
     * Create IDL.
     *
     * @parameter default-value="false"
     */
    private Boolean idl;

    /**
     * Do not generate methods for valuetypes.
     *
     * @parameter
     */
    private Boolean noValueMethods;

    /**
     * Do not delete intermediate generated source files.
     *
     * @parameter
     */
    private Boolean keep;

    /**
     * Turn off rmic warnings.
     *
     * @parameter
     */
    private Boolean nowarn;

    /**
     * Enable poa generation.
     *
     * @parameter
     */
    private Boolean poa;

    /**
     * Enable verbose output.
     *
     * @parameter
     */
    private Boolean verbose;


    public boolean isIiop()
    {
        return isSetOrDefault( iiop, mojo.isIiop() );
    }

    public boolean isNoLocalStubs()
    {
        return isSetOrDefault( noLocalStubs, mojo.isNoLocalStubs() );
    }

    public boolean isIdl()
    {
        return isSetOrDefault( idl, mojo.isIdl() );
    }

    public boolean isNoValueMethods()
    {
        return isSetOrDefault( noValueMethods, mojo.isNoValueMethods() );
    }

    public boolean isKeep()
    {
        return isSetOrDefault( keep, mojo.isKeep() );
    }

    public boolean isNowarn()
    {
        return isSetOrDefault( nowarn, mojo.isNowarn() );
    }

    public boolean isPoa()
    {
        return isSetOrDefault( poa, mojo.isPoa() );
    }

    public boolean isVerbose()
    {
        return isSetOrDefault( verbose, mojo.isVerbose() );
    }

    public String getVersion()
    {
        return version != null ? version : mojo.getVersion();
    }

    private static boolean isSetOrDefault( Boolean field, boolean defaultValue )
    {
        return field != null ? field.booleanValue() : defaultValue;
    }

    void setRmiMojo( AbstractRmiMojo mojo )
    {
        this.mojo = mojo;
    }

    Set getIncludes()
    {
        return !isEmpty( includes ) ? includes
                : isEmpty( mojo.includes ) ? createOneElementSet( INCLUDE_ALL ) : mojo.includes;
    }

    Set getExcludes()
    {
        return !isEmpty( excludes ) ? excludes
                : isEmpty( mojo.excludes ) ? new HashSet() : mojo.excludes;
    }

    private static HashSet createOneElementSet( Object element )
    {
        return new HashSet( Arrays.asList( new Object[]{element} ) );
    }

    private static boolean isEmpty( Collection collection )
    {
        return collection == null || collection.isEmpty();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "Including " ).append( getIncludes() ).append( "; excluding " ).append( getExcludes() );
        sb.append( "\nwith options: " );
        appendIfTrue( sb, isIiop(), "-iiop" );
        appendIfTrue( sb, isIiop() && isNoLocalStubs(), "-noLocalStubs" );
        appendIfTrue( sb, isIdl(), "-idl" );
        appendIfTrue( sb, isIdl() && isNoValueMethods(), "-noValueMethods" );
        appendIfTrue( sb, isKeep(), "-keep" );
        appendIfTrue( sb, isNowarn(), "-nowarn" );
        appendIfTrue( sb, isPoa(), "-poa" );

        if (getVersion() != null)
        {
            sb.append( "-v" ).append( getVersion() );
        }
        return sb.toString();
    }

    private void appendIfTrue( StringBuffer sb, boolean condition, String option )
    {
        if ( condition )
        {
            sb.append( option ).append( ' ' );
        }
    }
}
