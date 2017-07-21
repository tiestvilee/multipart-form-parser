package com.springernature.multipartform;

import com.googlecode.totallylazy.Pair;

import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.springernature.multipartform.MultipartFormParts.FIELD_SEPARATOR;

public class ValidMultipartFormBuilder {
    public static final String CR_LF = new String(FIELD_SEPARATOR);
    private final String boundary;
    private final StringBuilder builder = new StringBuilder();

    public ValidMultipartFormBuilder(String boundary) {this.boundary = boundary;}

    public String build() {
        return builder.toString() + boundary + "--" + CR_LF;
    }

    public ValidMultipartFormBuilder field(String name, String value) {
        builder.append(boundary).append(CR_LF);
        appendHeader("Content-Disposition", pair("form-data", null), pair("name", name));
        builder.append(CR_LF)
            .append(value).append(CR_LF);
        return this;
    }

    private void appendHeader(final String headerName, Pair... pairs) {
        builder.append(headerName).append(": ")
            .append(sequence(pairs).map((pair) -> {
                if (pair.getValue() != null) {
                    return pair.getKey() + "=\"" + pair.getValue() + "\"";
                }
                return pair.getKey();
            }).toString("; "))
            .append(CR_LF);
    }

    public ValidMultipartFormBuilder file(String fieldName, String filename, String contentType, String contents) {
        builder.append(boundary).append(CR_LF);
        appendHeader("Content-Disposition", pair("form-data", null), pair("name", fieldName), pair("filename", filename));
        appendHeader("Content-Type", pair(contentType, null));
        builder.append(CR_LF)
            .append(contents).append(CR_LF);
        return this;
    }

    public ValidMultipartFormBuilder rawPart(String raw) {
        builder.append(boundary).append(CR_LF).append(raw).append(CR_LF);
        return this;
    }
}
