package com.springernature.multipartform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DiskBackedPart extends PartWithInputStream {

    private final File theFile;

    public DiskBackedPart(Part part, File theFile) {
        super(part.fieldName, part.formField, part.contentType, part.fileName, part.headers);
        this.theFile = theFile;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(theFile);
    }
}
