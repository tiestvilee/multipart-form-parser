package com.springernature.multipartform;

import java.nio.charset.Charset;

public class InMemoryPart extends Part {
    public final byte[] bytes;
    public final String content;

    InMemoryPart(Part original, byte[] bytes, Charset encoding) {
        super(original.fieldName, original.formField, original.contentType, original.fileName, null, original.headers);

        this.bytes = bytes;
        this.content = new String(bytes, encoding);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getContent() {
        return content;
    }
}
