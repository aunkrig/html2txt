
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
import java.io.PrintWriter;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.text.xml.XmlUtil;
import de.unkrig.html2txt.Html2Txt.HtmlException;

/**
 * A command line interface for {@link Html2Txt}.
 */
public final
class Main {

    private Main() {}

    /**
     * Usage:
     * <dl>
     *   <dt>{@code html2txt} <var>input-file</var></dt>
     *   <dd>
     *     Converts the HTML document in the <var>input-file</var> to plain text, and writes it to STDOUT.
     *   </dd>
     *   <dt>{@code html2txt} <var>input-file</var> <var>output-file</var></dt>
     *   <dd>
     *     Converts the HTML document in the <var>input-file</var> to plain text, and writes it to the
     *     <var>output-file</var>.
     *   </dd>
     *   <dt>{@code html2txt -help}</dt>
     *   <dd>
     *     Prints this text.
     *   </dd>
     * </dl>
     */
    public static void
    main(String[] args) throws Exception {

        if (args.length == 1 && "-help".equals(args[0])) {
            IoUtil.copy(Main.class.getClassLoader().getResourceAsStream("de/unkrig/html2txt/usage.txt"), System.out);
            return;
        }

        try {
            switch (args.length)  {

            case 1:
                {
                    File inputFile = new File(args[0]);
                    new Html2Txt().html2txt(inputFile, new PrintWriter(System.out));
                }
                break;

            case 2:
                {
                    File inputFile  = new File(args[0]);
                    File outputFile = new File(args[1]);
                    new Html2Txt().html2txt(inputFile, outputFile);
                }
                break;

            default:
                System.err.println("Invalid number of command line arguments; try \"-help\".");
                System.exit(1);
            }
        } catch (SAXParseException e) {

            System.err.println(
                "Line "
                + e.getLineNumber()
                + ", column "
                + e.getColumnNumber()
                + ": "
                + e.getMessage()
            );
            System.exit(1);
        } catch (TransformerException e) {

            SourceLocator locator = e.getLocator();
            if (locator == null) {
                System.err.println(e.getMessage());
            } else {
                System.err.println(
                    "Line "
                    + locator.getLineNumber()
                    + ", column "
                    + locator.getColumnNumber()
                    + ": "
                    + e.getMessage()
                );
            }
            System.exit(1);
        } catch (HtmlException e) {

            Locator l = XmlUtil.getLocation(e.getNode());
            if (l == null) {
                System.err.println(e.getMessage());
            } else {
                System.err.println(
                    "Line "
                    + l.getLineNumber()
                    + ", column "
                    + l.getColumnNumber()
                    + ": "
                    + e.getMessage()
                );
            }
            System.exit(1);
        }
    }
}
