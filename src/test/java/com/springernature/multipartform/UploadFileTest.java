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
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + "--" + CR_LF);

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testEmptyFile() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aFile\"; filename=\"\"" + CR_LF +
            "Content-Type: application/octet-stream" + CR_LF +
            CR_LF +
            CR_LF +
            boundary + "--" + CR_LF);

        assertFilePart(form, "aFile", "", "application/octet-stream", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void hasNextIsIdempotent() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aFile\"; filename=\"\"" + CR_LF +
            "Content-Type: application/octet-stream" + CR_LF +
            CR_LF +
            CR_LF +
            boundary + "--" + CR_LF);

        assertThereAreMoreParts(form);
        assertThereAreMoreParts(form);

        form.next();

        assertThereAreNoMoreParts(form);
        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testEmptyField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aField\"" + CR_LF +
            CR_LF +
            CR_LF +
            boundary + "--" + CR_LF);

        assertFieldPart(form, "aField", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testSmallFile() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aFile\"; filename=\"file.name\"" + CR_LF +
            "Content-Type: application/octet-stream" + CR_LF +
            CR_LF +
            "File contents here\n" + CR_LF +
            boundary + "--" + CR_LF);

        assertFilePart(form, "aFile", "file.name", "application/octet-stream", "File contents here\n");

        assertThereAreNoMoreParts(form);
    }


    @Test
    public void testSmallField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aField\"" + CR_LF +
            CR_LF +
            "Here is the value of the field\n" + CR_LF +
            boundary + "--" + CR_LF);

        assertFieldPart(form, "aField", "Here is the value of the field\n");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testFileUpload() throws Exception {

        String boundary = "-----1234";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"" + CR_LF +
            "Content-Type: text/whatever" + CR_LF +
            CR_LF +
            "This is the content of the file\n" +
            CR_LF +
            boundary + CR_LF +
            "Content-Disposition: form-data; name=\"field\"" + CR_LF +
            CR_LF +
            "fieldValue" + CR_LF +
            boundary + CR_LF +
            "Content-Disposition: form-data; name=\"multi\"" + CR_LF +
            CR_LF +
            "value1" + CR_LF +
            boundary + CR_LF +
            "Content-Disposition: form-data; name=\"multi\"" + CR_LF +
            CR_LF +
            "value2" + CR_LF +
            boundary + "--" + CR_LF);

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
