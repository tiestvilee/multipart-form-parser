package org.tiestvilee.multipartform;

import org.junit.Test;
import org.tiestvilee.multipartform.exceptions.ParseError;
import org.tiestvilee.multipartform.exceptions.TokenNotFoundException;
import org.tiestvilee.multipartform.part.StreamingPart;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.tiestvilee.multipartform.StreamingMultipartFormParts.HEADER_SIZE_MAX;
import static org.tiestvilee.multipartform.ValidMultipartFormBuilder.pair;

public class StreamingMultipartFormSadTests {

    @Test
    public void failsWhenNoBoundaryInStream() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, "No boundary anywhere".getBytes());

        assertParseErrorWrapsTokenNotFound(form, "Boundary not found <<-----1234>>");

        form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, "No boundary anywhere".getBytes());

        try {
            form.next();
            fail("Should have thrown ParseError");
        } catch (ParseError e) {
            assertThat(e.getCause().getClass(), equalTo(TokenNotFoundException.class));
            assertThat(e.getCause().getMessage(), equalTo("Boundary not found <<-----1234>>"));
        }

    }

    @Test
    public void failsWhenGettingNextPastEndOfParts() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build());

        form.next(); // aFile
        form.next(); // anotherFile
        try {
            form.next(); // no such element
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void failsWhenGettingNextPastEndOfPartsAfterHasNext() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build());

        form.next(); // aFile
        form.next(); // anotherFile
        assertThat(form.hasNext(), equalTo(false));
        try {
            form.next(); // no such element
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void partHasNoHeaders() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("multi", "value0")
            .rawPart("" + StreamingMultipartFormHappyTests.CR_LF + "value with no headers")
            .field("multi", "value2")
            .build());

        form.next();
        StreamingPart StreamingPart = form.next();
        assertThat(StreamingPart.getFieldName(), nullValue());
        assertThat(StreamingPart.getContentsAsString(), equalTo("value with no headers"));
        assertThat(StreamingPart.getHeaders().size(), equalTo(0));
        assertThat(StreamingPart.isFormField(), equalTo(true));
        assertThat(StreamingPart.getFileName(), nullValue());
        form.next();
    }

    @Test
    public void overwritesPartHeaderIfHeaderIsRepeated() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .part("contents of StreamingPart",
                pair("Content-Disposition", asList(pair("form-data", null), pair("bit", "first"), pair("name", "first-name"))),
                pair("Content-Disposition", asList(pair("form-data", null), pair("bot", "second"), pair("name", "second-name"))))
            .build());

        StreamingPart StreamingPart = form.next();
        assertThat(StreamingPart.getFieldName(), equalTo("second-name"));
        assertThat(StreamingPart.getHeaders().get("Content-Disposition"),
            equalTo("form-data; bot=\"second\"; name=\"second-name\""));
    }

    @Test
    public void failsIfFoundBoundaryButNoFieldSeparator() throws Exception {
        String boundary = "-----2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + // no CR_LF
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        assertParseErrorWrapsTokenNotFound(form, "Boundary must be followed by field separator, but didn't find it");
    }

    @Test
    public void failsIfHeaderMissingFieldSeparator() throws Exception {
        String boundary = "-----2345";

        assertParseError(StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + // no CR_LF
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes()), "Header didn't include a colon <<value>>");


        assertParseError(StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            // no CR_LF
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes()), "Header didn't include a colon <<value>>");
    }

    @Test
    public void failsIfContentsMissingFieldSeparator() throws Exception {
        String boundary = "-----2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + // no CR_LF
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        form.next();
        // StreamingPart's content stream hasn't been closed
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it");
    }

    @Test
    public void failsIfContentsMissingFieldSeparatorAndHasReadToEndOfContent() throws Exception {
        String boundary = "-----2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + // no CR_LF
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        StreamingPart StreamingPart = form.next();
        StreamingPart.getContentsAsString();
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it");
    }

    @Test
    public void failsIfClosingBoundaryIsMissingFieldSeparator() throws Exception {
        String boundary = "-----2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--").getBytes()); // no CR_LF

        form.next();
        assertParseErrorWrapsTokenNotFound(form, "Stream terminator must be followed by field separator, but didn't find it");
    }

    @Test
    public void failsIfClosingBoundaryIsMissing() throws Exception {
        String boundary = "-----2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        form.next();
        assertParseErrorWrapsTokenNotFound(form, "Didn't find Token <<\r\n>>. Last 2 bytes read were <<>>");
    }

    @Test
    public void failsIfHeadingTooLong() throws Exception {
        String boundary = "-----2345";

        char[] chars = new char[HEADER_SIZE_MAX];
        Arrays.fill(chars, 'x');
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", new String(chars), "application/octet-stream", "File contents here").build());

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Token <<\r\n>> within 10240 bytes");
    }

    @Test
    public void failsIfTooManyHeadings() throws Exception {
        String boundary = "-----2345";

        char[] chars = new char[1024];
        Arrays.fill(chars, 'x');
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .part("some contents",
                pair("Content-Disposition", asList(pair("form-data", null), pair("name", "fieldName"), pair("filename", "filename"))),
                pair("Content-Type", asList(pair("text/plain", null))),
                pair("extra-1", asList(pair(new String(chars), null))),
                pair("extra-2", asList(pair(new String(chars), null))),
                pair("extra-3", asList(pair(new String(chars), null))),
                pair("extra-4", asList(pair(new String(chars), null))),
                pair("extra-5", asList(pair(new String(chars), null))),
                pair("extra-6", asList(pair(new String(chars), null))),
                pair("extra-7", asList(pair(new String(chars), null))),
                pair("extra-8", asList(pair(new String(chars), null))),
                pair("extra-9", asList(pair(new String(chars), null))),
                pair("extra-10", asList(pair(new String(chars, 0, 816), null))) // header section exactly 10240 bytes big!
            ).build());

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Header section within 10240 bytes");
    }

    private void assertParseErrorWrapsTokenNotFound(Iterator<StreamingPart> form, String errorMessage) {
        try {
            form.hasNext();
            fail("Should have thrown a parse error");
        } catch (ParseError e) {
            assertThat(e.getCause().getClass(), equalTo(TokenNotFoundException.class));
            assertThat(e.getCause().getMessage(), equalTo(errorMessage));
        }
    }

    private void assertParseError(Iterator<StreamingPart> form, String errorMessage) {
        try {
            form.hasNext(); // will hit missing \r\n
            fail("Should have thrown a parse error");
        } catch (ParseError e) {
            assertThat(e.getMessage(), equalTo(errorMessage));
        }
    }


}
