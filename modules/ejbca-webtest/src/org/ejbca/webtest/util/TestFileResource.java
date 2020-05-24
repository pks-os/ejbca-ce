/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.webtest.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

// TODO ECA-8963: Extract into a separate module ejbca-unittest, as it is common utility class that can be reused.
/**
 * This is a help class to locate an input file for a test from classpath.
 *
 * @version $Id$
 */
public class TestFileResource {

    private final String fileName;
    private final File file;

    public TestFileResource(final String fileName) {
        this.fileName = fileName;
        final URL fileUrl = getClass().getClassLoader().getResource(fileName);
        if(fileUrl == null) {
            throw new RuntimeException("Cannot locate file [" + fileName + "] in classpath.");
        }
        this.file = new File(fileUrl.getFile());
    }

    /**
     * Returns a file instance.
     * @return file instance.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns a file name.
     * @return file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns an absolute path of the test file.
     *
     * @return absolute path of the test file.
     */
    public String getFileAbsolutePath() {
        return file.getAbsolutePath();
    }

    /**
     * Returns the file content as string using default charset UTF-8.
     *
     * @return file content.
     */
    public String getFileContent() throws IOException {
        return getFileContent(StandardCharsets.UTF_8);
    }

    /**
     * Returns the file content as string using given charset.
     * @param charset charset.
     * @return file content.
     */
    public String getFileContent(final Charset charset) throws IOException {
        final String LS = System.lineSeparator();
        final List<String> fileLines = Files.readAllLines(file.toPath(), charset);
        return String.join(LS, fileLines);
    }

}
