package com.springernature.multipartform;

import org.junit.Ignore;
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
        MultipartFormParts form = getMultipartFormParts("-----1234", "-----1234" + CR_LF +
            "Content-Disposition: form-data; name=\"file\"; filename=\"\"" + CR_LF +
            CR_LF +
            CR_LF +
            "-----1234--" + CR_LF);

        assertThereAreMoreParts(form);

        Part file = form.next();
        assertPartIsNotField(file);
//        assertEquals("", file.getString());
        assertThat(file.getName(), equalTo(""));

        assertThereAreNoMoreParts(form);
    }

    private MultipartFormParts getMultipartFormParts(String boundary, String multipartFormContents) throws IOException {
        InputStream multipartFormContentsStream = new ByteArrayInputStream(multipartFormContents.getBytes(Charset.forName("UTF-8")));
        return MultipartFormParts.parse(boundary, multipartFormContentsStream);
    }

    @Test
    @Ignore
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
            assertThat(file.getName(), equalTo("foo.tab"));
//        assertEquals("This is the content of the file\n", file.getString());
        }

        {
            assertThereAreMoreParts(form);
            Part field = form.next();
            assertThat(field.getFieldName(), equalTo("file"));
            assertPartIsFormField(field);
//            assertEquals("fieldValue", field.getString());
        }
        {
            assertThereAreMoreParts(form);
            Part multi_1 = form.next();
            assertThat(multi_1.getFieldName(), equalTo("file"));
            assertPartIsFormField(multi_1);
//            assertEquals("value1", multi0.getString());
        }
        {
            assertThereAreMoreParts(form);
            Part multi_2 = form.next();
            assertThat(multi_2.getFieldName(), equalTo("file"));
            assertPartIsFormField(multi_2);
//            assertEquals("value2", multi1.getString());
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
        assertTrue("the is a form field", field.isFormField());
    }

    private void assertPartIsNotField(Part file) {
        assertFalse("the file is not a form field", file.isFormField());
    }
}
