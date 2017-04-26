
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

package test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.unkrig.html2txt.Html2Txt;
import de.unkrig.html2txt.Html2Txt.HtmlException;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the "html2txt" tool.
 */
public
class TestHtml2Txt {

    private static final String ONE_THRU_TWENTYFIVE = (
        "one two three four five six seven eight nine ten eleven twelve thirteen fourteen\n"
        + "      fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-two twenty-three\n"
        + "      twenty-four twenty-five"
    );

    @Test public void
    testSimple() throws Exception {

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "BLA\n"
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
    testMarkup() throws Exception {

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "This is a link (see \"http://unkrig.de\").\n"
            + "This is an anchor.\n"
            + "This is an abbreviation.\n"
            + "This is an acronym.\n"
            + "This is *bold text*.\n"
            + "This is big text.\n"
            + "This is [ a button ].\n"
            + "This is a citation.\n"
            + "This is code.\n"
            + "This is dfn.\n"
            + "This text is <emphasized>.\n"
            + "This is <text in italics>.\n"
            + "This is an image: [IMG]\n"
            + "This is an input control: []\n"
            + "This is [ keyboard text ].\n"
            + "This is a label.\n"
            + "This is \"a quote\".\n"
            + "This is a sample.\n"
            + "This is [ a select form element ].\n"
            + "This is small text.\n"
            + "This is a span element.\n"
            + "This is *strong text*.\n"
            + "This is subscript text.\n"
            + "This is ^superscript text.\n"
            + "This is [ a textarea form element ].\n"
            + "This is teletype-style text.\n"
            + "This is _underlined text_.\n"
            + "This is <variable text>.\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    This is <a href=\"http://unkrig.de\">a link</a>.<br />\n"
            + "    This is <a name=\"middle\" />an anchor.<br />\n"
            + "    This is <abbr>an abbreviation</abbr>.<br />\n"
            + "    This is <acronym>an acronym</acronym>.<br />\n"
            + "    This is <b>bold text</b>.<br />\n"
            + "    This is <big>big text</big>.<br />\n"
            + "    This is <button>a button</button>.<br />\n"
            + "    This is <cite>a citation</cite>.<br />\n"
            + "    This is <code>code</code>.<br />\n"
            + "    This is <dfn>dfn</dfn>.<br />\n"
            + "    This text is <em>emphasized</em>.<br />\n"
            + "    This is <i>text in italics</i>.<br />\n"
            + "    This is an image: <img /><br />\n"
            + "    This is an input control: <input /><br />\n"
            + "    This is <kbd>keyboard text</kbd>.<br />\n"
            + "    This is <label>a label</label>.<br />\n"
            + "    This is <q>a quote</q>.<br />\n"
            + "    This is <samp>a sample</samp>.<br />\n"
            + "    This is <select>a select form element</select>.<br />\n"
            + "    This is <small>small text</small>.<br />\n"
            + "    This is <span>a span element</span>.<br />\n"
            + "    This is <strong>strong text</strong>.<br />\n"
            + "    This is <sub>subscript text</sub>.<br />\n"
            + "    This is <sup>superscript text</sup>.<br />\n"
            + "    This is <textarea>a textarea form element</textarea>.<br />\n"
            + "    This is <tt>teletype-style text</tt>.<br />\n"
            + "    This is <u>underlined text</u>.<br />\n"
            + "    This is <var>variable text</var>.\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    @Test public void
    testUl() throws Exception {

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "zero\n"
            + " * one\n"
            + "   one-and-a-half\n"
            + " * two\n"
            + "    * three\n"
            + "      three-and-a-half\n"
            + "    * four\n"
            + "      five\n"
            + " * five\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    zero\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "  1. one\n"
            + "     one-and-a-half\n"
            + "  2. two\n"
            + "      * three\n"
            + "        three-and-a-half\n"
            + "      * four\n"
            + "        five\n"
            + "  3. five\n"
            + "       a. six\n"
            + "          six-and-a-half\n"
            + "       b. seven\n"
            + "            i. eight\n"
            + "               eight-and-a-half\n"
            + "           ii. nine\n"
            + "               ten\n"
            + "          iii. eleven\n"
            + "               twelve\n"
            + "           iv. thirteen\n"
            + "               fourteen\n"
            + "       c. fifteen\n"
            + "  4. sixteen\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "alpha beta\n"
            + "gamma delta\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "+-----+-----+\n"
            + "|alpha|beta |\n"
            + "+-----+-----+\n"
            + "|gamma|delta|\n"
            + "+-----+-----+\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "++=====++=====++\n"
            + "||alpha||beta ||\n"
            + "++=====++=====++\n"
            + "||gamma||delta||\n"
            + "++=====++=====++\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "+----+----------+------+\n"
            + "|eins|zwei      |drei  |\n"
            + "+----+----+-----+------+\n"
            + "|vier|fünf|sechs|sieben|\n"
            + "+----+----+-----+------+\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "+----+----+----+\n"
            + "|eins|zwei|drei|\n"
            + "+----+    +----+\n"
            + "|vier|    |fünf|\n"
            + "+----+----+----+\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "+----+----+-----+\n"
            + "|eins|zwei|drei |\n"
            + "+====+----+=====+\n"
            + "|vier|fünf|sechs|\n"
            + "+----+----+-----+\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "+--------------------------------------+--------------------------------------+\n"
            + "|one two three four five six seven     |alpha                                 |\n"
            + "|eight nine ten eleven twelve thirteen |                                      |\n"
            + "|fourteen fifteen sixteen seventeen    |                                      |\n"
            + "|eighteen nineteen twenty twenty-one   |                                      |\n"
            + "|twenty-two twenty-three twenty-four   |                                      |\n"
            + "|twenty-five                           |                                      |\n"
            + "+--------------------------------------+--------------------------------------+\n"
            + "|beta                                  |one two three four five six seven     |\n"
            + "|                                      |eight nine ten eleven twelve thirteen |\n"
            + "|                                      |fourteen fifteen sixteen seventeen    |\n"
            + "|                                      |eighteen nineteen twenty twenty-one a |\n"
            + "|                                      |b c twenty-two twenty-three twenty-   |\n"
            + "|                                      |four twenty-five                      |\n"
            + "+--------------------------------------+--------------------------------------+\n"
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

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "   +------------+------------+\n"
            + "   |one         |alpha       |\n"
            + "   |two         |            |\n"
            + "   |three       |            |\n"
            + "   |four        |            |\n"
            + "   |five        |            |\n"
            + "   |six         |            |\n"
            + "   |seven       |            |\n"
            + "   |eight       |            |\n"
            + "   |nine        |            |\n"
            + "   |ten         |            |\n"
            + "   |eleven      |            |\n"
            + "   |twelve      |            |\n"
            + "   |thirteen    |            |\n"
            + "   |fourteen    |            |\n"
            + "   |fifteen     |            |\n"
            + "   |sixteen     |            |\n"
            + "   |seventeen   |            |\n"
            + "   |eighteen    |            |\n"
            + "   |nineteen    |            |\n"
            + "   |twenty      |            |\n"
            + "   |twenty-one  |            |\n"
            + "   |twenty-two  |            |\n"
            + "   |twenty-three|            |\n"
            + "   |twenty-four |            |\n"
            + "   |twenty-five |            |\n"
            + "   +------------+------------+\n"
            + "   |beta        |one         |\n"
            + "   |            |two         |\n"
            + "   |            |three       |\n"
            + "   |            |four        |\n"
            + "   |            |five        |\n"
            + "   |            |six         |\n"
            + "   |            |seven       |\n"
            + "   |            |eight       |\n"
            + "   |            |nine        |\n"
            + "   |            |ten         |\n"
            + "   |            |eleven      |\n"
            + "   |            |twelve      |\n"
            + "   |            |thirteen    |\n"
            + "   |            |fourteen    |\n"
            + "   |            |fifteen     |\n"
            + "   |            |sixteen     |\n"
            + "   |            |seventeen   |\n"
            + "   |            |eighteen    |\n"
            + "   |            |nineteen    |\n"
            + "   |            |twenty      |\n"
            + "   |            |twenty-one  |\n"
            + "   |            |a           |\n"
            + "   |            |b           |\n"
            + "   |            |c           |\n"
            + "   |            |twenty-two  |\n"
            + "   |            |twenty-three|\n"
            + "   |            |twenty-four |\n"
            + "   |            |twenty-five |\n"
            + "   +------------+------------+\n"
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

    @Test public void
    testAlign1() throws Exception {

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + "fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "two twenty-three twenty-four twenty-five\n"
            + ""
            + "        one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + " fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "                                       two twenty-three twenty-four twenty-five\n"
            + ""
            + "    one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + "fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "                   two twenty-three twenty-four twenty-five\n"
            + ""
            + "one  two  three four  five  six seven  eight  nine ten  eleven  twelve thirteen\n"
            + "fourteen  fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "two twenty-three twenty-four twenty-five\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <div align=\"left\">"    + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "</div>\n"
            + "    <div align=\"right\">"   + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "</div>\n"
            + "    <div align=\"center\">"  + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "</div>\n"
            + "    <div align=\"justify\">" + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "</div>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    @Test public void
    testAlign2() throws Exception {

        TestHtml2Txt.assertHtml2Txt((
            ""
            + "        one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + " fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "                                       two twenty-three twenty-four twenty-five\n"
            + ""
            + "        one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + " fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "                                       two twenty-three twenty-four twenty-five\n"
            + ""
            + "    one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + "fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "                   two twenty-three twenty-four twenty-five\n"
            + ""
            + "        one two three four five six seven eight nine ten eleven twelve thirteen\n"
            + " fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-\n"
            + "                                       two twenty-three twenty-four twenty-five\n"
        ), (
            ""
            + "<html>\n"
            + "  <body>\n"
            + "    <div align=\"right\">\n"
            + "      " + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "\n"
            + "      <div>\n"
            + "        " + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "\n"
            + "      </div>\n"
            + "      <div align=\"center\">\n"
            + "        " + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "\n"
            + "      </div>\n"
            + "      " + TestHtml2Txt.ONE_THRU_TWENTYFIVE + "\n"
            + "    </div>\n"
            + "  </body>\n"
            + "</html>\n"
        ));
    }

    /**
     * Converts an HTML document to plain text and verifies that the result equals <var>expected</var>.
     *
     * @param expected The expected output of the conversion; lines separated with {@code "\n"}
     * @param html     The HTML document that to convert
     */
    private static void
    assertHtml2Txt(String expected, String html)
    throws ParserConfigurationException, SAXException, TransformerException, HtmlException, IOException {
        TestHtml2Txt.assertHtml2Txt(expected, html, new Html2Txt());
    }

    /**
     * Uses the <var>html2Text</var> to convert an HTML document to plain text, and verifies that the result equals
     * <var>expected</var>.
     *
     * @param expected The expected output of the conversions; lines separated with {@code "\n"}
     * @param html     The HTML document that to convert
     * @param html2Txt The (possibly custom-configured) {@link Html2Txt} object that implements the conversion
     */
    private static void
    assertHtml2Txt(String expected, String html, Html2Txt html2Txt)
    throws ParserConfigurationException, SAXException, TransformerException, HtmlException, IOException {
        StringWriter sw = new StringWriter();
        html2Txt.html2txt(new StringReader(html), sw);
        Assert.assertEquals(expected, sw.toString().replace(System.getProperty("line.separator"), "\n"));
    }
}
