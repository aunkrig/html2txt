
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;

import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Converts one ore more HTML files into plain text files.
 * <p>
 *   The following attributes are mutually exclusive:
 * </p>
 * <dl>
 *   <dd>{@link #setTofile(File)}</dd>
 *   <dd>{@link #setTodir(File)}</dd>
 * </dl>
 */
public
class AntTask extends Task {

    private final Html2Txt html2txt = new Html2Txt();

    @Nullable private File                 file;
    @Nullable private File                 tofile;
    @Nullable private File                 todir;
    private final List<ResourceCollection> resourceCollections = new ArrayList<ResourceCollection>();

    // BEGIN CONFIGURATION SETTERS

    /**
     * The file that contains the HTML document to convert.
     */
    public void
    setFile(File value) { this.file = value; }

    /**
     * The file that contains generated plain text. Only allowed if exactly <i>one</i> HTML is converted.
     */
    public void
    setTofile(File value) { this.tofile = value; }

    /**
     * The directory where the output file(s) will be created. The name of each output file(s) will be that of the
     * input file, less the "{@code .html}" suffix (if any), plus an "{@code .txt}" extension.
     *
     * @de.unkrig.doclet.ant.defaultValue The project's base directory
     */
    public void
    setTodir(File value) { this.todir = value; }

    /**
     * Resources to convert.
     */
    public void
    addConfigured(ResourceCollection value) { this.resourceCollections.add(value); }

    // END CONFIGURATION SETTERS

    /**
     * The ANT task "execute" method.
     *
     * @see Task#execute
     */
    @Override public void
    execute() throws BuildException {
        try {
            this.execute2();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void
    execute2() throws Exception {

        final File                     file                = this.file;
        final File                     tofile              = this.tofile;
        final File                     todir               = this.todir;
        final List<ResourceCollection> resourceCollections = this.resourceCollections;

        List<Resource> resources = new ArrayList<Resource>();

        if (file != null) resources.add(new FileResource(file));

        for (ResourceCollection resourceCollection : resourceCollections) {

            // Process each resource of each collection.
            for (
                @SuppressWarnings("unchecked") Iterator<Resource> it = resourceCollection.iterator();
                it.hasNext();
            ) resources.add(it.next());
        }

        if (resources.isEmpty()) return;

        if (resources.size() == 1 && tofile != null && todir == null) {
            this.convertResource(resources.get(0), tofile);
        } else
        if (tofile == null) {
            if (todir == null) this.todir = this.getProject().getBaseDir();
            for (Resource resource : resources) {
                String outputFileName = resource.getName();
                if (outputFileName.endsWith(".html")) {
                    outputFileName = outputFileName.substring(outputFileName.length() - 5);
                }
                outputFileName += ".txt";
                this.convertResource(resource, new File(todir, outputFileName));
            }
        } else
        {
            throw new BuildException("Invalid combination of attributes and subelements");
        }
    }

    private void
    convertResource(Resource in, final File out) throws Exception {

        if (in.isFilesystemOnly()) {
            this.html2txt.html2txt(((FileResource) in).getFile(), out);
        } else
        {
            FileUtil.asFile(
                in.getInputStream(),                         // inputStream
                true,                                        // closeInputStream
                "h2t",                                       // prefix
                ".html",                                     // suffix
                null,                                        // directory
                new ConsumerWhichThrows<File, Exception>() { // delegate

                    @Override public void
                    consume(File temporaryFile) throws Exception {
                        AntTask.this.html2txt.html2txt(temporaryFile, out);
                    }
                }
            );
        }
    }
}
