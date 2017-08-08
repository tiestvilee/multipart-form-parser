package com.springernature.multipartform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemException;

public class DiskBackedPart extends PartWithInputStream {

    private final File theFile;

    public DiskBackedPart(Part part, File theFile) {
        super(part.fieldName, part.formField, part.contentType, part.fileName, part.headers);
        this.theFile = theFile;
    }

    public InputStream getNewInputStream() throws IOException {
        return new FileInputStream(theFile);
    }

    public void close() throws IOException {
        if (!theFile.delete()) {
            throw new FileSystemException("Failed to delete file");
        }
    }
}
