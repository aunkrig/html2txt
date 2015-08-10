
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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Assert;
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
    testUl() throws Exception {

        assertHtml2Txt((
            ""
            + "zero\r\n"
            + " * one\r\n"
            + "   one-and-a-half\r\n"
            + " * two\r\n"
            + "    * three\r\n"
            + "      three-and-a-half\r\n"
            + "    * four\r\n"
            + "      five\r\n"
            + " * five\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "  zero\n"
            + "    <ul>\n"
            + "      <li>one</li>\n"
            + "      one-and-a-half\n"
            + "      <li>two</li>\n"
            + "      <ul>\n"
            + "        <li>three</li>\n"
            + "        three-and-a-half\n"
            + "        <li>four<p>five</p></li>\n"
            + "      </ul>\n"
            + "      <li>five</li>\n"
            + "    </ul>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    @Test public void
    testOl() throws Exception {

        assertHtml2Txt((
            ""
            + "  1. one\r\n"
            + "     one-and-a-half\r\n"
            + "  2. two\r\n"
            + "      * three\r\n"
            + "        three-and-a-half\r\n"
            + "      * four\r\n"
            + "        five\r\n"
            + "  3. five\r\n"
            + "       a. six\r\n"
            + "          six-and-a-half\r\n"
            + "       b. seven\r\n"
            + "            i. eight\r\n"
            + "               eight-and-a-half\r\n"
            + "           ii. nine\r\n"
            + "               ten\r\n"
            + "          iii. eleven\r\n"
            + "               twelve\r\n"
            + "           iv. thirteen\r\n"
            + "               fourteen\r\n"
            + "       c. fifteen\r\n"
            + "  4. sixteen\r\n"
        ), (
            ""
            + "<ol>\n"
            + "  <li>one</li>\n"
            + "  one-and-a-half\n"
            + "  <li>two</li>\n"
            + "  <ul>\n"
            + "    <li>three</li>\n"
            + "    three-and-a-half\n"
            + "    <li>four<p>five</p></li>\n"
            + "  </ul>\n"
            + "  <li>five</li>\n"
            + "  <ol type=\"a\">\n"
            + "    <li>six</li>\n"
            + "    six-and-a-half\n"
            + "    <li>seven</li>\n"
            + "    <ol type=\"i\">\n"
            + "      <li>eight</li>\n"
            + "      eight-and-a-half\n"
            + "      <li>nine<p>ten</p></li>\n"
            + "      <li>eleven<p>twelve</p></li>\n"
            + "      <li>thirteen<p>fourteen</p></li>\n"
            + "    </ol>\n"
            + "    <li>fifteen</li>\n"
            + "  </ol>\n"
            + "  <li>sixteen</li>\n"
            + "</ol>\n"
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

    @Test public void
    testTh() throws Exception {

        assertHtml2Txt((
            ""
            + "+----+----+-----+\r\n"
            + "|eins|zwei|drei |\r\n"
            + "+====+----+=====+\r\n"
            + "|vier|fünf|sechs|\r\n"
            + "+----+----+-----+\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <table border=\"1\">\n"
            + "      <tr><th>eins</th><td>zwei</td><th>drei</th></tr>\n"
            + "      <tr><td>vier</td><td>fünf</td><td>sechs</td></tr>\n"
            + "    </table>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    @Test public void
    testBig1() throws Exception {

        assertHtml2Txt((
            ""
            + "+--------------------------------------+--------------------------------------+\r\n"
            + "|one two three four five six seven     |alpha                                 |\r\n"
            + "|eight nine ten eleven twelve thirteen |                                      |\r\n"
            + "|fourteen fifteen sixteen seventeen    |                                      |\r\n"
            + "|eighteen nineteen twenty twenty-one   |                                      |\r\n"
            + "|twenty-two twenty-three twenty-four   |                                      |\r\n"
            + "|twenty-five                           |                                      |\r\n"
            + "+--------------------------------------+--------------------------------------+\r\n"
            + "|beta                                  |one two three four five six seven     |\r\n"
            + "|                                      |eight nine ten eleven twelve thirteen |\r\n"
            + "|                                      |fourteen fifteen sixteen seventeen    |\r\n"
            + "|                                      |eighteen nineteen twenty twenty-one a |\r\n"
            + "|                                      |b c twenty-two twenty-three twenty-   |\r\n"
            + "|                                      |four twenty-five                      |\r\n"
            + "+--------------------------------------+--------------------------------------+\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <table border=\"1\">\n"
            + "      <tr>\n"
            + "        <td>\n"
            + "          one two three four five six seven eight nine ten eleven twelve thirteen fourteen\n"
            + "          fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-two twenty-three\n"
            + "          twenty-four twenty-five\n"
            + "        </td>\n"
            + "        <td>alpha</td>\n"
            + "      </tr>\n"
            + "      <tr>\n"
            + "        <td>beta</td>\n"
            + "        <td>\n"
            + "          one two three four five six seven eight nine ten eleven twelve thirteen fourteen\n"
            + "          fifteen sixteen seventeen eighteen nineteen twenty twenty-one a b c twenty-two twenty-three\n"
            + "          twenty-four twenty-five\n"
            + "        </td>\n"
            + "      </tr>\n"
            + "    </table>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    @Test public void
    testBig2() throws Exception {

        assertHtml2Txt((
            ""
            + "   +------------+------------+\r\n"
            + "   |one         |alpha       |\r\n"
            + "   |two         |            |\r\n"
            + "   |three       |            |\r\n"
            + "   |four        |            |\r\n"
            + "   |five        |            |\r\n"
            + "   |six         |            |\r\n"
            + "   |seven       |            |\r\n"
            + "   |eight       |            |\r\n"
            + "   |nine        |            |\r\n"
            + "   |ten         |            |\r\n"
            + "   |eleven      |            |\r\n"
            + "   |twelve      |            |\r\n"
            + "   |thirteen    |            |\r\n"
            + "   |fourteen    |            |\r\n"
            + "   |fifteen     |            |\r\n"
            + "   |sixteen     |            |\r\n"
            + "   |seventeen   |            |\r\n"
            + "   |eighteen    |            |\r\n"
            + "   |nineteen    |            |\r\n"
            + "   |twenty      |            |\r\n"
            + "   |twenty-one  |            |\r\n"
            + "   |twenty-two  |            |\r\n"
            + "   |twenty-three|            |\r\n"
            + "   |twenty-four |            |\r\n"
            + "   |twenty-five |            |\r\n"
            + "   +------------+------------+\r\n"
            + "   |beta        |one         |\r\n"
            + "   |            |two         |\r\n"
            + "   |            |three       |\r\n"
            + "   |            |four        |\r\n"
            + "   |            |five        |\r\n"
            + "   |            |six         |\r\n"
            + "   |            |seven       |\r\n"
            + "   |            |eight       |\r\n"
            + "   |            |nine        |\r\n"
            + "   |            |ten         |\r\n"
            + "   |            |eleven      |\r\n"
            + "   |            |twelve      |\r\n"
            + "   |            |thirteen    |\r\n"
            + "   |            |fourteen    |\r\n"
            + "   |            |fifteen     |\r\n"
            + "   |            |sixteen     |\r\n"
            + "   |            |seventeen   |\r\n"
            + "   |            |eighteen    |\r\n"
            + "   |            |nineteen    |\r\n"
            + "   |            |twenty      |\r\n"
            + "   |            |twenty-one  |\r\n"
            + "   |            |a           |\r\n"
            + "   |            |b           |\r\n"
            + "   |            |c           |\r\n"
            + "   |            |twenty-two  |\r\n"
            + "   |            |twenty-three|\r\n"
            + "   |            |twenty-four |\r\n"
            + "   |            |twenty-five |\r\n"
            + "   +------------+------------+\r\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <table border=\"1\">\n"
            + "      <tr>\n"
            + "        <td>\n"
            + "          one two three four five six seven eight nine ten eleven twelve thirteen fourteen\n"
            + "          fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-two twenty-three\n"
            + "          twenty-four twenty-five\n"
            + "        </td>\n"
            + "        <td>alpha</td>\n"
            + "      </tr>\n"
            + "      <tr>\n"
            + "        <td>beta</td>\n"
            + "        <td>\n"
            + "          one two three four five six seven eight nine ten eleven twelve thirteen fourteen\n"
            + "          fifteen sixteen seventeen eighteen nineteen twenty twenty-one a b c twenty-two twenty-three\n"
            + "          twenty-four twenty-five\n"
            + "        </td>\n"
            + "      </tr>\n"
            + "    </table>\n"
            + "  </body>\n"
            + "</html>\n"
        ), new Html2Txt().setPageLeftMarginWidth(3).setPageRightMarginWidth(5).setPageWidth(9));
    }

    private void
    assertHtml2Txt(String expected, String html)
    throws ParserConfigurationException, SAXException, TransformerException, HtmlException {
        assertHtml2Txt(expected, html, new Html2Txt());
    }

    private void
    assertHtml2Txt(String expected, String html, Html2Txt html2Txt)
        throws ParserConfigurationException, SAXException, TransformerException, HtmlException {
        StringWriter sw = new StringWriter();
        html2Txt.html2txt(new StringReader(html), sw);
        Assert.assertEquals(expected, sw.toString());
    }
}
