package com.springernature.multipartform;

import java.nio.charset.Charset;

public class DiskBackedPart extends Part {

    public DiskBackedPart(Part original, byte[] result, Charset encoding) {
        super(original.fieldName, original.formField, original.contentType, original.fileName, original.headers);

//        this.bytes = bytes;
//        this.content = new String(bytes, encoding); // double the memory... bad?
    }
}
