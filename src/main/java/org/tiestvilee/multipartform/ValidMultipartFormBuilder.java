package org.tiestvilee.multipartform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.tiestvilee.multipartform.StreamingMultipartFormParts.FIELD_SEPARATOR;
import static org.tiestvilee.multipartform.StreamingMultipartFormParts.STREAM_TERMINATOR;

public class ValidMultipartFormBuilder {
    private final Deque<byte[]> boundary = new ArrayDeque<>();
    private final ByteArrayOutputStream builder = new ByteArrayOutputStream();
    private final Charset encoding;

    public ValidMultipartFormBuilder(String boundary) {
        this(boundary.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public ValidMultipartFormBuilder(byte[] boundary, Charset encoding) {
        this.encoding = encoding;
        this.boundary.push(StreamingMultipartFormParts.prependBoundaryWithStreamTerminator(boundary));
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
            pair("Content-Disposition", asList(pair("form-data", null), pair("name", name)))
        );
        return this;
    }

    private void appendHeader(final String headerName, List<Pair<String, String>> pairs) {
        try {
            String headers = headerName + ": " + pairs.stream().map((pair) -> {
                if (pair.getValue() != null) {
                    return pair.getKey() + "=\"" + pair.getValue() + "\"";
                }
                return pair.getKey();
            }).collect(Collectors.joining("; "));

            builder.write(headers.getBytes(encoding));
            builder.write(FIELD_SEPARATOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder part(String contents, Pair<String, List<Pair<String, String>>>... headers) {
        try {
            builder.write(boundary.peek());
            builder.write(FIELD_SEPARATOR);
            asList(headers).forEach(header -> {
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
            pair("Content-Disposition", asList(pair("form-data", null), pair("name", fieldName), pair("filename", filename))),
            pair("Content-Type", asList(pair(contentType, null)))
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
            appendHeader("Content-Disposition", asList(pair("form-data", null), pair("name", multipartFieldName)));
            appendHeader("Content-Type", asList(pair("multipart/mixed", null), pair("boundary", subpartBoundary)));
            builder.write(FIELD_SEPARATOR);
            boundary.push((new String(STREAM_TERMINATOR, encoding) + subpartBoundary).getBytes(encoding));
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder attachment(String fileName, String contentType, String contents) {
        part(contents,
            pair("Content-Disposition", asList(pair("attachment", null), pair("filename", fileName))),
            pair("Content-Type", asList(pair(contentType, null)))
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

    public static <K, V> Pair<K, V> pair(K key, V value) {
        return new Pair<>(key, value);
    }

    public static class Pair<K, V> {

        public final K key;
        public final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
