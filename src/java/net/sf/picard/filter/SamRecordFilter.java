/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.sf.picard.filter;

import net.sf.samtools.SAMRecord;

/**
 * API for filtering SAMRecords
 *
 * $Id$
 */
public interface SamRecordFilter {

    /**
     * Determines whether a SAMRecord matches this filter
     *
     * @param record the SAMRecord to evaluate
     *
     * @return true if the SAMRecord matches the filter, otherwise false
     */
    public boolean filterOut(SAMRecord record);

    /**
     * Determines whether a pair of SAMRecords matches this filter
     *
     * @param first  the first SAMRecord to evaluate
     * @param second the second SAMRecord to evaluate
     *
     * @return true if the pair of records matches filter, otherwise false
     */
    public boolean filterOut(SAMRecord first, SAMRecord second);
}
