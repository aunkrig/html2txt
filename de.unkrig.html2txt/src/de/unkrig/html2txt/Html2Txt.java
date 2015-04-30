
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
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

    private int pageLeftMargin  /*= 0*/;
    private int pageRightMargin /*= 0*/;
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

    public
    interface InlineElementFormatter {
        void format(Element e, StringBuilder result) throws HtmlException;
    }

    /**
     * Sets a custom {@link HtmlErrorHandler} on this object. The default handler is {@link
     * #SIMPLE_HTML_ERROR_HANDLER}.
     */
    public void
    setErrorHandler(HtmlErrorHandler htmlErrorHandler) {
        this.htmlErrorHandler = htmlErrorHandler;
    }

    public void
    setPageWidth(int pageWidth) { this.pageWidth = pageWidth; }

    public void
    setPageLeftMargin(int pageLeftMargin) { this.pageLeftMargin = pageLeftMargin; }

    public void
    setPageRightMargin(int pageRightMargin) { this.pageRightMargin = pageRightMargin; }

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
    html2txt(Document document, Consumer<String> lc) throws HtmlException {

        // Some block tags render vertical space, which we want to compress.
        lc = LineUtil.compressEmptyLines(lc);

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
                        this.pageLeftMargin,
                        this.pageWidth - this.pageLeftMargin - this.pageRightMargin,
                        XmlUtil.iterable(bodyElement.getChildNodes()),
                        lc
                    );
                }
            }

            return;
        }

        // Otherwise, assume that the document poses an HTML *fragment*, and the top level nodes ar *blocks*.
        this.formatBlocks(
            this.pageLeftMargin,
            this.pageWidth - this.pageLeftMargin - this.pageRightMargin,
            Collections.singletonList(documentElement),
            lc
        );
    }

    private <N extends Node> void
    formatBlocks(int leftMarginWidth, int measure, Iterable<N> nodes, Consumer<String> lc) throws HtmlException {

        List<Node> inlineNodes = new ArrayList<Node>();
        for (Node n : nodes) {
            if (n.getNodeType() == Node.TEXT_NODE) {
                inlineNodes.add(n);
            } else
            if (this.isHtmlInlineElement(n)) {
                inlineNodes.add(n);
            } else
            if (Html2Txt.isHtmlBlockElement(n)) {
                if (!inlineNodes.isEmpty()) {
                    this.formatBlock(leftMarginWidth, measure, inlineNodes, lc);
                    inlineNodes.clear();
                }
                lc.consume("");
                this.formatBlockElement(leftMarginWidth, measure, (Element) n, lc);
            } else
            {
                this.htmlErrorHandler.error(
                    new HtmlException(n, "Unexpected node \"" + XmlUtil.toString(n) + "\" in <body>")
                );
            }
        }
        if (!inlineNodes.isEmpty()) {
            this.formatBlock(leftMarginWidth, measure, inlineNodes, lc);
            inlineNodes.clear();
        }

    }

    private void
    formatBlockElement(int leftMargin, int measure, Element element, Consumer<String> lc) throws HtmlException {

        String tagName = element.getTagName();
        if ("p".equals(tagName)) {
            this.formatBlock(leftMargin, measure, XmlUtil.iterable(element.getChildNodes()), lc);
        } else
        if ("h2".equals(tagName)) {
            String text = this.getBlock(XmlUtil.iterable(element.getChildNodes()));
            lc.consume(text);
            lc.consume(StringUtil.repeat(text.length(), '='));
            lc.consume("");
        } else
        if ("h3".equals(tagName)) {
            String text = this.getBlock(XmlUtil.iterable(element.getChildNodes()));
            lc.consume(text);
            lc.consume(StringUtil.repeat(text.length(), '-'));
            lc.consume("");
        } else
        if ("dl".equals(tagName)) {
            for (Node dle : XmlUtil.iterable(element.getChildNodes())) {

                if (
                    dle.getNodeType() == Node.TEXT_NODE
                    && dle.getTextContent().trim().length() == 0
                ) continue;

                if (dle.getNodeType() != Node.ELEMENT_NODE) {
                    this.htmlErrorHandler.error(new HtmlException(element, "Unexpected node in <dl>"));
                    continue;
                }
                Element dlee = (Element) dle;

                String dleTagName = dlee.getTagName();
                if ("dt".equals(dleTagName)) {
                    this.formatBlocks(leftMargin + 2, measure - 2, XmlUtil.iterable(dlee.getChildNodes()), lc);
                } else
                if ("dd".equals(dleTagName)) {
                    this.formatBlocks(leftMargin + 6, measure - 6, XmlUtil.iterable(dlee.getChildNodes()), lc);
                } else
                {
                    this.htmlErrorHandler.error(new HtmlException(element, "Unexpected element in <dl>"));
                }
            }
        } else
        {
            this.htmlErrorHandler.error(new HtmlException(element, "Not a recognized block element"));
        }
    }

    private void
    formatBlock(int leftMargin, int measure, Iterable<Node> nodes, Consumer<String> lc) throws HtmlException {

        this.formatBlock(leftMargin, measure, this.getBlock(nodes), lc, nodes.iterator().next());
    }

    /**
     * The given <var>text</var> is word-wrapped such that each output line begins with <var>leftMargin</var> spaces,
     * followed by up to <var>measure</var> characters. If the <var>text</var> contains very long words, then some of
     * the output lines may be longer than "<var>leftMarginWidth</var> + <var>measure</var>".
     * <p>
     *   Newline characters ({@code '\n'}) appear as line breaks in the output.
     * </p>
     * <p>
     *   The output lines are fed to the <var>lc</var>.
     * </p>
     *
     * @param ref Is used iff an {@link HtmlException} is thrown ({@link HtmlException}s have a reference to the
     *            "offending" node)
     */
    private void
    formatBlock(int leftMargin, int measure, String text, Consumer<String> lc, Node ref) throws HtmlException {

        text = text.trim();
        if (text.length() == 0) return;

        for (int nlidx = text.indexOf('\n'); nlidx != -1; nlidx = text.indexOf('\n')) {
            this.formatBlock(leftMargin, measure, text.substring(0, nlidx), lc, ref);
            text = text.substring(nlidx + 1);
        }

        if (measure < 5) {
            this.htmlErrorHandler.error(new HtmlException(ref, "Page too narrow"));
            return;
        }

        while (text.length() > measure) {
            int idx1 = text.lastIndexOf(' ', measure);
            if (idx1 == -1) break;
            int idx2;
            for (idx2 = idx1; idx2 > 0 && text.charAt(idx2 - 1) == ' '; idx2--);
            if (idx2 == 0) break;
            lc.consume(StringUtil.repeat(leftMargin, ' ') + text.substring(0, idx2));
            text = text.substring(idx1 + 1);
        }

        lc.consume(StringUtil.repeat(leftMargin, ' ') + text);
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

                InlineElementFormatter ief = this.ALL_HTML_INLINE_ELEMENTS.get(e.getTagName());
                if (ief == null) {
                    this.htmlErrorHandler.error(
                        new HtmlException(n, "Unexpected element \"" + XmlUtil.toString(e) + "\" in block")
                    );
                } else {
                    ief.format(e, sb);
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
    isHtmlBlockElement(Node node) {

        if (node.getNodeType() != Node.ELEMENT_NODE) return false;
        Element e = (Element) node;

        return Html2Txt.ALL_HTML_BLOCK_ELEMENTS.contains(e.getTagName());
    }
    private static final Set<String> ALL_HTML_BLOCK_ELEMENTS = new HashSet<String>(Arrays.asList(
        "address", "article", "aside", "audio", "blockquote", "canvas", "dd", "div", "dl", "fieldset", "figcaption",
        "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "main", "nav",
        "noscript", "ol", "output", "p", "pre", "section", "table", "tfoot", "ul", "video"
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
    private boolean
    isHtmlInlineElement(Node node) {

        if (node.getNodeType() != Node.ELEMENT_NODE) return false;
        Element e = (Element) node;

        return this.ALL_HTML_INLINE_ELEMENTS.containsKey(e.getTagName());
    }

    private final InlineElementFormatter
    A_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Element e, StringBuilder result) throws HtmlException {
            String name = e.getAttribute("name");
            String href = e.getAttribute("href");
            if (!name.isEmpty() && href.isEmpty()) {
                if (!Html2Txt.this.getBlock(XmlUtil.iterable(e.getChildNodes())).isEmpty()) {
                    Html2Txt.this.htmlErrorHandler.warning(
                        new HtmlException(e, "'<a name=\"...\" />' tag should have content")
                    );
                }

                // '<a name="..." />' renders as "".
                ;
            } else
            if (!href.isEmpty() && name.isEmpty()) {
                result.append(Html2Txt.this.getBlock(XmlUtil.iterable(e.getChildNodes())));
                result.append(" (see ").append(href).append(')');
            } else
            {
                Html2Txt.this.htmlErrorHandler.warning(
                    new HtmlException(e, "\"<a>\" tag has an unexpected combination of attributes")
                );
            }
        }
    };

    private final InlineElementFormatter
    ABBR_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Element e, StringBuilder result) throws HtmlException {

            result.append(Html2Txt.this.getBlock(XmlUtil.iterable(e.getChildNodes())));

            String title = e.getAttribute("title");
            if (!title.isEmpty()) {
                result.append(" (\"").append(title).append("\")");
            }
        }
    };

    private final InlineElementFormatter
    BR_FORMATTER = new InlineElementFormatter() {

        @Override
        public void format(Element e, StringBuilder result) throws HtmlException {

            if (e.hasChildNodes()) {
                Html2Txt.this.htmlErrorHandler.warning(
                    new HtmlException(e, "\"<br>\" tag should not have subelements nor contain text")
                );
            }
            result.append('\n');
        }
    };

    private final InlineElementFormatter
    INPUT_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Element e, StringBuilder result) {

            String type = e.getAttribute("type");
            if ("checkbox".equals(type)) {
                result.append("checked".equals(e.getAttribute("checked")) ? "[x]" : "[ ]");
            } else
            if ("hidden".equals(type)) {
                ;
            } else
            if ("password".equals(type)) {
                result.append("[******]");
            } else
            if ("radio".equals(type)) {
                result.append("checked".equals(e.getAttribute("checked")) ? "(o)" : "( )");
            } else
            if ("range".equals(type)) {
                result.append("[RANGE-INPUT]");
            } else
            if ("submit".equals(type)) {
                String label = e.getAttribute("value");
                if (label.isEmpty()) label = "Submit";
                result.append("[ ").append(label).append(" ]");
            } else
            if ("text".equals(type) || "".equals(type)) {
                result.append('[').append(e.getAttribute("value")).append(']');
            } else
            {
                result.append('[').append(type.toUpperCase()).append("-INPUT]");
            }
        }
    };

    private final InlineElementFormatter
    Q_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Element e, StringBuilder result) throws HtmlException {

            final String cite = e.getAttribute("cite");

            result.append('"');
            result.append(Html2Txt.this.getBlock(XmlUtil.iterable(e.getChildNodes())));
            result.append("\" (").append(cite).append(')');
        }
    };

    /**
     * Simply appends the element's formatted content, a.k.a. "ignoring a tag".
     */
    private final InlineElementFormatter NOP_FORMATTER = new FramingFormatter("", "");

    /**
     * Appends the <var>prefix</var>, the element's formatted content, and finally the <var>suffix</var>.
     */
    class FramingFormatter implements InlineElementFormatter {

        private final String prefix, suffix;

        public
        FramingFormatter(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override public void
        format(Element e, StringBuilder result) throws HtmlException {

            result.append(this.prefix);
            result.append(Html2Txt.this.getBlock(XmlUtil.iterable(e.getChildNodes())));
            result.append(this.suffix);
        }
    }

    private final InlineElementFormatter
    NYI_FORMATTER = new InlineElementFormatter() {

        @Override public void
        format(Element e, StringBuilder result) throws HtmlException {

            Html2Txt.this.htmlErrorHandler.warning(
                new HtmlException(e, "\"<" + e.getNodeName() + ">\" is not yet implemented and thus ignored")
            );

            result.append(Html2Txt.this.getBlock(XmlUtil.iterable(e.getChildNodes())));
        }
    };

    private final Map<String, InlineElementFormatter>
    ALL_HTML_INLINE_ELEMENTS = CollectionUtil.<String, InlineElementFormatter>map(
        "a",        this.A_FORMATTER,
        "abbr",     this.ABBR_FORMATTER,
        "acronym",  this.ABBR_FORMATTER,
        "b",        new FramingFormatter("*", "*"),
        "bdo",      this.NYI_FORMATTER,
        "big",      this.NOP_FORMATTER,
        "br",       this.BR_FORMATTER,
        "button",   new FramingFormatter("[ ", " ]"),
        "cite",     this.NOP_FORMATTER,
        "code",     this.NOP_FORMATTER,
        "dfn",      this.NOP_FORMATTER,
        "em",       new FramingFormatter("<", ">"),
        "i",        new FramingFormatter("<", ">"),
        "img",      this.NYI_FORMATTER,
        "input",    this.INPUT_FORMATTER,
        "kbd",      new FramingFormatter("[ ", " ]"),
        "label",    this.NOP_FORMATTER,
        "map",      this.NYI_FORMATTER,
        "object",   this.NYI_FORMATTER,
        "q",        this.Q_FORMATTER,
        "samp",     this.NOP_FORMATTER,
        "script",   this.NYI_FORMATTER,
        "select",   new FramingFormatter("[ ", " ]"),
        "small",    this.NOP_FORMATTER,
        "span",     this.NOP_FORMATTER,
        "strong",   new FramingFormatter("*", "*"),
        "sub",      this.NOP_FORMATTER,
        "sup",      new FramingFormatter("^", ""),
        "textarea", new FramingFormatter("[ ", " ]"),
        "tt",       this.NOP_FORMATTER,
        "u",        new FramingFormatter("_", "_"),
        "var",      new FramingFormatter("<", ">")
    );
}
