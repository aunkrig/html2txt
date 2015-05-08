
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
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.xml.XmlUtil;
import de.unkrig.commons.util.collections.CollectionUtil;

/**
 * A command
 */
public
class Html2Txt {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /** All methods of this {@link ErrorHandler} throw the {@link SAXException} they recieve. */
    @SuppressWarnings("null")
    public static final ErrorHandler
    SIMPLE_SAX_ERROR_HANDLER = new ErrorHandler() {
        @Override public void warning(@Nullable SAXParseException e)    throws SAXParseException { throw e; }
        @Override public void fatalError(@Nullable SAXParseException e) throws SAXParseException { throw e; }
        @Override public void error(@Nullable SAXParseException e)      throws SAXParseException { throw e; }
    };

    /** All methods of theis {@link HtmlErrorHandler} throw the {@link HtmlException} they recieve. */
    public static final HtmlErrorHandler
    SIMPLE_HTML_ERROR_HANDLER = new HtmlErrorHandler() {
        @Override public void warning(HtmlException e)    throws HtmlException { throw e; }
        @Override public void fatalError(HtmlException e) throws HtmlException { throw e; }
        @Override public void error(HtmlException e)      throws HtmlException { throw e; }
    };

    private HtmlErrorHandler htmlErrorHandler = Html2Txt.SIMPLE_HTML_ERROR_HANDLER;

    private int pageLeftMarginWidth  /*= 0*/;
    private int pageRightMarginWidth = 1;
    private int pageWidth;
    {
        try {
            this.pageWidth = Integer.parseInt(System.getenv("COLUMNS"));
        } catch (Exception e) {
            this.pageWidth = 80;
        }
    }

    /**
     * Representation of an exceptional condition that occurred during HTML processing. This exception is always
     * related to a node in the HTML DOM.
     *
     * @see #getNode()
     */
    public static
    class HtmlException extends Exception {

        private static final long serialVersionUID = 1L;

        private final Node node;

        public
        HtmlException(Node node, String message) {
            super(message);
            this.node = node;
        }

        public Node
        getNode() { return this.node; }
    }

    /** Handles {@link HtmlException}s. */
    public
    interface HtmlErrorHandler {
        // SUPPRESS CHECKSTYLE JavadocMethod:3
        void warning(HtmlException e)    throws HtmlException;
        void fatalError(HtmlException e) throws HtmlException;
        void error(HtmlException e)      throws HtmlException;
    }

    /**
     * Formats an HTML block element.
     */
    public
    interface BlockElementFormatter {

        /**
         * Appends lines to the <var>result</var>. The first <var>leftMarginWidth</var> characters of each produced
         * line are spaces (except for the first line, where the string produced by {@link Html2Txt.Bulleting#next()}
         * is placed in the left margin, followed by up to <var>measure</var> characters
         */
        void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException;
    }

    /**
     * Formats an HTML inline element.
     */
    public
    interface InlineElementFormatter {

        /**
         * Appends characters to the <var>result</var>; "{@code \n}" represents a "break" ("{@code <br />}").
         */
        void format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException;
    }

    interface Bulleting {

        /**
         * @return The text for the "next" bullet, e.g. "7.", "G.", "vii."
         */
        String next();

        /**
         * {@link #next()} always returns the empty string.
         */
        Bulleting NONE = new Bulleting() {  @Override public String next() { return ""; } };
    }

    enum NumberingType {

        /**
         * <dl>
         *   <dt>0</dt><dd>({@code NumberFormatException})</dd>
         *   <dt>1</dt><dd>"{@code a}"</dd>
         *   <dt>2</dt><dd>"{@code b}"</dd>
         *   <dt>26</dt><dd>"{@code z}"</dd>
         *   <dt>27</dt><dd>"{@code aa}"</dd>
         *   <dt>28</dt><dd>"{@code ab}"</dd>
         *   <dt>702</dt><dd>"{@code zz}"</dd>
         *   <dt>703</dt><dd>"{@code aaa}"</dd>
         * </dl>
         * Etc.
         */
        LOWERCASE_LETTERS {

            @Override public long
            parse(String s) {
                long result = 0;
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c >= 'A' && c <= 'Z') {
                        result = 26 * result + (c - 'A') + 1;
                    } else
                    if (c >= 'a' && c <= 'z') {
                        result = 26 * result + (c - 'a') + 1;
                    } else
                    {
                        throw new NumberFormatException();
                    }
                }
                return result;
            }

            @Override public String
            toString(long value) {

                if (value < 0) return '-' + this.toString(-value);
                if (value == 0) throw new NumberFormatException();
                if (value <= 26) return String.valueOf((char) (value + 'a' - 1));
                return this.toString(value / 26) + ((char) ((value % 26) + 'a' - 1));
            }
        },

        /**
         * <dl>
         *   <dt>0</dt><dd>({@code NumberFormatException})</dd>
         *   <dt>1</dt><dd>"{@code A}"</dd>
         *   <dt>2</dt><dd>"{@code B}"</dd>
         *   <dt>26</dt><dd>"{@code Z}"</dd>
         *   <dt>27</dt><dd>"{@code AA}"</dd>
         *   <dt>28</dt><dd>"{@code AB}"</dd>
         *   <dt>702</dt><dd>"{@code ZZ}"</dd>
         *   <dt>703</dt><dd>"{@code AAA}"</dd>
         * </dl>
         * Etc.
         */
        UPPERCASE_LETTERS {

            @Override public long parse(String s) { return LOWERCASE_LETTERS.parse(s); }

            @Override public String
            toString(long value) {

                if (value < 0) return '-' + this.toString(-value);
                if (value == 0) throw new NumberFormatException();
                if (value <= 26) return String.valueOf((char) (value + 'A' - 1));
                return this.toString(value / 26) + ((char) ((value % 26) + 'A' - 1));
            }
        },

        /**
         * <dl>
         *   <dt>0</dt><dd>({@code NumberFormatException})</dd>
         *   <dt>1</dt><dd>"{@code i}"</dd>
         *   <dt>2</dt><dd>"{@code ii}"</dd>
         *   <dt>3</dt><dd>"{@code iii}"</dd>
         *   <dt>4</dt><dd>"{@code iv}"</dd>
         *   <dt>9999</dt><dd>"{@code mmmmmmmmmcmlcix}"</dd>
         *   <dt>10000</dt><dd>({@code NumberFormatException})</dd>
         * </dl>
         * Etc.
         */
        LOWERCASE_ROMAN_NUMERALS {

            private final String[][] ds = {
                " i ii iii iv v vi vii viii ix".split(" "),
                " x xx xxx xl l lx lxx lxxx lc".split(" "),
                " c cc ccc cd d dc dcc dccc cm".split(" "),
                " m mm mmm mmmm mmmmm mmmmmm mmmmmmm mmmmmmmm mmmmmmmmm".split(" "),
            };

            @Override public long
            parse(String s) {
                if (s.isEmpty()) throw new NumberFormatException();
                s = s.toLowerCase();

                long result = 0;
                for (int i = 3; i >= 0; i--) {
                    for (int j = 9;; j--) {
                        String d = this.ds[i][j];
                        if (s.startsWith(d)) {
                            result = 10 * result + j;
                            break;
                        }
                    }
                }
                return result;
            }

            @Override public String
            toString(long value) {
                if (value == 0) throw new NumberFormatException();
                if (value < 0) return '-' + this.toString(-value);
                if (value >= 10000) throw new NumberFormatException();

                if (value <= 9) return this.ds[0][(int) value];
                StringBuilder sb = new StringBuilder();
                if (value >= 1000) {
                    sb.append(this.ds[3][(int) value / 1000]);
                    value %= 1000;
                }
                if (value >= 100) {
                    sb.append(this.ds[2][(int) value / 100]);
                    value %= 100;
                }
                if (value >= 10) {
                    sb.append(this.ds[1][(int) value / 10]);
                    value %= 10;
                }
                if (value >= 1) {
                    sb.append(this.ds[0][(int) value]);
                }
                return sb.toString();
            }
        },

        /**
         * <dl>
         *   <dt>0</dt><dd>({@code NumberFormatException})</dd>
         *   <dt>1</dt><dd>"{@code I}"</dd>
         *   <dt>2</dt><dd>"{@code II}"</dd>
         *   <dt>3</dt><dd>"{@code III}"</dd>
         *   <dt>4</dt><dd>"{@code IV}"</dd>
         *   <dt>9999</dt><dd>"{@code MMMMMMMMMCMLCIX}"</dd>
         *   <dt>10000</dt><dd>({@code NumberFormatException})</dd>
         * </dl>
         * Etc.
         */
        UPPERCASE_ROMAN_LITERALS {
            @Override public long   parse(String s)      { return NumberingType.LOWERCASE_ROMAN_NUMERALS.parse(s); }
            @Override public String toString(long value) { return LOWERCASE_ROMAN_NUMERALS.toString().toUpperCase(); }
        },

        /**
         * @see Long#parseLong(String)
         * @see Long#toString(long)
         */
        ARABIC_DIGITS {
            @Override public long   parse(String s)      { return Long.parseLong(s); }
            @Override public String toString(long value) { return Long.toString(value); }
        };

        /**
         * Converts the given string to an integral value.
         */
        public abstract long parse(String s);

        /**
         * Converts the given integral value to a string. Notice that some {@link NumberingType}s do not support the
         * value zero, or numbers greater than 9999.
         */
        public abstract String toString(long value);
    }

    /**
     * Sets a custom {@link HtmlErrorHandler} on this object. The default handler is {@link
     * #SIMPLE_HTML_ERROR_HANDLER}.
     */
    public void
    setErrorHandler(HtmlErrorHandler htmlErrorHandler) {
        this.htmlErrorHandler = htmlErrorHandler;
    }

    /**
     * The number of spaces that preceeds each line of output; defaults to zero.
     */
    public void
    setPageLeftMarginWidth(int pageLeftMarginWidth) { this.pageLeftMarginWidth = pageLeftMarginWidth; }

    /**
     * The maximum length of output lines is "<var>pageWidth</var> - <var>rightMarginWidth</var>".
     * <p>
     *   Defaults to "{@code 1}", to avoid extra line wraps on certain terminals.
     * </p>
     *
     * @see #setPageWidth(int)
     */
    public void
    setPageRightMarginWidth(int pageRightMarginWidth) { this.pageRightMarginWidth = pageRightMarginWidth; }

    /**
     * The maximum length of output lines is "<var>pageWidth</var> - <var>rightMarginWidth</var>".
     * <p>
     *   Defaults to the value of the environment variable "{@code $COLUMNS}", or, if that is not set, to 80.
     * </p>
     *
     * @see #setPageRightMarginWidth(int)
     */
    public void
    setPageWidth(int pageWidth) { this.pageWidth = pageWidth; }

    /**
     * Reads, scans and parses the HTML document in the {@code inputFile}, generates a plain text document, and
     * writes it to the {@code output}.
     */
    public void
    html2txt(File inputFile, Writer output)
    throws ParserConfigurationException, SAXException, TransformerException, HtmlException {

        final Document document;
        {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            db.setErrorHandler(Html2Txt.SIMPLE_SAX_ERROR_HANDLER);

//            document = db.parse(inputFile);
            document = XmlUtil.parse(db, inputFile);

            document.getDocumentElement().normalize();
        }

//        TransformerFactory.newInstance().newTransformer().transform(
//            new DOMSource(document),
//            new StreamResult(new File("output.xml"))
//        );

        PrintWriter pw = output instanceof PrintWriter ? (PrintWriter) output : new PrintWriter(output);

        this.html2txt(document, LineUtil.lineConsumer(pw));
    }

    /**
     * Reads, scans and parses the HTML document in the {@code inputFile}, generates a plain text document, and
     * writes it to the {@code outputFile}.
     */
    public void
    html2txt(final File inputFile, File outputFile) throws Exception {

        FileUtil.printToFile(
            outputFile,
            Charset.forName("ISO8859-1"),
            new ConsumerWhichThrows<PrintWriter, Exception>() {

                @Override public void
                consume(PrintWriter pw) throws Exception { Html2Txt.this.html2txt(inputFile, pw); }
            }
        );
    }

    private void
    html2txt(Document document, Consumer<? super CharSequence> output) throws HtmlException {

        // Some block tags render vertical space, which we want to compress.
        output = ConsumerUtil.<CharSequence>compress(output, StringUtil.IS_BLANK, "");

        Element documentElement = document.getDocumentElement();

        // Iff the document is structured like
        //
        //     <html>
        //       ...
        //       <body>...</body>
        //       ...
        //       <body>...</body>
        //       ...
        //     </html>
        //     ...
        //
        // , then the result is the formatted <body>s.
        if ("html".equals(documentElement.getNodeName())) {
            for (Node n : XmlUtil.iterable(documentElement.getChildNodes())) {
                if (n.getNodeType() == Node.ELEMENT_NODE && "body".equals(n.getNodeName())) {
                    Element bodyElement = (Element) n;
                    this.formatBlocks(
                        this.pageLeftMarginWidth,
                        Bulleting.NONE,
                        this.pageWidth - this.pageLeftMarginWidth - this.pageRightMarginWidth,
                        XmlUtil.iterable(bodyElement.getChildNodes()),
                        output
                    );
                }
            }

            return;
        }

        // Otherwise, assume that the document poses an HTML *fragment*, and the top level nodes ar *blocks*.
        this.formatBlocks(
            this.pageLeftMarginWidth,
            Bulleting.NONE,
            this.pageWidth - this.pageLeftMarginWidth - this.pageRightMarginWidth,
            Collections.singletonList(documentElement),
            output
        );
    }

    /**
     * Formats a sequence of {@link Node#TEXT_NODE TEXT} nodes and HTML block {@link Node#ELEMENT_NODE ELEMENT} nodes.
     */
    private <N extends Node> void
    formatBlocks(
        int                            leftMarginWidth,
        Bulleting                      bulleting,
        int                            measure,
        Iterable<N>                    nodes,
        Consumer<? super CharSequence> output
    ) throws HtmlException {

        List<Node> inlineNodes = new ArrayList<Node>();
        for (Node n : nodes) {
            if (n.getNodeType() == Node.TEXT_NODE) {
                inlineNodes.add(n);
            } else
            if (Html2Txt.isInlineElement(n)) {
                inlineNodes.add(n);
            } else
            if (Html2Txt.isBlockElement(n)) {
                if (!inlineNodes.isEmpty()) {
                    this.wordWrap(
                        leftMarginWidth,
                        bulleting,
                        measure,
                        this.getBlock(inlineNodes),
                        output,
                        inlineNodes.get(0) // ref
                    );
                    inlineNodes.clear();
                }

                Element e = (Element) n;

                BlockElementFormatter bef = Html2Txt.ALL_BLOCK_ELEMENTS.get(e.getTagName());
                if (bef == null) {
                    this.htmlErrorHandler.error(
                        new HtmlException(n, "Unexpected block element \"" + XmlUtil.toString(e) + "\" in block")
                    );
                } else {
                    bef.format(this, leftMarginWidth, bulleting, measure, e, output);
                }
            } else
            {
                this.htmlErrorHandler.error(
                    new HtmlException(n, "Unexpected node \"" + XmlUtil.toString(n) + "\" in <body>")
                );
            }
        }

        if (!inlineNodes.isEmpty()) {
            this.wordWrap(
                leftMarginWidth,
                bulleting,
                measure,
                this.getBlock(inlineNodes),
                output,
                inlineNodes.get(0) // ref
            );
            inlineNodes.clear();
        }

    }

    /**
     * The given <var>text</var> is word-wrapped such that each output line begins with <var>leftMarginWidth</var>
     * spaces, followed by up to <var>measure</var> characters. If the <var>text</var> contains very long words, then
     * some of the output lines may be longer than "<var>leftMarginWidth</var> + <var>measure</var>".
     * <p>
     *   Newline characters ({@code '\n'}) appear as line breaks in the output.
     * </p>
     * <p>
     *   The output lines are fed to the <var>lc</var>.
     * </p>
     * @param bulleting The string produced by {@link Bulleting#next()} is placed in the left margin of the first
     *                  line generated
     * @param ref       Is used iff an {@link HtmlException} is thrown ({@link HtmlException}s have a reference to the
     *                  "offending" node)
     */
    private void
    wordWrap(
        int                            leftMarginWidth,
        Bulleting                      bulleting,
        int                            measure,
        String                         text,
        Consumer<? super CharSequence> output,
        Node                           ref
    ) throws HtmlException {

        text = text.trim();
        if (text.length() == 0) return;

        if (measure < 1) measure = 1;

        // From this point on, the first letter of "text" is always a non-space character.

        for (int nlidx = text.indexOf('\n'); nlidx != -1; nlidx = text.indexOf('\n')) {
            this.wordWrap(leftMarginWidth, bulleting, measure, text.substring(0, nlidx), output, ref);
            for (nlidx++; nlidx < text.length() && text.charAt(nlidx) == ' '; nlidx++);
            if (nlidx == text.length()) return;
            text = text.substring(nlidx);
        }

        String continuationLineLeftMargin = StringUtil.repeat(leftMarginWidth, ' ');
        String leftMargin;
        {
            String bullet = bulleting.next();
            if (bullet.length() == 0) {
                leftMargin = continuationLineLeftMargin;
            } else
            if (bullet.length() + 1 < leftMarginWidth) {
                leftMargin = StringUtil.repeat(leftMarginWidth - bullet.length() - 1, ' ') + bullet + ' ';
            } else
            {
                leftMargin = bullet + ' ';
            }
        }

        for (;;) {

            if (text.length() <= measure) break;

            // Determine the point to wrap at.
            int idx1; // Space after the last word to keep in THIS line.
            int idx2; // First letter of the first word to put on the NEXT line.
            if (text.charAt(measure) == ' ') {
                for (idx1 = measure; idx1 > 0 && text.charAt(idx1 - 1) == ' '; idx1--);
                for (idx2 = measure + 1; idx2 < text.length() && text.charAt(idx2) == ' '; idx2++);
            } else
            {
                for (idx2 = measure; idx2 > 0 && text.charAt(idx2 - 1) != ' '; idx2--);
                if (idx2 == 0) {
                    for (idx1 = measure + 1; idx1 < text.length() && text.charAt(idx1) != ' '; idx1++);
                    if (idx1 == text.length()) break;
                    for (idx2 = idx1 + 1; idx2 < text.length() && text.charAt(idx2) == ' '; idx2++);
                    if (idx2 == text.length()) {
                        text = text.substring(0, idx1);
                        break;
                    }
                } else {
                    for (idx1 = idx2 - 1; text.charAt(idx1 - 1) == ' '; idx1--);
                }
            }

            output.consume(leftMargin + text.substring(0, idx1));

            text = text.substring(idx2);

            leftMargin = continuationLineLeftMargin;
        }

        output.consume(leftMargin + text);
    }

    /**
     * Formats text and inline elements into one long line, except for "{@code <br />}" tags, which map into
     * line breaks.
     */
    private String
    getBlock(Iterable<Node> nodes) throws HtmlException {
        StringBuilder sb = new StringBuilder();

        for (Node n : nodes) {
            short nodeType = n.getNodeType();

            if (nodeType == Node.TEXT_NODE) {
                String content = n.getTextContent();
                sb.append(content.replaceAll("\\s+", " "));
            } else
            if (nodeType == Node.ELEMENT_NODE) {
                Element e = (Element) n;

                InlineElementFormatter ief = Html2Txt.ALL_INLINE_ELEMENTS.get(e.getTagName());
                if (ief == null) {
                    this.htmlErrorHandler.error(
                        new HtmlException(n, "Unexpected element \"" + XmlUtil.toString(e) + "\" in block")
                    );
                } else {
                    ief.format(this, e, sb);
                }
            } else
            {
                this.htmlErrorHandler.error(new HtmlException(n, "Unexpected node in block"));
            }
        }
        return sb.toString();
    }

    /**
     * "Block-Level" is categorization of HTML elements, as contrasted with "inline" elements.
     * <p>
     *   Block-level elements may appear only within a {@code <body>} element.
     *   Their most significant characteristic is that they typically are formatted with a line break before and after
     *   the element (thereby creating a stand-alone block of content). That is, they take up the width of their
     *   containers.
     * </p>
     * <p>
     *   The distinction of block-level vs. inline elements is used in HTML specifications up  to 4.01. In HTML5, this
     *   binary distinction is replaced with a more complex set of content categories. The "block-level" category
     *   roughly corresponds to the category of flow content in HTML5, while "inline" corresponds to phrasing content,
     *   but there are additional categories.
     * </p>
     * <p>
     *   There are a couple of key differences between block-level elements and inline elements:
     * </p>
     * <dl>
     *   <dt>Formatting</dt>
     *   <dd>
     *     By default, block-level elements begin on new lines.
     *   </dd>
     *   <dt>Content model</dt>
     *   <dd>
     *     Generally, block-level elements may contain inline elements and other block-level elements. Inherent in
     *     this structural distinction is the idea that block elements create "larger" structures than inline elements.
     *   </dd>
     * </dl>
     * <p>
     *   Quoted from <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/Block-level_elements">Mozilla Developer
     *   Network, "Block-level Elements"</a>.
     * </p>
     *
     * <p>
     *   See also <a href="http://www.w3schools.com/html/html_blocks.asp">HTML Tutorial, section "HTML Block
     *   Elements"</a>.
     * </p>
     *
     * @return Whether the given {@code node} is one of the "block elements" by the HTML standard
     */
    private static boolean
    isBlockElement(Node node) {

        if (node.getNodeType() != Node.ELEMENT_NODE) return false;
        Element e = (Element) node;

        return Html2Txt.ALL_BLOCK_ELEMENTS.containsKey(e.getTagName());
    }

    private static final BlockElementFormatter
    HR_FORMATTER = new BlockElementFormatter() {

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) {
            output.consume(StringUtil.repeat(leftMarginWidth, ' ') + StringUtil.repeat(measure, '-'));
        }
    };

    private static final BlockElementFormatter
    OL_FORMATTER = new BlockElementFormatter() {

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException {

            // Determine the OL type.
            final NumberingType numberingType;
            {
                Attr s = element.getAttributeNode("type");
                numberingType = (
                    "a".equals(s) ? NumberingType.LOWERCASE_LETTERS :
                    "A".equals(s) ? NumberingType.UPPERCASE_LETTERS :
                    "i".equals(s) ? NumberingType.LOWERCASE_ROMAN_NUMERALS :
                    "I".equals(s) ? NumberingType.UPPERCASE_ROMAN_LITERALS :
                    NumberingType.ARABIC_DIGITS
                );
            }

            // Compute the index to start from.
            final int start;
            {
                int tmp;
                try {
                    tmp = Integer.parseInt(element.getAttribute("start"));
                } catch (Exception e) {
                    tmp = 1;
                }
                start = tmp;
            }

            html2Txt.formatBlocks(
                leftMarginWidth + 5,
                new Bulleting() {
                    int nextValue = start;
                    @Override public String next() { return numberingType.toString(this.nextValue++) + "."; }
                },
                measure - 5,
                XmlUtil.iterable(element.getChildNodes()),
                output
            );
        }
    };

    private static final BlockElementFormatter
    PRE_FORMATTER = new BlockElementFormatter() {

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException {

            StringBuilder sb = new StringBuilder();
            for (Node n : XmlUtil.iterable(element.getChildNodes())) {
                short nodeType = n.getNodeType();

                if (nodeType == Node.TEXT_NODE) {
                    sb.append(n.getTextContent());
                } else
                if (nodeType == Node.ELEMENT_NODE) {
                    Element e = (Element) n;

                    InlineElementFormatter ief = Html2Txt.ALL_INLINE_ELEMENTS.get(e.getTagName());
                    if (ief == null) {
                        html2Txt.htmlErrorHandler.error(
                            new HtmlException(n, "Unexpected element \"" + XmlUtil.toString(e) + "\" in <pre>")
                        );
                    } else {
                        ief.format(html2Txt, e, sb);
                    }
                } else
                {
                    html2Txt.htmlErrorHandler.error(new HtmlException(n, "Unexpected node in <pre>"));
                }
            }

            Producer<CharSequence> lp = LineUtil.lineProducer(sb);
            for (boolean first = true;; first = false) {

                CharSequence line = lp.produce();
                if (line == null) break;

                // Ignore leading empty lines.
                if (first && line.length() == 0) continue;

                if (first) {
                    String bullet = bulleting.next();
                    if (bullet.length() + 1 > leftMarginWidth) {
                        line = bullet + ' ' + line;
                    } else {
                        line = StringUtil.repeat(leftMarginWidth - bullet.length() - 1, ' ') + bullet + ' ' + line;
                    }
                }

                output.consume(line);
            }
        }
    };

    private static final BlockElementFormatter
    UL_FORMATTER = new BlockElementFormatter() {

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException {

            html2Txt.formatBlocks(
                leftMarginWidth + 3,
                new Bulleting() { @Override public String next() { return "*"; } },
                measure - 3,
                XmlUtil.iterable(element.getChildNodes()),
                output
            );
        }
    };

    private static
    class HeadingBlockElementFormatter implements BlockElementFormatter {

        private boolean          emptyLineAbove, emptyLineBelow;
        @Nullable private String prefix, suffix;
        private int              underline = -1;

        public
        HeadingBlockElementFormatter(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public
        HeadingBlockElementFormatter(boolean emptyLineAbove, char underline, boolean emptyLineBelow) {
            this.emptyLineAbove = emptyLineAbove;
            this.underline      = underline;
            this.emptyLineBelow = emptyLineBelow;
        }

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException {

            String text = html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes()));
            if (this.prefix != null) text = this.prefix.concat(text);
            if (this.suffix != null) text = text.concat(this.suffix);

            if (this.emptyLineAbove) output.consume("");
            output.consume(text);
            if (this.underline != -1) output.consume(StringUtil.repeat(text.length(), (char) this.underline));
            if (this.emptyLineBelow) output.consume("");
        }
    }

    /**
     * Simply appends the element's formatted content, a.k.a. "the tag is ignored".
     */
    private static final BlockElementFormatter
    IGNORE_BLOCK_ELEMENT_FORMATTER = new IndentingBlockElementFormatter(0);

    /**
     * Does <i>nothing</i>, i.e. even its contents is ignored.
     */
    private static final BlockElementFormatter NOP_BLOCK_ELEMENT_FORMATTER = new BlockElementFormatter() {

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) {
            ;
        }
    };

    private static final BlockElementFormatter
    NYI_BLOCK_ELEMENT_FORMATTER = new BlockElementFormatter() {

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException {

            html2Txt.htmlErrorHandler.warning(
                new HtmlException(
                    element,
                    "HTML block element \"<" + element.getNodeName() + ">\" is not yet implemented and thus ignored"
                )
            );

            Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER.format(
                html2Txt,
                leftMarginWidth,
                bulleting,
                measure,
                element,
                output
            );
        }
    };

    private static
    class IndentingBlockElementFormatter implements BlockElementFormatter {

        private final int indentation;

        public IndentingBlockElementFormatter(int indentation) { this.indentation = indentation; }

        @Override public void
        format(
            Html2Txt                       html2Txt,
            int                            leftMarginWidth,
            Bulleting                      bulleting,
            int                            measure,
            Element                        element,
            Consumer<? super CharSequence> output
        ) throws HtmlException {

            html2Txt.formatBlocks(
                leftMarginWidth + this.indentation,
                bulleting,
                measure - this.indentation,
                XmlUtil.iterable(element.getChildNodes()),
                output
            );
        }
    }

    /**
     * Defines the strategies for formatting HTML block elements.
     * <p>
     *   To see the HTML block elements and how they are formatted, click the word "{@code ALL_BLOCK_ELEMENTS}"
     *   (right above). The right hand side of the mapping means:
     *   <dl>
     *     <dt>{@link Html2Txt#NYI_BLOCK_ELEMENT_FORMATTER NYI_BLOCK_ELEMENT_FORMATTER}</dt>
     *     <dd>
     *       Issues a "Not yet implemented" warning.
     *     </dd>
     *     <dt>{@link Html2Txt#IGNORE_BLOCK_ELEMENT_FORMATTER IGNORE_BLOCK_ELEMENT_FORMATTER}</dt>
     *     <dd>
     *       The element is simply replaced with its content (a.k.a. "the element is ignored").
     *     </dd>
     *     <dt>{@code new} {@link IndentingBlockElementFormatter IndentingBlockElementFormatter(<var>N</var>)}</dt>
     *     <dd>
     *       The block is formatted <var>N</var> characters indented, relative to the enclosing block.
     *     </dd>
     *     <dt>(Other)</dt>
     *     <dd>
     *       This HTML block element is formatted specially; see the respective field documentation on this page (e.g.
     *       {@link #OL_FORMATTER}).
     *     </dd>
     *   </dl>
     * </p>
     */
    private static final Map<String, BlockElementFormatter>
    ALL_BLOCK_ELEMENTS = Collections.unmodifiableMap(CollectionUtil.<String, BlockElementFormatter>map(
        "address",    new IndentingBlockElementFormatter(2),
        "article",    Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "aside",      Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "audio",      Html2Txt.NYI_BLOCK_ELEMENT_FORMATTER,
        "blockquote", new IndentingBlockElementFormatter(2),
        "canvas",     Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "dd",         new IndentingBlockElementFormatter(4),
        "div",        Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "dl",         new IndentingBlockElementFormatter(2),
        "dt",         Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "fieldset",   Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "figcaption", Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "figure",     Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "footer",     Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "form",       Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "h1",         new HeadingBlockElementFormatter(true, '*', true),
        "h2",         new HeadingBlockElementFormatter(true, '=', true),
        "h3",         new HeadingBlockElementFormatter(true, '-', true),
        "h4",         new HeadingBlockElementFormatter("=== ", " ==="),
        "h5",         new HeadingBlockElementFormatter("== ",  " =="),
        "h6",         new HeadingBlockElementFormatter("= ",   " ="),
        "header",     Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "hgroup",     Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "hr",         Html2Txt.HR_FORMATTER,
        "main",       Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "nav",        Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "noscript",   Html2Txt.NOP_BLOCK_ELEMENT_FORMATTER,
        "ol",         Html2Txt.OL_FORMATTER,
        "output",     Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "p",          Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "pre",        Html2Txt.PRE_FORMATTER,
        "section",    Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "table",      Html2Txt.NYI_BLOCK_ELEMENT_FORMATTER,
        "tfoot",      Html2Txt.IGNORE_BLOCK_ELEMENT_FORMATTER,
        "ul",         Html2Txt.UL_FORMATTER,
        "video",      Html2Txt.NYI_BLOCK_ELEMENT_FORMATTER
    ));

    /**
     * HTML (Hypertext Markup Language) elements are usually "inline" elements or "block-level" elements.
     * <p>
     *   An inline element occupies only the space bounded by the tags that define the inline element.
     * </p>
     *
     * <p>
     *   Quoted from <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/Inline_elemente">Mozilla Developer
     *   Network, "Inline Elements"</a>.
     * </p>
     * <p>
     *   See <a href="http://www.w3schools.com/html/html_blocks.asp">HTML Tutorial, section "HTML Block Elements"</a>.
     * </p>
     */
    private static boolean
    isInlineElement(Node node) {

        if (node.getNodeType() != Node.ELEMENT_NODE) return false;
        Element e = (Element) node;

        return Html2Txt.ALL_INLINE_ELEMENTS.containsKey(e.getTagName());
    }

    private static final InlineElementFormatter
    A_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException {
            String name = element.getAttribute("name");
            String href = element.getAttribute("href");
            if (!name.isEmpty() && href.isEmpty()) {
                if (!html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes())).isEmpty()) {
                    html2Txt.htmlErrorHandler.warning(
                        new HtmlException(element, "'<a name=\"...\" />' tag should have content")
                    );
                }

                // '<a name="..." />' renders as "".
                ;
            } else
            if (!href.isEmpty() && name.isEmpty()) {
                result.append(html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes())));
                result.append(" (see ").append(href).append(')');
            } else
            {
                html2Txt.htmlErrorHandler.warning(
                    new HtmlException(element, "\"<a>\" tag has an unexpected combination of attributes")
                );
            }
        }
    };

    private static final InlineElementFormatter
    ABBR_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException {

            result.append(html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes())));

            String title = element.getAttribute("title");
            if (!title.isEmpty()) {
                result.append(" (\"").append(title).append("\")");
            }
        }
    };

    private static final InlineElementFormatter
    BR_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException {

            if (element.hasChildNodes()) {
                html2Txt.htmlErrorHandler.warning(
                    new HtmlException(element, "\"<br>\" tag should not have subelements nor contain text")
                );
            }
            result.append('\n');
        }
    };

    private static final InlineElementFormatter
    INPUT_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) {

            String type = element.getAttribute("type");
            if ("checkbox".equals(type)) {
                result.append("checked".equals(element.getAttribute("checked")) ? "[x]" : "[ ]");
            } else
            if ("hidden".equals(type)) {
                ;
            } else
            if ("password".equals(type)) {
                result.append("[******]");
            } else
            if ("radio".equals(type)) {
                result.append("checked".equals(element.getAttribute("checked")) ? "(o)" : "( )");
            } else
            if ("range".equals(type)) {
                result.append("[RANGE-INPUT]");
            } else
            if ("submit".equals(type)) {
                String label = element.getAttribute("value");
                if (label.isEmpty()) label = "Submit";
                result.append("[ ").append(label).append(" ]");
            } else
            if ("text".equals(type) || "".equals(type)) {
                result.append('[').append(element.getAttribute("value")).append(']');
            } else
            {
                result.append('[').append(type.toUpperCase()).append("-INPUT]");
            }
        }
    };

    private static final InlineElementFormatter
    Q_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException {

            final String cite = element.getAttribute("cite");

            result.append('"');
            result.append(html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes())));
            result.append("\" (").append(cite).append(')');
        }
    };

    /**
     * Simply appends the element's formatted content, a.k.a. "ignoring a tag".
     */
    private static final InlineElementFormatter
    IGNORE_INLINE_ELEMENT_FORMATTER = new SimpleInlineElementFormatter("", "");

    /**
     * Concatenates the <var>prefix</var>, the element's formatted content, and the <var>suffix</var>.
     */
    static
    class SimpleInlineElementFormatter implements InlineElementFormatter {

        private final String prefix, suffix;

        public
        SimpleInlineElementFormatter(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException {

            result.append(this.prefix);
            result.append(html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes())));
            result.append(this.suffix);
        }
    }

    private static final InlineElementFormatter
    NYI_INLINE_ELEMENT_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Html2Txt html2Txt, Element element, StringBuilder result) throws HtmlException {

            html2Txt.htmlErrorHandler.warning(
                new HtmlException(
                    element,
                    "HTML inline element \"<" + element.getNodeName() + ">\" is not yet implemented and thus ignored"
                )
            );

            result.append(html2Txt.getBlock(XmlUtil.iterable(element.getChildNodes())));
        }
    };

    /**
     * Defines the strategies for formatting HTML inline elements.
     * <p>
     *   To see the HTML inline elements and how they are formatted, click the word "{@code ALL_INLINE_ELEMENTS}"
     *   (right above). The right hand side of the mapping means:
     *   <dl>
     *     <dt>{@link #NYI_INLINE_ELEMENT_FORMATTER}</dt>
     *     <dd>
     *       Issues a "Not yet implemented" warning.
     *     </dd>
     *     <dt>{@link #IGNORE_INLINE_ELEMENT_FORMATTER}</dt>
     *     <dd>
     *       The element is simply replaced with its content (a.k.a. "the element is ignored").
     *     </dd>
     *     <dt>{@code new} {@link Html2Txt.SimpleInlineElementFormatter SimpleInlineElementFormatter("foo", "bar")}</dt>
     *     <dd>
     *       The element is replaced with "{@code foo}", the element content, and "{@code bar}".
     *     </dd>
     *     <dt>(Other)</dt>
     *     <dd>
     *       This HTML inline element is formatted specially; see the respective field documentation on this page (e.g.
     *       {@link #A_FORMATTER}).
     *     </dd>
     *   </dl>
     * </p>
     */
    protected static final Map<String, InlineElementFormatter>
    ALL_INLINE_ELEMENTS = Collections.unmodifiableMap(CollectionUtil.<String, InlineElementFormatter>map(
        "a",        Html2Txt.A_FORMATTER,
        "abbr",     Html2Txt.ABBR_FORMATTER,
        "acronym",  Html2Txt.ABBR_FORMATTER,
        "b",        new SimpleInlineElementFormatter("*", "*"),
        "bdo",      Html2Txt.NYI_INLINE_ELEMENT_FORMATTER,
        "big",      Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "br",       Html2Txt.BR_FORMATTER,
        "button",   new SimpleInlineElementFormatter("[ ", " ]"),
        "cite",     Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "code",     Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "dfn",      Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "em",       new SimpleInlineElementFormatter("<", ">"),
        "i",        new SimpleInlineElementFormatter("<", ">"),
        "img",      Html2Txt.NYI_INLINE_ELEMENT_FORMATTER,
        "input",    Html2Txt.INPUT_FORMATTER,
        "kbd",      new SimpleInlineElementFormatter("[ ", " ]"),
        "label",    Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "map",      Html2Txt.NYI_INLINE_ELEMENT_FORMATTER,
        "object",   Html2Txt.NYI_INLINE_ELEMENT_FORMATTER,
        "q",        Html2Txt.Q_FORMATTER,
        "samp",     Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "script",   Html2Txt.NYI_INLINE_ELEMENT_FORMATTER,
        "select",   new SimpleInlineElementFormatter("[ ", " ]"),
        "small",    Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "span",     Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "strong",   new SimpleInlineElementFormatter("*", "*"),
        "sub",      Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "sup",      new SimpleInlineElementFormatter("^", ""),
        "textarea", new SimpleInlineElementFormatter("[ ", " ]"),
        "tt",       Html2Txt.IGNORE_INLINE_ELEMENT_FORMATTER,
        "u",        new SimpleInlineElementFormatter("_", "_"),
        "var",      new SimpleInlineElementFormatter("<", ">")
    ));
}
