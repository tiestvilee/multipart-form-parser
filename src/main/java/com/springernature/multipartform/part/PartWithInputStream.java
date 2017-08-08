package com.springernature.multipartform.part;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class PartWithInputStream extends Part implements Closeable {
    public PartWithInputStream(String fieldName, boolean formField, String contentType, String fileName, Map<String, String> headers) {super(fieldName, formField, contentType, fileName, headers);}

    public abstract InputStream getNewInputStream() throws IOException;
}
