package com.springernature.multipartform;

import java.io.ByteArrayInputStream;

public class InMemoryPart extends Part {
    public final String content;

    InMemoryPart(Part original, String content) {
        super(original.fieldName, original.formField, original.contentType, original.fileName, new ByteArrayInputStream(content.getBytes()), original.headers);

        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
