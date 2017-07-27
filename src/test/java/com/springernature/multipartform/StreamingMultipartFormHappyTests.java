package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.AlreadyClosedException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import static com.googlecode.totallylazy.Pair.pair;
import static com.googlecode.totallylazy.Sequences.sequence;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class StreamingMultipartFormHappyTests {

    public static final String CR_LF = "\r\n";

    @Test
    public void uploadEmptyContents() throws Exception {
        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary).build());

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadEmptyFile() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "", "doesnt/matter", "").build());

        assertFilePart(form, "aFile", "", "doesnt/matter", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void hasNextIsIdempotent() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "", "application/octet-stream", "")
            .file("anotherFile", "", "application/octet-stream", "").build());

        assertThereAreMoreParts(form);
        assertThereAreMoreParts(form);

        form.next();

        assertThereAreMoreParts(form);
        assertThereAreMoreParts(form);

        form.next();

        assertThereAreNoMoreParts(form);
        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadEmptyField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("aField", "").build());

        assertFieldPart(form, "aField", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadSmallFile() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        assertFilePart(form, "aFile", "file.name", "application/octet-stream", "File contents here");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadSmallFileAsAttachment() throws Exception {
        String boundary = "-----4567";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("beforeFile", "before.txt", "application/json", "[]")
            .startMultipart("multipartFieldName", "7890")
            .attachment("during.txt", "plain/text", "Attachment contents here")
            .attachment("during2.txt", "plain/text", "More text here")
            .endMultipart()
            .file("afterFile", "after.txt", "application/json", "[]")
            .build());

        assertFilePart(form, "beforeFile", "before.txt", "application/json", "[]");
        assertFilePart(form, "multipartFieldName", "during.txt", "plain/text", "Attachment contents here");
        assertFilePart(form, "multipartFieldName", "during2.txt", "plain/text", "More text here");
        assertFilePart(form, "afterFile", "after.txt", "application/json", "[]");

        assertThereAreNoMoreParts(form);
    }


    @Test
    public void uploadSmallField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("aField", "Here is the value of the field\n").build());

        assertFieldPart(form, "aField", "Here is the value of the field\n");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadMultipleFilesAndFields() throws Exception {
        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary,
            new ValidMultipartFormBuilder(boundary)
                .file("file", "foo.tab", "text/whatever", "This is the content of the file\n")
                .field("field", "fieldValue")
                .field("multi", "value1")
                .file("anotherFile", "BAR.tab", "text/something", "This is another file\n")
                .field("multi", "value2")
                .build());

        assertFilePart(form, "file", "foo.tab", "text/whatever", "This is the content of the file\n");
        assertFieldPart(form, "field", "fieldValue");
        assertFieldPart(form, "multi", "value1");
        assertFilePart(form, "anotherFile", "BAR.tab", "text/something", "This is another file\n");
        assertFieldPart(form, "multi", "value2");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadFieldsWithMultilineHeaders() throws Exception {
        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary,
            new ValidMultipartFormBuilder(boundary)
                .rawPart(
                    "Content-Disposition: form-data; \r\n" +
                        "\tname=\"field\"\r\n" +
                        "\r\n" +
                        "fieldValue")
                .rawPart(
                    "Content-Disposition: form-data;\r\n" +
                        "     name=\"multi\"\r\n" +
                        "\r\n" +
                        "value1")
                .field("multi", "value2")
                .build());

        assertFieldPart(form, "field", "fieldValue");
        assertFieldPart(form, "multi", "value1");
        assertFieldPart(form, "multi", "value2");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void partsCanHaveLotsOfHeaders() throws Exception {
        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary,
            new ValidMultipartFormBuilder(boundary)
                .part("This is the content of the file\n",
                    pair("Content-Disposition", sequence(pair("form-data", null), pair("name", "fileFieldName"), pair("filename", "filename.txt"))),
                    pair("Content-Type", sequence(pair("plain/text", null))),
                    pair("Some-header", sequence(pair("some value", null))))
                .part("This is the content of the field\n",
                    pair("Content-Disposition", sequence(pair("form-data", null), pair("name", "fieldFieldName"))),
                    pair("Another-header", sequence(pair("some-key", "some-value")))
                )
                .build());

        Part file = assertFilePart(form, "fileFieldName", "filename.txt", "plain/text", "This is the content of the file\n");

        Map<String, String> fileHeaders = file.getHeaders();
        assertThat(fileHeaders.size(), equalTo(3));
        assertThat(fileHeaders.get("Content-Disposition"), equalTo("form-data; name=\"fileFieldName\"; filename=\"filename.txt\""));
        assertThat(fileHeaders.get("Content-Type"), equalTo("plain/text"));
        assertThat(fileHeaders.get("Some-header"), equalTo("some value"));

        Part field = assertFieldPart(form, "fieldFieldName", "This is the content of the field\n");

        Map<String, String> fieldHeaders = field.getHeaders();
        assertThat(fieldHeaders.size(), equalTo(2));
        assertThat(fieldHeaders.get("Content-Disposition"), equalTo("form-data; name=\"fieldFieldName\""));
        assertThat(fieldHeaders.get("Another-header"), equalTo("some-key=\"some-value\""));

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void closedPartsCannotBeReadFrom() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        Part file = form.next();

        while (file.read() > 0) {
            // keep reading.
        }

        assertThat(file.read(), equalTo(-1));
        file.close();
        file.close(); // can close multiple times
        try {
            int ignored = file.read();
            fail("Should have complained that the part has been closed " + ignored);
        } catch (AlreadyClosedException e) {
            // pass
        }
    }

    @Test
    public void readingPartsContentsAsStringClosesStream() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        Part file = form.next();
        file.getContentsAsString();

        try {
            int ignored = file.read();
            fail("Should have complained that the part has been closed " + ignored);
        } catch (AlreadyClosedException e) {
            // pass
        }

        file.close(); // can close multiple times
    }

    @Test
    public void gettingNextPartClosesOldPart() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build());

        Part file1 = form.next();

        Part file2 = form.next();

        assertThat(file1, not(equalTo(file2)));

        try {
            int ignored = file1.read();
            fail("Should have complained that the part has been closed " + ignored);
        } catch (AlreadyClosedException e) {
            // pass
        }

        file1.close(); // can close multiple times

        assertThat(file2.getContentsAsString(), equalTo("Different file contents here"));
    }

    static MultipartFormParts getMultipartFormParts(String boundary, byte[] multipartFormContents) throws IOException {
        return getMultipartFormParts(boundary.getBytes(Charset.forName("UTF-8")), multipartFormContents, Charset.forName("UTF-8"));
    }

    static MultipartFormParts getMultipartFormParts(byte[] boundary, byte[] multipartFormContents, Charset encoding) throws IOException {
        InputStream multipartFormContentsStream = new ByteArrayInputStream(multipartFormContents);
        return MultipartFormParts.parse(boundary, multipartFormContentsStream, encoding);
    }

    static Part assertFilePart(MultipartFormParts form, String fieldName, String fileName, String contentType, String contents) throws IOException {
        assertThereAreMoreParts(form);
        Part file = form.next();
        assertThat("file name", file.getFileName(), equalTo(fileName));
        assertThat("content type", file.getContentType(), equalTo(contentType));
        assertPartIsNotField(file);
        assertPart(fieldName, contents, file);
        return file;
    }

    static Part assertFieldPart(MultipartFormParts form, String fieldName, String fieldValue) throws IOException {
        assertThereAreMoreParts(form);
        Part field = form.next();
        assertPartIsFormField(field);
        assertPart(fieldName, fieldValue, field);
        return field;
    }

    static void assertPart(String fieldName, String fieldValue, Part part) throws IOException {
        assertThat("field name", part.getFieldName(), equalTo(fieldName));
        assertThat("contents", part.getContentsAsString(), equalTo(fieldValue));
    }

    static void assertThereAreNoMoreParts(MultipartFormParts form) {
        assertFalse("Too many parts", form.hasNext());
    }

    static void assertThereAreMoreParts(MultipartFormParts form) {
        assertTrue("Not enough parts", form.hasNext());
    }

    static void assertPartIsFormField(Part field) {
        assertTrue("the part is a form field", field.isFormField());
    }

    static void assertPartIsNotField(Part file) {
        assertFalse("the part is not a form field", file.isFormField());
    }
}
