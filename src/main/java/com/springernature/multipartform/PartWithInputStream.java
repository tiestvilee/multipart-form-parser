package com.springernature.multipartform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class PartWithInputStream extends Part {
    public PartWithInputStream(String fieldName, boolean formField, String contentType, String fileName, Map<String, String> headers) {super(fieldName, formField, contentType, fileName, headers);}

    public abstract InputStream getInputStream() throws IOException;
}
