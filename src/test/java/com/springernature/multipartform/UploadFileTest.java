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

        assertThereAreMoreParts(form);

        Part file = form.next();
        assertPartIsNotField(file);
        assertThat(file.getContentsAsString(), equalTo(""));
        assertThat(file.getFieldName(), equalTo("aFile"));

        assertThereAreNoMoreParts(form);
    }


    /*
------WebKitFormBoundaryTYNvmYSrpPPAI5OJ
Content-Disposition: form-data; name="articleType"

review
------WebKitFormBoundaryTYNvmYSrpPPAI5OJ
Content-Disposition: form-data; name="uploadManuscript"; filename=""
Content-Type: application/octet-stream


------WebKitFormBoundaryTYNvmYSrpPPAI5OJ--

     */

    @Test
    public void testEmptyField() throws Exception {
        String boundary = "-----3456";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aField\"" + CR_LF +
            CR_LF +
            CR_LF +
            boundary + "--" + CR_LF);

        assertThereAreMoreParts(form);

        Part field = form.next();
        assertPartIsFormField(field);
        assertThat(field.getContentsAsString(), equalTo(""));
        assertThat(field.getFieldName(), equalTo("aField"));

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void testSmallFile() throws Exception {
        String boundary = "-----2345";
        MultipartFormParts form = getMultipartFormParts(boundary, boundary + CR_LF +
            "Content-Disposition: form-data; name=\"aFile\"; filename=\"\"" + CR_LF +
            "Content-Type: application/octet-stream" + CR_LF +
            CR_LF +
            "File contents here\n" + CR_LF +
            boundary + "--" + CR_LF);

        assertThereAreMoreParts(form);

        Part file = form.next();
        assertPartIsNotField(file);
        assertThat(file.getContentsAsString(), equalTo("File contents here\n"));
        assertThat(file.getFieldName(), equalTo("aFile"));

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

        assertThereAreMoreParts(form);

        Part field = form.next();
        assertPartIsFormField(field);
        assertThat(field.getContentsAsString(), equalTo("Here is the value of the field\n"));
        assertThat(field.getFieldName(), equalTo("aField"));

        assertThereAreNoMoreParts(form);
    }

    private MultipartFormParts getMultipartFormParts(String boundary, String multipartFormContents) throws IOException {
        InputStream multipartFormContentsStream = new ByteArrayInputStream(multipartFormContents.getBytes(Charset.forName("UTF-8")));
        return MultipartFormParts.parse(boundary, multipartFormContentsStream);
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

        {
            assertThereAreMoreParts(form);
            Part file = form.next();
            assertThat(file.getFieldName(), equalTo("file"));
            assertPartIsNotField(file);
            assertThat(file.getContentType(), equalTo("text/whatever"));
            assertThat(file.getFileName(), equalTo("foo.tab"));
            assertThat(file.getContentsAsString(), equalTo("This is the content of the file\n"));
        }
        {
            assertThereAreMoreParts(form);
            Part field = form.next();
            assertThat(field.getFieldName(), equalTo("field"));
            assertPartIsFormField(field);
            assertThat(field.getContentsAsString(), equalTo("fieldValue"));
        }
        {
            assertThereAreMoreParts(form);
            Part multi_1 = form.next();
            assertThat(multi_1.getFieldName(), equalTo("multi"));
            assertPartIsFormField(multi_1);
            assertThat(multi_1.getContentsAsString(), equalTo("value1"));
        }
        {
            assertThereAreMoreParts(form);
            Part multi_2 = form.next();
            assertThat(multi_2.getFieldName(), equalTo("multi"));
            assertPartIsFormField(multi_2);
            assertThat(multi_2.getContentsAsString(), equalTo("value2"));
        }
        assertThereAreNoMoreParts(form);
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
