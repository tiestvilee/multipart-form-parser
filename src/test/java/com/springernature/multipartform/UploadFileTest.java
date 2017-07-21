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
            .file("aFile", "", "application/octet-stream", "").build());

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

    /*
    @Test
    public void testFILEUPLOAD62() throws Exception {
        final String contentType = "multipart/form-data; boundary=AaB03x";
        final String request =
            "--AaB03x\r\n" +
            "content-disposition: form-data; name=\"field1\"\r\n" +
            "\r\n" +
            "Joe Blow\r\n" +
            "--AaB03x\r\n" +
            "content-disposition: form-data; name=\"pics\"\r\n" +
            "Content-type: multipart/mixed; boundary=BbC04y\r\n" +
            "\r\n" +
            "--BbC04y\r\n" +
            "Content-disposition: attachment; filename=\"file1.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "... contents of file1.txt ...\r\n" +
            "--BbC04y\r\n" +
            "Content-disposition: attachment; filename=\"file2.gif\"\r\n" +
            "Content-type: image/gif\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "\r\n" +
            "...contents of file2.gif...\r\n" +
            "--BbC04y--\r\n" +
            "--AaB03x--";
        List<FileItem> fileItems = Util.parseUpload(upload, request.getBytes("US-ASCII"), contentType);
        assertEquals(3, fileItems.size());
        FileItem item0 = fileItems.get(0);
        assertEquals("field1", item0.getFieldName());
        assertNull(item0.getName());
        assertEquals("Joe Blow", new String(item0.get()));
        FileItem item1 = fileItems.get(1);
        assertEquals("pics", item1.getFieldName());
        assertEquals("file1.txt", item1.getName());
        assertEquals("... contents of file1.txt ...", new String(item1.get()));
        FileItem item2 = fileItems.get(2);
        assertEquals("pics", item2.getFieldName());
        assertEquals("file2.gif", item2.getName());
        assertEquals("...contents of file2.gif...", new String(item2.get()));
    }
     */

    @Test
    public void uploadMultipartFormWithSubParts() throws Exception {

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
