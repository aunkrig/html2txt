
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
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.html2txt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXParseException;

import de.unkrig.commons.util.CommandLineOptions;
import de.unkrig.commons.util.annotation.CommandLineOption;
import de.unkrig.html2txt.Html2Txt.HtmlException;

/**
 * A command line interface for {@link Html2Txt}.
 */
public final
class Main {

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
     * {@main.commandLineOptions}
     * </dl>
     */
    public static void
    main(String[] args) throws Exception {

        Main main = new Main();

        args = CommandLineOptions.parse(args, main);


        try {
            switch (args.length)  {

            case 1:
                main.html2Txt.html2txt(new File(args[0]), new PrintWriter(System.out));
                break;

            case 2:
                main.html2Txt.html2txt(new File(args[0]), new File(args[1]));
                break;

            default:
                System.err.println("Invalid number of command line arguments; try \"--help\".");
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

    private final Html2Txt html2Txt = new Html2Txt();

    private
    Main() {}

    /**
     * Print this text and terminate.
     */
    @CommandLineOption public static void
    help() throws IOException {
        CommandLineOptions.printResource(Main.class, "main(String[]).txt", Charset.forName("UTF-8"), System.out);
        System.exit(0);
    }

    /**
     * The maximum line length to produce. Defaults to the value of the "{@code $COLUMNS}" environment variable, if
     * set, otherwise to "80".
     */
    @CommandLineOption public void
    setPageWidth(int width) { this.html2Txt.setPageWidth(width); }

    /**
     * The charset to use when reading the input file and writing the output file.
     */
    @CommandLineOption public void
    setEncoding(Charset charset) {
        this.html2Txt.setInputCharset(charset);
        this.html2Txt.setOutputCharset(charset);
    }

    /**
     * The charset to use when reading the input file.
     */
    @CommandLineOption public void
    setInputEncoding(Charset charset) {
        this.html2Txt.setInputCharset(charset);
    }

    /**
     * The charset to use when writing the output file.
     */
    @CommandLineOption public void
    setOutputEncoding(Charset charset) {
        this.html2Txt.setOutputCharset(charset);
    }
}
