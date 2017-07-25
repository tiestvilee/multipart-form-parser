package com.springernature.multipartform;

import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.springernature.multipartform.MultipartFormParts.FIELD_SEPARATOR;

public class ValidMultipartFormBuilder {
    public static final String CR_LF = new String(FIELD_SEPARATOR);
    private final Deque<String> boundary = new ArrayDeque<>();
    private final StringBuilder builder = new StringBuilder();

    public ValidMultipartFormBuilder(String boundary) {this.boundary.push(boundary);}

    public String build() {
        return builder.toString() + boundary.peek() + "--" + CR_LF;
    }

    public ValidMultipartFormBuilder field(String name, String value) {
        part(value,
            pair("Content-Disposition", sequence(pair("form-data", null), pair("name", name)))
        );
        return this;
    }

    private void appendHeader(final String headerName, Sequence<Pair<String, String>> pairs) {
        builder.append(headerName).append(": ")
            .append(pairs.map((pair) -> {
                if (pair.getValue() != null) {
                    return pair.getKey() + "=\"" + pair.getValue() + "\"";
                }
                return pair.getKey();
            }).toString("; "))
            .append(CR_LF);
    }

    public ValidMultipartFormBuilder part(String contents, Pair<String, Sequence<Pair<String, String>>>... headers) {
        builder.append(boundary.peek()).append(CR_LF);
        sequence(headers).forEach(header -> {
            appendHeader(header.getKey(), header.getValue());
        });
        builder.append(CR_LF)
            .append(contents).append(CR_LF);
        return this;
    }

    public ValidMultipartFormBuilder file(String fieldName, String filename, String contentType, String contents) {
        part(contents,
            pair("Content-Disposition", sequence(pair("form-data", null), pair("name", fieldName), pair("filename", filename))),
            pair("Content-Type", sequence(pair(contentType, null)))
        );
        return this;
    }

    public ValidMultipartFormBuilder rawPart(String raw) {
        builder.append(boundary.peek()).append(CR_LF).append(raw).append(CR_LF);
        return this;
    }

    public ValidMultipartFormBuilder startMultipart(String multipartFieldName, String subpartBoundary) {
        builder.append(boundary.peek()).append(CR_LF);
        appendHeader("Content-Disposition", sequence(pair("form-data", null), pair("name", multipartFieldName)));
        appendHeader("Content-Type", sequence(pair("multipart/mixed", null), pair("boundary", subpartBoundary)));
        builder.append(CR_LF);
        boundary.push("--" + subpartBoundary);
        return this;
    }

    public ValidMultipartFormBuilder attachment(String fileName, String contentType, String contents) {
        part(contents,
            pair("Content-Disposition", sequence(pair("attachment", null), pair("filename", fileName))),
            pair("Content-Type", sequence(pair(contentType, null)))
        );
        return this;
    }

    public ValidMultipartFormBuilder endMultipart() {
        builder.append(boundary.pop()).append("--" + CR_LF);
        return this;
    }
}
