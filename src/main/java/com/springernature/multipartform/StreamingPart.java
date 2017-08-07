package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.StreamTooLongException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StreamingPart extends Part {
    public final InputStream inputStream;

    public StreamingPart(String fieldName, boolean formField, String contentType, String fileName, InputStream inputStream, Map<String, String> headers) {
        super(fieldName, formField, contentType, fileName, headers);
        this.inputStream = inputStream;
    }

    public String getContentsAsString() throws IOException {
        return getContentsAsString(StandardCharsets.UTF_8, 4096);
    }

    public String getContentsAsString(Charset encoding, int maxPartContentSize) throws IOException {
        byte[] bytes = new byte[maxPartContentSize];
        int length = getContentsAsBytes(maxPartContentSize, bytes);
        return new String(bytes, 0, length, encoding);
    }

    public int getContentsAsBytes(int maxLength, byte[] bytes) throws IOException {
        int length = 0;

        while (true) {
            int count = inputStream.read(bytes, length, maxLength - length);
            if (count < 0) {
                inputStream.close();
                return length;
            }
            if (length >= maxLength) {
                inputStream.close();
                throw new StreamTooLongException("Part contents was longer than " + maxLength + " bytes");
            }
            length += count;
        }
    }
}
