package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.StreamTooLongException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class Part extends InputStream implements Closeable {
    public final String fieldName;
    public final boolean formField;
    public final String contentType;
    public final String fileName;
    public final InputStream inputStream;
    public final Map<String, String> headers;

    public Part(String fieldName, boolean formField, String contentType, String fileName, InputStream inputStream, Map<String, String> headers) {
        this.fieldName = fieldName;
        this.formField = formField;
        this.contentType = contentType;
        this.fileName = fileName;
        this.inputStream = inputStream;
        this.headers = Collections.unmodifiableMap(headers);
    }

    @Override public int read() throws IOException {
        return inputStream.read();
    }

    @Override public void close() throws IOException {
        inputStream.close();
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isFormField() {
        return formField;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void sink() {
        throw new UnsupportedOperationException("sink not implemented");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getContentsAsString() throws IOException {
        return getContentsAsString(4096, StandardCharsets.UTF_8);
    }

    public String getContentsAsString(int maxLength, Charset encoding) throws IOException {
        byte[] bytes = new byte[maxLength];
        int length = getContentsAsBytes(maxLength, bytes);
        return new String(bytes, 0, length, encoding);
    }

    private int getContentsAsBytes(int maxLength, byte[] bytes) throws IOException {
        int length = 0;

        while (true) {
            int count = inputStream.read(bytes, length, maxLength - length);
            if (count < 0) {
                inputStream.close();
                break;
            }
            if (length >= maxLength) {
                inputStream.close();
                throw new StreamTooLongException("Part contents was longer than " + maxLength + " bytes");
            }
            length += count;
        }
        return length;
    }

    public InMemoryPart realise(Charset encoding, int maxPartContentSize) throws IOException {
        byte[] bytes = new byte[maxPartContentSize];
        int length = getContentsAsBytes(maxPartContentSize, bytes);
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return new InMemoryPart(this, result, encoding);
    }

}
