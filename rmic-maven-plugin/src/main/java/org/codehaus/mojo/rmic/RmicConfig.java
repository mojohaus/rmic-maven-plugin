package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2008-2012, Codehaus.org
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

/**
 * Interface containing required methods for retrieving information
 * necessary for rmi compilation to take place.
 *
 * @author pgier
 * @version $Id$
 */
public interface RmicConfig
{
    /**
     * @return The version of the compiler to use
     */
    String getVersion();

    /**
     * @return Whether iiop stubs should be generated
     */
    boolean isIiop();

    /**
     * @return Whether skeletons should be poa-compatible should be generated
     */
    boolean isPoa();

    /**
     * @return Create IDL
     */
    boolean isIdl();

    /**
     * Keep intermediate files
     *
     * @return true or false
     */
    boolean isKeep();

    /**
     * Turn off warnings
     *
     * @return true or false
     */
    boolean isNowarn();

    /**
     * Output messages about compilation
     *
     * @return true or false
     */
    boolean isVerbose();

    /**
     * Do not create stubs optimized for same process.
     *
     * @return true or false
     */
    boolean isNoLocalStubs();

    /**
     * Do not create methods for valuetypes.
     *
     * @return true or false
     */
    boolean isNoValueMethods();
}
