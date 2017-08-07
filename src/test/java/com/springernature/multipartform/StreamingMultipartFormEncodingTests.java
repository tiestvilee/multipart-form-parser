package com.springernature.multipartform;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static com.springernature.multipartform.StreamingMultipartFormHappyTests.*;

public class StreamingMultipartFormEncodingTests {

    @Test
    public void uploadUTF8() throws Exception {
        testForm(constructForm(StandardCharsets.UTF_8), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_8);
    }

    @Test
    public void uploadISO_8859_1() throws Exception {
        testForm(constructForm(StandardCharsets.ISO_8859_1), "\u00E9", "?", StandardCharsets.ISO_8859_1);
    }

    @Test
    public void uploadUTF_16BE() throws Exception {
        testForm(constructForm(StandardCharsets.UTF_16BE), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_16BE);
    }

    @Test
    public void uploadUTF_16LE() throws Exception {
        testForm(constructForm(StandardCharsets.UTF_16LE), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_16LE);
    }

    @Test
    public void uploadUS_ASCII() throws Exception {
        testForm(constructForm(StandardCharsets.US_ASCII), "?", "?", StandardCharsets.US_ASCII);
    }

    private void testForm(Iterator<StreamingPart> form, String simpleChar, String complexChar, Charset encoding) throws IOException {
        assertFilePart(form, "file", "foo.tab" + complexChar, "text/whatever" + simpleChar + complexChar, "This is the content of the file" + simpleChar + complexChar, encoding);
        assertFieldPart(form, "field" + complexChar, "fieldValue" + simpleChar + complexChar, encoding);
        assertFieldPart(form, "multi", "value1" + simpleChar, encoding);
        assertFilePart(form, "anotherFile", "BAR.tab", "text/something" + simpleChar, "This is another file" + simpleChar, encoding);
        assertFieldPart(form, "multi", "value2" + simpleChar, encoding);

        assertThereAreNoMoreParts(form);
    }

    private Iterator<StreamingPart> constructForm(Charset encoding) throws IOException {
        String boundary = "-----\u00E91234\uD83D\uDCA9";
        byte[] boundaryBytes = boundary.getBytes(encoding);
        return getMultipartFormParts(boundaryBytes,
            new ValidMultipartFormBuilder(boundaryBytes, encoding)
                .file("file", "foo.tab\uD83D\uDCA9", "text/whatever\u00E9\uD83D\uDCA9", "This is the content of the file\u00E9\uD83D\uDCA9")
                .field("field\uD83D\uDCA9", "fieldValue\u00E9\uD83D\uDCA9")
                .field("multi", "value1\u00E9")
                .file("anotherFile", "BAR.tab", "text/something\u00E9", "This is another file\u00E9")
                .field("multi", "value2\u00E9")
                .build(), encoding);
    }
}
