
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.unkrig.commons.lang.StringUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.lang.protocol.ConsumerUtil;
import de.unkrig.commons.lang.protocol.Producer;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.xml.XmlUtil;
import de.unkrig.html2txt.Html2Txt.BlockElementFormatter;
import de.unkrig.html2txt.Html2Txt.Bulleting;
import de.unkrig.html2txt.Html2Txt.HtmlException;

/**
 * This formatter is much more complicated than the others, so we dedicate a separate compilation unit to it.
 */
final
class TableFormatter implements BlockElementFormatter {

    @Override public void
    format(
        Html2Txt                       html2Txt,
        int                            leftMarginWidth,
        Bulleting                      bulleting,
        int                            measure,
        Element                        tableElement,
        Consumer<? super CharSequence> output
    ) throws HtmlException {

        Table table = this.parse(html2Txt, tableElement);

        Cell[][] grid = this.arrange(table);

        // Format the table to the absolute minimum cell widths.
        int[] minimumColumnWidths;
        {
            int[] columnMeasures = new int[grid[0].length];
            TableFormatter.formatCells(html2Txt, grid, columnMeasures, table.columnSeparator.length());
            SortedMap<Integer, SortedMap<Integer, Integer>> minimumCellWidths = TableFormatter.computeCellWidths(
                html2Txt,
                grid
            );
            minimumColumnWidths = TableFormatter.computeColumnWidths(minimumCellWidths, grid[0].length);
        }

        int minimumTableWidth = TableFormatter.computeTableWidth(
            minimumColumnWidths,
            table.leftBorder.length(),
            table.columnSeparator.length(),
            table.rightBorder.length()
        );

        int[] columnWidths;
        if (measure <= minimumTableWidth) {
            columnWidths = minimumColumnWidths;
        } else {

            // Format the table to its "natural" cell widths.
            int[] naturalColumnWidths;
            {
                int[] columnMeasures = new int[grid[0].length];
                Arrays.fill(columnMeasures, Integer.MAX_VALUE);
                TableFormatter.formatCells(html2Txt, grid, columnMeasures, table.columnSeparator.length());
                SortedMap<Integer, SortedMap<Integer, Integer>> naturalCellWidths = TableFormatter.computeCellWidths(
                    html2Txt,
                    grid
                );
                naturalColumnWidths = TableFormatter.computeColumnWidths(naturalCellWidths, grid[0].length);
            }

            int naturalTableWidth = TableFormatter.computeTableWidth(
                naturalColumnWidths,
                table.leftBorder.length(),
                table.columnSeparator.length(),
                table.rightBorder.length()
            );

            if (naturalTableWidth <= measure) {
                columnWidths = naturalColumnWidths;
                if (table.is100Percent) {
                    TableFormatter.spreadEvenly(measure - naturalTableWidth, columnWidths);
                }
            } else {
                columnWidths = minimumColumnWidths;
                TableFormatter.spreadEvenly(measure - minimumTableWidth, columnWidths);
                TableFormatter.formatCells(html2Txt, grid, columnWidths, table.columnSeparator.length());
            }
        }

        // Now compute the optimal row heights.
        int[] rowHeights;
        {
            SortedMap<Integer, SortedMap<Integer, Integer>> cellHeights = TableFormatter.computeCellHeights(
                html2Txt,
                grid
            );
            rowHeights = TableFormatter.computeColumnWidths(cellHeights, grid.length);
        }

        // Non-null elements represent rowspanned cell contents.
        @SuppressWarnings("unchecked") Producer<String>[] cellContents = new Producer[grid[0].length];

        for (int rowno = 0;; rowno++) {

            // Print top border resp. row separator resp. bottom border.
            char c = (
                rowno == 0               ? table.topBorder    :
                rowno == grid.length - 1 ? table.bottomBorder :
                table.rowSeparator
            );
            if (c != '\0') {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtil.repeat(leftMarginWidth, ' '));
                sb.append(StringUtil.repeat(table.leftBorder.length(), '+'));
                for (int colno = 0; colno < grid[0].length; colno++) {
                    if (cellContents[colno] != null) {
                        sb.append(cellContents[colno].produce());
                    } else {
                        char c2 = table.headingRowSeparator != '\0' && rowno >= 1 && grid[rowno - 1][colno].isTh ? table.headingRowSeparator : c;
                        sb.append(StringUtil.repeat(columnWidths[colno], c2));
                    }
                    // The "cross" at the intersection of cells' borders.
                    sb.append(StringUtil.repeat(
                        (colno == grid[0].length - 1 ? table.rightBorder : table.columnSeparator).length(),
                        (rowno == 0 || (colno != grid[0].length - 1 && grid[rowno - 1][colno] == grid[rowno - 1][colno + 1]))
                        && (colno != grid[0].length - 1 && grid[rowno][colno] == grid[rowno][colno + 1]) ? c : '+'
                    ));
                }
                output.consume(sb.toString());
            }

            if (rowno == grid.length) break;
            Cell[] row = grid[rowno];

            // Print the row contents.
            {

                // The segments of a line of the row.
                List<Producer<String>> segments = new ArrayList<Producer<String>>();

                // Left margin.
                segments.add(ProducerUtil.constantProducer(StringUtil.repeat(leftMarginWidth, ' ')));

                // Left border.
                segments.add(ProducerUtil.constantProducer(table.leftBorder));

                for (int colno = 0; colno < row.length;) {
                    Cell cell = row[colno];

                    // Next line-of-text of a cell.
                    int w = columnWidths[colno];
                    int colno2;
                    for (colno2 = colno + 1; colno2 < row.length && row[colno2] == cell; colno2++) {
                        w += table.columnSeparator.length() + columnWidths[colno2];
                    }

                    Producer<String> p = cellContents[colno];
                    if (p == null) {
                        List<CharSequence> lines = cell.lines;
                        assert lines != null;
                        p = ProducerUtil.concat(
                            Html2Txt.rightPad(ProducerUtil.fromCollection(lines), w, ' '),
                            ProducerUtil.constantProducer(StringUtil.repeat(w, ' '))
                        );
                    }
                    if (rowno < grid.length - 1 && grid[rowno + 1][colno] == cell) {
                        cellContents[colno] = p;
                    } else {
                        cellContents[colno] = null;
                    }

                    segments.add(p);

                    colno = colno2;

                    // Column separator resp. right border.
                    segments.add(ProducerUtil.constantProducer(
                        colno == row.length ? table.rightBorder : table.columnSeparator
                    ));
                }

                // Now print all lines of the row contents.
                for (int i = 0; i < rowHeights[rowno]; i++) {
                    StringBuilder sb = new StringBuilder();
                    for (Producer<String> p : segments) {
                        sb.append(p.produce());
                    }
                    output.consume(sb.toString());
                }
            }
        }
    }

    /**
     * Fills {@link Cell#lines}, {@link Cell#width} and {@link Cell#height} while obeying the the given
     * <var>columnMeasures</var>.
     */
    public static void
    formatCells(Html2Txt html2Txt, Cell[][] grid, int[] columnMeasures, int columnSeparatorWidth)
    throws HtmlException {

        for (int rowno = 0; rowno < grid.length; rowno++) {
            Cell[] row = grid[rowno];
            assert row.length == columnMeasures.length : row.length + "!=" + columnMeasures.length;
            for (int colno = 0; colno < row.length;) {
                Cell cell = row[colno];

                if (rowno > 0 && grid[rowno - 1][colno] == cell) { colno++; continue; }

                if (colno > 0 && row[colno - 1] == cell) { colno++; continue; }

                int columnMeasure = columnMeasures[colno];
                int colno2;
                for (colno2 = colno + 1; colno2 < row.length && row[colno2] == cell; colno2++) {
                    columnMeasure += columnSeparatorWidth + columnMeasures[colno2];
                }
                colno = colno2;

                List<CharSequence> lines = (cell.lines = new ArrayList<CharSequence>());
                html2Txt.formatBlocks(
                    0,                                  // leftMarginWidth
                    Bulleting.NONE,                     // bulleting
                    Bulleting.NONE,                      // measure
                    columnMeasure,                    // nodes
                    cell.childNodes
, ConsumerUtil.addToCollection(lines) // output
                );
                cell.width  = Html2Txt.maxLength(lines);
                cell.height = lines.size();
            }
        }
    }

    class Cell {

        final boolean        isTh;
        final Iterable<Node> childNodes;

        @Nullable List<CharSequence> lines;
        int                          width, height;

        Cell(boolean isTh, Iterable<Node> childNodes) {
            this.isTh = isTh;
            this.childNodes = childNodes;
        }
    }

    /**
     * Creates a two-dimensional grid of (non-{@code null}) {@link Cell}s, where each {@code <td>} is referenced by
     * {@code colspan} x {@code rowspan} tiles.
     * <p>
     *   Some tiles may be "filler" tiles. Such tiles may occur for "short" table rows, or for colspans/rowspans that
     *   would otherwise cause overlaps.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <pre>
     *    +-----+-----+-----+-----+
     *    |  A  |  B  |  C  |#####|
     *    +-----+-----+-----+-----+
     *    |  D  |  E  |  F  |  G  |
     *    +-----+-----+-----+-----+
     *    |  H  |     |  J  |  K  |
     *    +-----+  I  +-----+-----+
     *    |#####|     |     L     |
     *    +-----+-----+-----+-----+
     *  </pre>
     */
    private Cell[][]
    arrange(Table table) {
        List<List<Cell> /*row*/> cells = new ArrayList<List<Cell>>();

        // For each <td>...
        int rowno = 0;
        for (Tr tr : table.trs) {
            int colno = 0;
            for (Td td : tr.tds) {

                // Determine the right place for the tiles that represent the the <td>.
                COLNO:
                for (;; colno++) {
                    for (int j = 0; j < td.rowspan; j++) {
                        if (cells.size() <= rowno + j) continue;
                        List<Cell> row = cells.get(rowno + j);
                        if (row == null) continue;
                        for (int i = 0; i < td.colspan; i++) {
                            if (row.size() > colno + i && row.get(colno + i) != null) continue COLNO;
                        }
                    }
                    break;
                }

                // At this point, "rowno" and "colno" point to the "right" place for the new cell.
                // Insert colspan x colspan tiles into the grid.
                Cell cell = new Cell(td.isTh, td.childNodes);
                for (int j = 0; j < td.rowspan; j++) {
                    while (cells.size() <= rowno + j) cells.add(new ArrayList<Cell>());
                    List<Cell> row = cells.get(rowno + j);
                    for (int i = 0; i < td.colspan; i++) {
                        while (row.size() <= colno + i) row.add(null);
                        Cell prev = row.set(colno + i, cell);
                        assert prev == null;
                    }
                }

                colno += td.colspan;
            }

            rowno++;
        }

        int nrows = cells.size();
        int ncols = 0;
        for (List<Cell> row : cells) {
            if (row.size() > ncols) ncols = row.size();
        }

        Cell[][] result = new Cell[nrows][ncols];
        rowno = 0;
        for (List<Cell> row : cells) {
            int colno = 0;
            for (Cell cell : row) {
                result[rowno][colno++] = cell;
            }
            rowno++;
        }

        // Put the "filler" tile into the empty places.
        Cell filler = new Cell(false, Collections.<Node>emptyList());
        filler.width = filler.height = 0;
        filler.lines = Collections.emptyList();
        for (rowno = 0; rowno < result.length; rowno++) {
            Cell[] row = result[rowno];
            for (int colno = 0; colno < row.length; colno++) {
                if (row[colno] == null) row[colno] = filler;
            }
        }

        return result;
    }

    private static int
    computeTableWidth(int[] columnWidths, int leftBorderWidth, int columnSeparatorWidth, int rightBorderWidth) {
        return leftBorderWidth + rightBorderWidth + (columnWidths.length - 1) * columnSeparatorWidth + TableFormatter.sum(columnWidths);
    }

    private static int
    sum(int[] list) {
        int result = 0;
        for (Integer i : list) result += i;
        return result;
    }

    /**
     * @return Column widths suitable for the given <var>cellWidths</var>
     */
    private static int[]
    computeColumnWidths(SortedMap<Integer, SortedMap<Integer, Integer>> cellWidths, int ncols) {

        int[] result = new int[ncols];
        for (Entry<Integer, SortedMap<Integer, Integer>> e : cellWidths.entrySet()) {
            int                         colspan     = e.getKey();
            SortedMap<Integer, Integer> colno2Width = e.getValue();

            for (Entry<Integer, Integer> e2 : colno2Width.entrySet()) {
                int colno     = e2.getKey();
                int cellWidth = e2.getValue();

                int tcw = 0;
                for (int i = colno; i < colno + colspan; i++) tcw += result[i];
                if (tcw < cellWidth) {
                    int excess = cellWidth - tcw;

                    TableFormatter.spreadEvenly(excess, result, colno, colspan);
                }
            }
        }

        return result;
    }

    private static void
    spreadEvenly(int excess, int[] values) {
        TableFormatter.spreadEvenly(excess, values, 0, values.length);
    }

    private static void
    spreadEvenly(int excess, int[] values, int off, int len) {

        if (excess == 0) return;

        int n = excess / len;
        int nfrac = excess - (len * n);
        int x = 0;
        for (int i = 0; i < len; i++) {
            int w = values[off + i] + n;
            if ((x += nfrac) > len) {
                w++;
                x -= len;
            }
            values[off + i] = w;
        }
    }

    class Table {

        /** '\0' means "no border/separator". */
        final char topBorder, rowSeparator, headingRowSeparator, bottomBorder;

        /** "" means "no border/separator". */
        final String leftBorder, columnSeparator, rightBorder;

        final List<Tr> trs;

        /** Whether to stretch the table to the full measure if it is more narrow. */
        private final boolean is100Percent;

        public Table(
            char     topBorder,
            char     rowSeparator,
            char     headingRowSeparator,
            char     bottomBorder,
            String   leftBorder,
            String   columnSeparator,
            String   rightBorder,
            boolean  is100Percent,
            List<Tr> trs
        ) {
            this.topBorder           = topBorder;
            this.rowSeparator        = rowSeparator;
            this.headingRowSeparator = headingRowSeparator;
            this.bottomBorder        = bottomBorder;
            this.leftBorder          = leftBorder;
            this.columnSeparator     = columnSeparator;
            this.rightBorder         = rightBorder;
            this.is100Percent        = is100Percent;
            this.trs                 = trs;
        }
    }

    class Tr {

        /** The "{@code <td>}" and "{@code <th>}" subelements of this "{@code <tr>}". */
        final List<Td> tds;

        Tr(List<Td> tds) { this.tds = tds; }
    }

    /**
     * Representation of a "{@code <td>}" or "{@code <th>}" subelement of a "{@code <tr>}" table row.
     */
    class Td {

        final boolean isTh;

        /** 1 or greater. */
        final int rowspan, colspan;

        final Iterable<Node> childNodes;

        Td(boolean isTh, int rowspan, int colspan, Iterable<Node> childNodes) {
            this.isTh       = isTh;
            this.rowspan    = rowspan;
            this.colspan    = colspan;
            this.childNodes = childNodes;

        }
    }

    private Table
    parse(Html2Txt html2Txt, Element tableElement) throws HtmlException {

        final char   topBorder, rowSeparator, headingRowSeparator, bottomBorder;
        final String leftBorder, columnSeparator, rightBorder;
        {
            Attr borderAttribute = tableElement.getAttributeNode("border");
            String border = borderAttribute == null ? null : borderAttribute.getValue();
            if ("1".equals(border)) {
                topBorder = rowSeparator = bottomBorder = '-';
                headingRowSeparator = '=';
                leftBorder = columnSeparator = rightBorder = "|";
            } else
            if ("2".equals(border)) {
                topBorder = rowSeparator = bottomBorder = '=';
                headingRowSeparator = '=';
                leftBorder = columnSeparator = rightBorder = "||";
            } else
            {
                topBorder = rowSeparator = headingRowSeparator = bottomBorder = '\0';
                leftBorder = rightBorder = "";
                columnSeparator = " ";
            }
        }

        final boolean is100Percent;
        {
            Attr s = tableElement.getAttributeNode("width");
            is100Percent = "100%".equals(s);
        }

        List<Tr> trs = new ArrayList<Tr>();
        for (Node trNode : XmlUtil.iterable(tableElement.getChildNodes())) {

            // Ignore whitespace text before, between and after <tr>s.
            if (trNode.getNodeType() == Node.TEXT_NODE && trNode.getTextContent().trim().length() == 0) continue;

            if (Html2Txt.isElement(trNode, "tr") == null) {
                html2Txt.htmlErrorHandler.warning(new HtmlException(
                    trNode,
                    "Expected \"<tr>\" instead of \"" + XmlUtil.toString(trNode) + "\""
                ));
                continue;
            }

            List<Td> tds = new ArrayList<Td>();
            for (Node n : XmlUtil.iterable(trNode.getChildNodes())) {

                // Ignore whitespace text before, between and after <tr>s.
                if (n.getNodeType() == Node.TEXT_NODE && n.getTextContent().trim().length() == 0) continue;

                Element tdElement;
                boolean isTh;
                if ((tdElement = Html2Txt.isElement(n, "th")) != null) {
                    isTh = true;
                } else
                if ((tdElement = Html2Txt.isElement(n, "td")) != null) {
                    isTh = false;
                } else
                {
                    html2Txt.htmlErrorHandler.warning(new HtmlException(n, "Expected \"<td>\" or \"<th>\""));
                    continue;
                }

                int colspan;
                try {
                    colspan = Math.max(1, Integer.parseInt(tdElement.getAttributeNode("colspan").getValue()));
                } catch (Exception e) {
                    colspan = 1;
                }

                int rowspan;
                try {
                    rowspan = Math.max(1,  Integer.parseInt(tdElement.getAttributeNode("rowspan").getValue()));
                } catch (Exception e) {
                    rowspan = 1;
                }

                tds.add(new Td(isTh, rowspan, colspan, XmlUtil.iterable(tdElement.getChildNodes())));
            }

            trs.add(new Tr(tds));
        }

        return new Table(
            topBorder,
            rowSeparator,
            headingRowSeparator,
            bottomBorder,
            leftBorder,
            columnSeparator,
            rightBorder,
            is100Percent,
            trs
        );
    }

    /**
     * @return <var>colspan</var> => <var>colno</var> => <var>width</var>
     */
    private static SortedMap<Integer, SortedMap<Integer, Integer>>
    computeCellWidths(Html2Txt html2Txt, Cell[][] grid) {

        SortedMap<Integer /*colspan*/, SortedMap<Integer /*colno*/, Integer /*width*/>>
        result = new TreeMap<Integer, SortedMap<Integer,Integer>>();

        for (int rowno = 0; rowno < grid.length; rowno++) {
            Cell[] row = grid[rowno];
            for (int colno = 0; colno < row.length;) {
                Cell cell = grid[rowno][colno];

                int colno2 = colno + 1;
                while (colno2 < row.length && row[colno2] == cell) colno2++;
                int colspan = colno2 - colno;

                SortedMap<Integer /*colno*/, Integer /*width*/> x = result.get(colspan);
                if (x == null) result.put(colspan, (x = new TreeMap<Integer, Integer>()));

                Integer prev = x.get(colno);
                if (prev == null || prev < cell.width) x.put(colno, cell.width);

                colno = colno2;
            }
        }

        return result;
    }

    /**
     * @return <var>rowspan</var> => <var>rowno</var> => <var>width</var>
     */
    private static SortedMap<Integer, SortedMap<Integer, Integer>>
    computeCellHeights(Html2Txt html2Txt, Cell[][] grid) {

        SortedMap<Integer, SortedMap<Integer, Integer>> result = new TreeMap<Integer, SortedMap<Integer,Integer>>();

        for (int rowno = 0; rowno < grid.length; rowno++) {
            Cell[] row = grid[rowno];
            for (int colno = 0; colno < row.length; colno++) {
                Cell cell = grid[rowno][colno];

                if (rowno > 0 && grid[rowno - 1][colno] == cell) continue;

                int rowno2 = rowno + 1;
                while (rowno2 < grid.length && grid[rowno2][colno] == cell) rowno2++;
                int rowspan = rowno2 - rowno;

                SortedMap<Integer, Integer> x = result.get(rowspan);
                if (x == null) result.put(rowspan, (x = new TreeMap<Integer, Integer>()));
                Integer prev = x.get(rowno);
                if (prev == null || prev < cell.height) x.put(rowno, cell.height);
            }
        }

        return result;
    }
}