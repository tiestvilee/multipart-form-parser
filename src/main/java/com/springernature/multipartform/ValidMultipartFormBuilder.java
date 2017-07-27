package com.springernature.multipartform;

import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.springernature.multipartform.MultipartFormParts.FIELD_SEPARATOR;
import static com.springernature.multipartform.MultipartFormParts.STREAM_TERMINATOR;

public class ValidMultipartFormBuilder {
    private final Deque<byte[]> boundary = new ArrayDeque<>();
    private final ByteArrayOutputStream builder = new ByteArrayOutputStream();
    private final Charset encoding;

    public ValidMultipartFormBuilder(String boundary) {
        this(boundary.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public ValidMultipartFormBuilder(byte[] boundary, Charset encoding) {
        this.encoding = encoding;
        this.boundary.push(boundary);
    }

    public byte[] build() {
        try {
            builder.write(boundary.peek());
            builder.write(STREAM_TERMINATOR);
            builder.write(FIELD_SEPARATOR);
            return builder.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder field(String name, String value) {
        part(value,
            pair("Content-Disposition", sequence(pair("form-data", null), pair("name", name)))
        );
        return this;
    }

    private void appendHeader(final String headerName, Sequence<Pair<String, String>> pairs) {
        try {
            String headers = headerName + ": " + pairs.map((pair) -> {
                if (pair.getValue() != null) {
                    return pair.getKey() + "=\"" + pair.getValue() + "\"";
                }
                return pair.getKey();
            }).toString("; ");

            builder.write(headers.getBytes(encoding));
            builder.write(FIELD_SEPARATOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder part(String contents, Pair<String, Sequence<Pair<String, String>>>... headers) {
        try {
            builder.write(boundary.peek());
            builder.write(FIELD_SEPARATOR);
            sequence(headers).forEach(header -> {
                appendHeader(header.getKey(), header.getValue());
            });
            builder.write(FIELD_SEPARATOR);
            builder.write(contents.getBytes(encoding));
            builder.write(FIELD_SEPARATOR);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder file(String fieldName, String filename, String contentType, String contents) {
        part(contents,
            pair("Content-Disposition", sequence(pair("form-data", null), pair("name", fieldName), pair("filename", filename))),
            pair("Content-Type", sequence(pair(contentType, null)))
        );
        return this;
    }

    public ValidMultipartFormBuilder rawPart(String raw) {
        try {
            builder.write(boundary.peek());
            builder.write(FIELD_SEPARATOR);
            builder.write(raw.getBytes(encoding));
            builder.write(FIELD_SEPARATOR);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder startMultipart(String multipartFieldName, String subpartBoundary) {
        try {
            builder.write(boundary.peek());
            builder.write(FIELD_SEPARATOR);
            appendHeader("Content-Disposition", sequence(pair("form-data", null), pair("name", multipartFieldName)));
            appendHeader("Content-Type", sequence(pair("multipart/mixed", null), pair("boundary", subpartBoundary)));
            builder.write(FIELD_SEPARATOR);
            boundary.push((new String(STREAM_TERMINATOR, encoding) + subpartBoundary).getBytes(encoding));
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder attachment(String fileName, String contentType, String contents) {
        part(contents,
            pair("Content-Disposition", sequence(pair("attachment", null), pair("filename", fileName))),
            pair("Content-Type", sequence(pair(contentType, null)))
        );
        return this;
    }

    public ValidMultipartFormBuilder endMultipart() {
        try {
            builder.write(boundary.pop());
            builder.write(STREAM_TERMINATOR);
            builder.write(FIELD_SEPARATOR);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
