
/*
 * html2txt - Converts HTML documents to plain text
 *
 * Copyright (c) 2015, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.html2txt;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXParseException;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.html2txt.Html2Txt.HtmlException;

/**
 * A command line interface for {@link Html2Txt}.
 */
public final
class Main {

    private Main() {}

    /**
     * <h2>Usage:</h2>
     *
     * <dl>
     *   <dt>{@code html2txt} [ <var>option</var> ] ... <var>input-file</var></dt>
     *   <dd>
     *     Converts the HTML document in the <var>input-file</var> to plain text, and writes it to STDOUT.
     *   </dd>
     *   <dt>{@code html2txt} [ <var>option</var> ] ... <var>input-file</var> <var>output-file</var></dt>
     *   <dd>
     *     Converts the HTML document in the <var>input-file</var> to plain text, and writes it to the
     *     <var>output-file</var>.
     *   </dd>
     * </dl>
     *
     * <h2>Options:</h2>
     *
     * <dl>
     *   <dt>{@code -help}</dt>
     *   <dd>
     *     Print this text and terminate.
     *   </dd>
     *   <dt>{@code -page-width} <var>N</var></dt>
     *   <dd>
     *     The maximum line length to produce. Defaults to the value of the "{@code $COLUMNS}" environment variable,
     *     if set, otherwise to "80".
     *   </dd>
     * </dl>
     */
    public static void
    main(String[] args) throws Exception {

        Html2Txt html2Txt = new Html2Txt();

        int idx = 0;
        while (idx < args.length) {
            String arg = args[idx];
            if (!arg.startsWith("-")) break;
            idx++;
            if ("-help".equals(arg)) {
                InputStream is = Main.class.getClassLoader().getResourceAsStream("de/unkrig/html2txt/usage.txt");
                IoUtil.copy(
                    is,         // inputStream
                    true,       // closeInputStream
                    System.out, // outputStream
                    false       // closeOutputStream
                );
                return;
            } else
            if ("-page-width".equals(arg)) {
                html2Txt.setPageWidth(Integer.parseInt(args[idx++]));
            } else
            {
                System.err.println("Invalid command line option \"" + arg + "\"; try \"-help\".");
                System.exit(1);
                return;
            }
        }

        try {
            switch (args.length - idx)  {

            case 1:
                {
                    File inputFile = new File(args[idx++]);
                    html2Txt.html2txt(inputFile, new PrintWriter(System.out));
                }
                break;

            case 2:
                {
                    File inputFile  = new File(args[idx++]);
                    File outputFile = new File(args[idx++]);
                    html2Txt.html2txt(inputFile, outputFile);
                }
                break;

            default:
                System.err.println("Invalid number of command line arguments; try \"-help\".");
                System.exit(1);
            }
        } catch (SAXParseException spe) {

            String publicId = spe.getPublicId();
            System.err.println(
                (publicId != null ? publicId + ", line " : "Line ")
                + spe.getLineNumber()
                + ", column "
                + spe.getColumnNumber()
                + ": "
                + spe.getMessage()
                + '.'
            );
            System.exit(1);
        } catch (TransformerException te) {

            SourceLocator l = te.getLocator();
            if (l == null) {
                System.err.println(te.getMessage());
            } else {
                String publicId = l.getPublicId(); // TODO: Do we get the input file path here?
                System.err.println(
                    (publicId != null ? publicId + ", line " : "Line ")
                    + ", line "
                    + l.getLineNumber()
                    + ", column "
                    + l.getColumnNumber()
                    + ": "
                    + te.getMessage()
                    + '.'
                );
            }
            System.exit(1);
        } catch (HtmlException he) {
            System.err.println(he);
            System.exit(1);
        }
    }
}
