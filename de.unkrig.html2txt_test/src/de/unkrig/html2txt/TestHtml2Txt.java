
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

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.xml.sax.SAXException;

import de.unkrig.html2txt.Html2Txt.HtmlException;

public class TestHtml2Txt {

    @Test public void
    testSimple() throws Exception {
        
        assertHtml2Txt((
            ""
            + "BLA\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    BLA\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }
    
    @Test public void
    testTable() throws Exception {

        assertHtml2Txt((
            ""
            + "alpha beta\r\n"
            + "gamma delta\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <table>\n"
            + "      <tr><td>alpha</td><td>beta</td></tr>\n"
            + "      <tr><td>gamma</td><td>delta</td></tr>\n"
            + "    </table>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }
    
    @Test public void
    testTableBorder1() throws Exception {
        
        assertHtml2Txt((
            ""
                + "+-----+-----+\r\n"
                + "|alpha|beta |\r\n"
                + "+-----+-----+\r\n"
                + "|gamma|delta|\r\n"
                + "+-----+-----+\r\n"
            ), (
                ""
                + "<html>\n"
                + "  <body>\n"
                + "    <table border=\"1\">\n"
                + "      <tr><td>alpha</td><td>beta</td></tr>\n"
                + "      <tr><td>gamma</td><td>delta</td></tr>\n"
                + "    </table>\n"
                + "  </body>\n"
                + "</html>\n"
            ));
    }

    @Test public void
    testTableBorder2() throws Exception {
        
        assertHtml2Txt((
            ""
            + "++=====++=====++\r\n"
            + "||alpha||beta ||\r\n"
            + "++=====++=====++\r\n"
            + "||gamma||delta||\r\n"
            + "++=====++=====++\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <table border=\"2\">\n"
            + "      <tr><td>alpha</td><td>beta</td></tr>\n"
            + "      <tr><td>gamma</td><td>delta</td></tr>\n"
            + "    </table>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }
    
    @Test public void
    testTableColspan() throws Exception {
        
        assertHtml2Txt((
            ""
                + "+----+----------+------+\r\n"
                + "|eins|zwei      |drei  |\r\n"
                + "+----+----+-----+------+\r\n"
                + "|vier|fünf|sechs|sieben|\r\n"
                + "+----+----+-----+------+\r\n"
            ), (
                ""
                    + "<html>\n"
                    + "  <body>\n"
                    + "    <table border=\"1\">\n"
                    + "      <tr><td>eins</td><td colspan=\"2\">zwei</td><td>drei</td></tr>\n"
                    + "      <tr><td>vier</td><td>fünf</td><td>sechs</td><td>sieben</td></tr>\n"
                    + "    </table>\n"
                    + "  </body>\n"
                    + "</html>\n"
                ));
    }
    
    @Test public void
    testTableRowspan() throws Exception {
        
        assertHtml2Txt((
            ""
            + "+----+----+----+\r\n"
            + "|eins|zwei|drei|\r\n"
            + "+----+    +----+\r\n"
            + "|vier|    |fünf|\r\n"
            + "+----+----+----+\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <table border=\"1\">\n"
            + "      <tr><td>eins</td><td rowspan=\"2\">zwei</td><td>drei</td></tr>\n"
            + "      <tr><td>vier</td><td>fünf</td></tr>\n"
            + "    </table>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    private void
    assertHtml2Txt(String expected, String html)
    throws ParserConfigurationException, SAXException, TransformerException, HtmlException {
        StringWriter sw = new StringWriter();
        new Html2Txt().html2txt(new StringReader(html), sw);
        assertEquals(expected, sw.toString());
    }
}
