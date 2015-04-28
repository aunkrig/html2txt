
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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

/**
 * A command
 */
public
class Html2Txt {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /** All methods of theis {@link ErrorHandler} throw the {@link SAXException} they recieve. */
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
     * Sets a custom {@link HtmlErrorHandler} on this object. The default handler is {@link
     * #SIMPLE_HTML_ERROR_HANDLER}.
     */
    public void
    setErrorHandler(HtmlErrorHandler htmlErrorHandler) {
        this.htmlErrorHandler = htmlErrorHandler;
    }

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

        Node body = this.getElementByTagName(document, "body");

        this.formatBlocks(0, 80, Html2Txt.getChildNodes(body), lc);
    }

    private void
    formatBlocks(int leftMargin, int pageWidth, Iterable<Node> nodes, Consumer<String> lc) throws HtmlException {

        List<Node> inlineNodes = new ArrayList<Node>();
        for (Node n : nodes) {
            if (Html2Txt.isInlineNode(n)) {
                inlineNodes.add(n);
            } else
            if (Html2Txt.isBlockNode(n)) {
                if (!inlineNodes.isEmpty()) {
                    this.formatBlock(leftMargin, pageWidth, inlineNodes, lc);
                    inlineNodes.clear();
                }
                lc.consume("");
                this.formatBlockNode(leftMargin, pageWidth, n, lc);
            } else
            {
                this.htmlErrorHandler.error(new HtmlException(n, "Unexpected node in <body>"));
            }
        }
        if (!inlineNodes.isEmpty()) {
            this.formatBlock(leftMargin, pageWidth, inlineNodes, lc);
            inlineNodes.clear();
        }

    }

    private void
    formatBlockNode(int leftMargin, int pageWidth, Node n, Consumer<String> lc) throws HtmlException {

        if (n.getNodeType() != Node.ELEMENT_NODE) {
            this.htmlErrorHandler.error(new HtmlException(n, "Node is not an element"));
            return;
        }
        Element e = (Element) n;

        String tagName = e.getTagName();
        if ("p".equals(tagName)) {
            this.formatBlock(leftMargin, pageWidth, Html2Txt.getChildNodes(n), lc);
        } else
        if ("h2".equals(tagName)) {
            String text = this.getBlock(Html2Txt.getChildNodes(n));
            lc.consume(text);
            lc.consume(StringUtil.repeat(text.length(), '='));
            lc.consume("");
        } else
        if ("h3".equals(tagName)) {
            String text = this.getBlock(Html2Txt.getChildNodes(n));
            lc.consume(text);
            lc.consume(StringUtil.repeat(text.length(), '-'));
            lc.consume("");
        } else
        if ("dl".equals(tagName)) {
            for (Node dle : Html2Txt.getChildNodes(e)) {

                if (
                    dle.getNodeType() == Node.TEXT_NODE
                    && dle.getTextContent().trim().length() == 0
                ) continue;

                if (dle.getNodeType() != Node.ELEMENT_NODE) {
                    this.htmlErrorHandler.error(new HtmlException(n, "Unexpected node in <dl>"));
                    continue;
                }
                Element dlee = (Element) dle;

                String dleTagName = dlee.getTagName();
                if ("dt".equals(dleTagName)) {
                    this.formatBlocks(leftMargin + 2, pageWidth, Html2Txt.getChildNodes(dlee), lc);
                } else
                if ("dd".equals(dleTagName)) {
                    this.formatBlocks(leftMargin + 6, pageWidth, Html2Txt.getChildNodes(dlee), lc);
                } else
                {
                    this.htmlErrorHandler.error(new HtmlException(n, "Unexpected element in <dl>"));
                }
            }
        } else
        {
            this.htmlErrorHandler.error(new HtmlException(n, "Not a recognized block element"));
        }
    }

    private void
    formatBlock(int leftMargin, int pageWidth, Iterable<Node> nodes, Consumer<String> lc) throws HtmlException {

        String block = this.getBlock(nodes).trim();
        if (block.length() == 0) return;

        int maxChars = pageWidth - leftMargin;
        if (maxChars < 5) {
            this.htmlErrorHandler.error(new HtmlException(nodes.iterator().next(), "Page too narrow"));
            return;
        }
        while (block.length() > maxChars) {
            int idx1 = block.lastIndexOf(' ', maxChars);
            if (idx1 == -1) break;
            int idx2;
            for (idx2 = idx1; idx2 > 0 && block.charAt(idx2 - 1) == ' '; idx2--);
            if (idx2 == 0) break;
            lc.consume(StringUtil.repeat(leftMargin, ' ') + block.substring(0, idx2));
            block = block.substring(idx1 + 1);
        }

        lc.consume(StringUtil.repeat(leftMargin, ' ') + block);
    }

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

                String tagName = e.getTagName();
                if ("var".equals(tagName)) {
                    sb.append('<');
                    sb.append(this.getBlock(Html2Txt.getChildNodes(e)));
                    sb.append('>');
                } else
                if ("code".equals(tagName)) {
                    sb.append(this.getBlock(Html2Txt.getChildNodes(e)));
                } else
                {
                    this.htmlErrorHandler.error(new HtmlException(n, "Unexpected element in block"));
                }
            } else
            {
                this.htmlErrorHandler.error(new HtmlException(n, "Unexpected node in block"));
            }
        }
        return sb.toString();
    }

    private static boolean
    isBlockNode(Node n) {

        if (n.getNodeType() != Node.ELEMENT_NODE) return false;
        Element e = (Element) n;

        String tagName = e.getTagName();
        return (
            "p".equals(tagName)
            || "dl".equals(tagName)
            || "h2".equals(tagName)
            || "h3".equals(tagName)
        );
    }

    private static boolean
    isInlineNode(Node n) {
        short nodeType = n.getNodeType();
        if (nodeType == Node.TEXT_NODE) return true;
        if (nodeType == Node.ELEMENT_NODE) {
            Element e = (Element) n;
            if (
                "var".equals(e.getTagName())
                || "code".equals(e.getTagName())
            ) return true;
        }
        return false;
    }

    private Node
    getElementByTagName(final Document document, String tagName) throws HtmlException {
        NodeList nl = document.getElementsByTagName(tagName);
        if (nl.getLength() == 0) {
            HtmlException he = new HtmlException(document, "Unexpected node in block");
            this.htmlErrorHandler.fatalError(he);
            throw he;
        }
        if (nl.getLength() > 1) {
            this.htmlErrorHandler.error(new HtmlException(
                nl.item(1),
                "Only one \"<" + tagName + ">\" element allowed"
            ));
        }
        return nl.item(0);
    }

    private static Iterable<Node>
    getChildNodes(Node node) {

        final NodeList nl = node.getChildNodes();
        return new Iterable<Node>() {

            @Override public Iterator<Node>
            iterator() {
                return new Iterator<Node>() {

                    private int idx;

                    @Override public Node
                    next() {
                        if (this.idx >= nl.getLength()) throw new NoSuchElementException();
                        return nl.item(this.idx++);
                    }

                    @Override public boolean
                    hasNext() { return this.idx < nl.getLength(); }

                    @Override public void
                    remove() { throw new UnsupportedOperationException("remove"); }
                };
            }
        };
    }
}
