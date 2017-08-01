package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.ParseError;
import com.springernature.multipartform.exceptions.TokenNotFoundException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.springernature.multipartform.StreamingMultipartFormHappyTests.CR_LF;
import static com.springernature.multipartform.StreamingMultipartFormHappyTests.getMultipartFormParts;
import static com.springernature.multipartform.StreamingMultipartFormParts.HEADER_SIZE_MAX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class StreamingMultipartFormSadTests {

    @Test
    public void failsWhenNoBoundaryInStream() throws Exception {
        String boundary = "-----1234";
        Iterator<Part> form = getMultipartFormParts(boundary, "No boundary anywhere".getBytes());

        assertParseErrorWrapsTokenNotFound(form, "Boundary not found <<-----1234>>");

        form = getMultipartFormParts(boundary, "No boundary anywhere".getBytes());

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
        Iterator<Part> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
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
        Iterator<Part> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
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
        Iterator<Part> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("multi", "value0")
            .rawPart("" + CR_LF + "value with no headers")
            .field("multi", "value2")
            .build());

        form.next();
        Part part = form.next();
        assertThat(part.getFieldName(), nullValue());
        assertThat(part.getContentsAsString(), equalTo("value with no headers"));
        assertThat(part.getHeaders().size(), equalTo(0));
        assertThat(part.isFormField(), equalTo(true));
        assertThat(part.getFileName(), nullValue());
        form.next();
    }

    @Test
    public void overwritesPartHeaderIfHeaderIsRepeated() throws Exception {
        String boundary = "-----2345";
        Iterator<Part> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .part("contents of part",
                pair("Content-Disposition", sequence(pair("form-data", null), pair("bit", "first"), pair("name", "first-name"))),
                pair("Content-Disposition", sequence(pair("form-data", null), pair("bot", "second"), pair("name", "second-name"))))
            .build());

        Part part = form.next();
        assertThat(part.getFieldName(), equalTo("second-name"));
        assertThat(part.getHeaders().get("Content-Disposition"),
            equalTo("form-data; bot=\"second\"; name=\"second-name\""));
    }

    @Test
    public void failsIfFoundBoundaryButNoFieldSeparator() throws Exception {
        String boundary = "-----2345";

        Iterator<Part> form = getMultipartFormParts(boundary, ("-----2345" + // no CR_LF
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345--" + CR_LF).getBytes());

        assertParseErrorWrapsTokenNotFound(form, "Boundary must be followed by field separator, but didn't find it");
    }

    @Test
    public void failsIfHeaderMissingFieldSeparator() throws Exception {
        String boundary = "-----2345";

        assertParseError(getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + // no CR_LF
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345--" + CR_LF).getBytes()), "Header didn't include a colon <<value>>");


        assertParseError(getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            // no CR_LF
            "value" + CR_LF +
            "-----2345--" + CR_LF).getBytes()), "Header didn't include a colon <<value>>");
    }

    @Test
    public void failsIfContentsMissingFieldSeparator() throws Exception {
        String boundary = "-----2345";

        Iterator<Part> form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + // no CR_LF
            "-----2345--" + CR_LF).getBytes());

        form.next();
        // part's content stream hasn't been closed
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it");
    }

    @Test
    public void failsIfContentsMissingFieldSeparatorAndHasReadToEndOfContent() throws Exception {
        String boundary = "-----2345";

        Iterator<Part> form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + // no CR_LF
            "-----2345--" + CR_LF).getBytes());

        Part part = form.next();
        part.getContentsAsString();
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it");
    }

    @Test
    public void failsIfClosingBoundaryIsMissingFieldSeparator() throws Exception {
        String boundary = "-----2345";

        Iterator<Part> form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345--").getBytes()); // no CR_LF

        form.next();
        assertParseErrorWrapsTokenNotFound(form, "Stream terminator must be followed by field separator, but didn't find it");
    }

    @Test
    public void failsIfClosingBoundaryIsMissing() throws Exception {
        String boundary = "-----2345";

        Iterator<Part> form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345" + CR_LF).getBytes());

        form.next();
        assertParseErrorWrapsTokenNotFound(form, "Didn't find Token <<\r\n>>. Last 2 bytes read were <<>>");
    }

    @Test
    public void failsIfHeadingTooLong() throws Exception {
        String boundary = "-----2345";

        char[] chars = new char[HEADER_SIZE_MAX];
        Arrays.fill(chars, 'x');
        Iterator<Part> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", new String(chars), "application/octet-stream", "File contents here").build());

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Token <<\r\n>> within 10240 bytes");
    }

    @Test
    public void failsIfTooManyHeadings() throws Exception {
        String boundary = "-----2345";

        char[] chars = new char[1024];
        Arrays.fill(chars, 'x');
        Iterator<Part> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .part("some contents",
                pair("Content-Disposition", sequence(pair("form-data", null), pair("name", "fieldName"), pair("filename", "filename"))),
                pair("Content-Type", sequence(pair("text/plain", null))),
                pair("extra-1", sequence(pair(new String(chars), null))),
                pair("extra-2", sequence(pair(new String(chars), null))),
                pair("extra-3", sequence(pair(new String(chars), null))),
                pair("extra-4", sequence(pair(new String(chars), null))),
                pair("extra-5", sequence(pair(new String(chars), null))),
                pair("extra-6", sequence(pair(new String(chars), null))),
                pair("extra-7", sequence(pair(new String(chars), null))),
                pair("extra-8", sequence(pair(new String(chars), null))),
                pair("extra-9", sequence(pair(new String(chars), null))),
                pair("extra-10", sequence(pair(new String(chars, 0, 816), null))) // header section exactly 10240 bytes big!
            ).build());

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Token <<\r\n>> within 820 bytes");
    }

    private void assertParseErrorWrapsTokenNotFound(Iterator<Part> form, String errorMessage) {
        try {
            form.hasNext();
            fail("Should have thrown a parse error");
        } catch (ParseError e) {
            assertThat(e.getCause().getClass(), equalTo(TokenNotFoundException.class));
            assertThat(e.getCause().getMessage(), equalTo(errorMessage));
        }
    }

    private void assertParseError(Iterator<Part> form, String errorMessage) {
        try {
            form.hasNext(); // will hit missing \r\n
            fail("Should have thrown a parse error");
        } catch (ParseError e) {
            assertThat(e.getMessage(), equalTo(errorMessage));
        }
    }


}
