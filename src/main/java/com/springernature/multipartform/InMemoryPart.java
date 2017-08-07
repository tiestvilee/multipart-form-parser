package com.springernature.multipartform;

import java.nio.charset.Charset;

public class InMemoryPart extends Part {
    private final byte[] bytes; // not immutable
    public final String content;

    InMemoryPart(Part original, byte[] bytes, Charset encoding) {
        super(original.fieldName, original.formField, original.contentType, original.fileName, original.headers);

        this.bytes = bytes;
        this.content = new String(bytes, encoding); // double the memory... bad?
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getContent() {
        return content;
    }
}
