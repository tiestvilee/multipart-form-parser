package com.springernature.multipartform;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UploadFileTest {

    public static final String CR_LF = "\r\n";

    @Test
    public void testEmptyContents() throws Exception {
        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary).build());

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testEmptyFile() throws Exception {
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
            .file("aFile", "", "application/octet-stream", "").build());

        assertThereAreMoreParts(form);
        assertThereAreMoreParts(form);

        form.next();

        assertThereAreNoMoreParts(form);
        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testEmptyField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("aField", "").build());

        assertFieldPart(form, "aField", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testSmallFile() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        assertFilePart(form, "aFile", "file.name", "application/octet-stream", "File contents here");

        assertThereAreNoMoreParts(form);
    }


    @Test
    public void testSmallField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("aField", "Here is the value of the field\n").build());

        assertFieldPart(form, "aField", "Here is the value of the field\n");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testFileUpload() throws Exception {

        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary,
            new ValidMultipartFormBuilder(boundary)
                .file("file", "foo.tab", "text/whatever", "This is the content of the file\n")
                .field("field", "fieldValue")
                .field("multi", "value1")
                .field("multi", "value2")
                .build());

        assertFilePart(form, "file", "foo.tab", "text/whatever", "This is the content of the file\n");
        assertFieldPart(form, "field", "fieldValue");
        assertFieldPart(form, "multi", "value1");
        assertFieldPart(form, "multi", "value2");

        assertThereAreNoMoreParts(form);
    }

    private MultipartFormParts getMultipartFormParts(String boundary, String multipartFormContents) throws IOException {
        InputStream multipartFormContentsStream = new ByteArrayInputStream(multipartFormContents.getBytes(Charset.forName("UTF-8")));
        return MultipartFormParts.parse(boundary, multipartFormContentsStream);
    }

    private void assertFilePart(MultipartFormParts form, String fieldName, String fileName, String contentType, String contents) throws IOException {
        assertThereAreMoreParts(form);
        Part file = form.next();
        assertThat(file.getFieldName(), equalTo(fieldName));
        assertPartIsNotField(file);
        assertThat(file.getContentType(), equalTo(contentType));
        assertThat(file.getFileName(), equalTo(fileName));
        assertThat(file.getContentsAsString(), equalTo(contents));
    }

    private void assertFieldPart(MultipartFormParts form, String fieldName, String fieldValue) throws IOException {
        assertThereAreMoreParts(form);
        Part field = form.next();
        assertThat(field.getFieldName(), equalTo(fieldName));
        assertPartIsFormField(field);
        assertThat(field.getContentsAsString(), equalTo(fieldValue));
    }

    private void assertThereAreNoMoreParts(MultipartFormParts form) {
        assertFalse("Too many parts", form.hasNext());
    }

    private void assertThereAreMoreParts(MultipartFormParts form) {
        assertTrue("Not enough parts", form.hasNext());
    }

    private void assertPartIsFormField(Part field) {
        assertTrue("the part is a form field", field.isFormField());
    }

    private void assertPartIsNotField(Part file) {
        assertFalse("the part is not a form field", file.isFormField());
    }
}
