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

    private MultipartFormParts getMultipartFormParts(String boundary, String multipartFormContents) throws IOException {
        InputStream multipartFormContentsStream = new ByteArrayInputStream(multipartFormContents.getBytes(Charset.forName("UTF-8")));
        return MultipartFormParts.parse(boundary, multipartFormContentsStream);
    }

    private Part assertFilePart(MultipartFormParts form, String fieldName, String fileName, String contentType, String contents) throws IOException {
        assertThereAreMoreParts(form);
        Part file = form.next();
        assertThat("file name", file.getFileName(), equalTo(fileName));
        assertThat("content type", file.getContentType(), equalTo(contentType));
        assertPartIsNotField(file);
        assertPart(fieldName, contents, file);
        return file;
    }

    private Part assertFieldPart(MultipartFormParts form, String fieldName, String fieldValue) throws IOException {
        assertThereAreMoreParts(form);
        Part field = form.next();
        assertPartIsFormField(field);
        assertPart(fieldName, fieldValue, field);
        return field;
    }

    private void assertPart(String fieldName, String fieldValue, Part part) throws IOException {
        assertThat("field name", part.getFieldName(), equalTo(fieldName));
        assertThat("contents", part.getContentsAsString(), equalTo(fieldValue));
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
