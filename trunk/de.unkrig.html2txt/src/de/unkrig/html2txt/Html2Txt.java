
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

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.io.LineUtil;
import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;

/**
 * A command
 */
public final
class Html2Txt {

    private Html2Txt() {}

    /**
     * Reads, scans and parses the HTML document in the {@code inputFile}, generates a plain text document, and
     * writes it to the {@code output}.
     */
    public static void
    html2txt(File inputFile, Writer output) throws Exception {

        final Document document;
        {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
            document.getDocumentElement().normalize();
        }

//        TransformerFactory.newInstance().newTransformer().transform(
//            new DOMSource(document),
//            new StreamResult(new File("output.xml"))
//        );

        PrintWriter pw = output instanceof PrintWriter ? (PrintWriter) output : new PrintWriter(output);

        Html2Txt.html2txt(document, LineUtil.lineConsumer(pw));
    }

    /**
     * Reads, scans and parses the HTML document in the {@code inputFile}, generates a plain text document, and
     * writes it to the {@code outputFile}.
     */
    public static void
    html2txt(final File inputFile, File outputFile) throws Exception {

        FileUtil.printToFile(
            outputFile,
            Charset.forName("ISO8859-1"),
            new ConsumerWhichThrows<PrintWriter, Exception>() {
                @Override public void consume(PrintWriter pw) throws Exception { Html2Txt.html2txt(inputFile, pw); }
            }
        );
    }

    private static void
    html2txt(Document document, Consumer<String> lc) {

        Node body = Html2Txt.getElementByTagName(document, "body");

        Html2Txt.formatBlocks(0, 80, Html2Txt.getChildNodes(body), lc);
    }

    private static void
    formatBlocks(int leftMargin, int pageWidth, Iterable<Node> nodes, Consumer<String> lc) {

        List<Node> inlineNodes = new ArrayList<Node>();
        for (Node n : nodes) {
            if (Html2Txt.isInlineNode(n)) {
                inlineNodes.add(n);
            } else
            if (Html2Txt.isBlockNode(n)) {
                if (!inlineNodes.isEmpty()) {
                    Html2Txt.formatBlock(leftMargin, pageWidth, inlineNodes, lc);
                    inlineNodes.clear();
                }
                lc.consume("");
                Html2Txt.formatBlockNode(leftMargin, pageWidth, n, lc);
            } else
            {
                throw new RuntimeException("Unexpected node \"" + n + "\"in <body>");
            }
        }
        if (!inlineNodes.isEmpty()) {
            Html2Txt.formatBlock(leftMargin, pageWidth, inlineNodes, lc);
            inlineNodes.clear();
        }

    }

    private static void
    formatBlockNode(int leftMargin, int pageWidth, Node n, Consumer<String> lc) {

        if (n.getNodeType() != Node.ELEMENT_NODE) {
            throw new RuntimeException("\"" + n + "\" is not an element");
        }
        Element e = (Element) n;

        String tagName = e.getTagName();
        if ("p".equals(tagName)) {
            Html2Txt.formatBlock(leftMargin, pageWidth, Html2Txt.getChildNodes(n), lc);
        } else
        if ("h2".equals(tagName)) {
            String text = Html2Txt.getBlock(Html2Txt.getChildNodes(n));
            lc.consume(text);
            lc.consume(StringUtil.repeat(text.length(), '='));
            lc.consume("");
        } else
        if ("h3".equals(tagName)) {
            String text = Html2Txt.getBlock(Html2Txt.getChildNodes(n));
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
                    throw new RuntimeException("Unexpected node \"" + dle + "\" in <dl>");
                }
                Element dlee = (Element) dle;

                String dleTagName = dlee.getTagName();
                if ("dt".equals(dleTagName)) {
                    Html2Txt.formatBlocks(leftMargin + 2, pageWidth, Html2Txt.getChildNodes(dlee), lc);
                } else
                if ("dd".equals(dleTagName)) {
                    Html2Txt.formatBlocks(leftMargin + 6, pageWidth, Html2Txt.getChildNodes(dlee), lc);
                } else
                {
                    throw new RuntimeException("Unexpected element \"" + dlee + "\" in <dl>");
                }
            }
        } else
        {
            throw new RuntimeException("\"" + n + "\" is not a recognized block element");
        }
    }

    private static void
    formatBlock(int leftMargin, int pageWidth, Iterable<Node> nodes, Consumer<String> lc) {

        String block = Html2Txt.getBlock(nodes).trim();
        if (block.length() == 0) return;

        int maxChars = pageWidth - leftMargin;
        if (maxChars < 5) throw new RuntimeException("Page too narrow");
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

    private static String
    getBlock(Iterable<Node> nodes) {
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
                    sb.append(Html2Txt.getBlock(Html2Txt.getChildNodes(e)));
                    sb.append('>');
                } else
                if ("code".equals(tagName)) {
                    sb.append(Html2Txt.getBlock(Html2Txt.getChildNodes(e)));
                } else
                {
                    throw new RuntimeException("Unexpected element \"" + e + "\" in block");
                }
            } else
            {
                throw new RuntimeException("Unexpected node \"" + n + "\" in block");
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

    private static Node
    getElementByTagName(final Document document, String tagName) {
        NodeList nl = document.getElementsByTagName(tagName);
        if (nl.getLength() == 0) {
            throw new RuntimeException("\"<" + tagName + ">\" element missing");
        }
        if (nl.getLength() > 1) {
            throw new RuntimeException("Only one \"<" + tagName + ">\" element allowed");
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
