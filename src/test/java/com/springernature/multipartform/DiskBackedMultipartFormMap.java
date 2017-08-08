package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.StreamTooLongException;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.Map;

import static com.springernature.multipartform.StreamingMultipartFormHappyTests.CR_LF;
import static com.springernature.multipartform.StreamingMultipartFormHappyTests.compareStreamToFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

public class DiskBackedMultipartFormMap {
    public static final File TEMPORARY_FILE_DIRECTORY = new File("./out/tmp");

    static {
        TEMPORARY_FILE_DIRECTORY.mkdirs();
    }

    /*

    delete file when done
     - File.deleteOnExit
     - Closing try/catch




     */


    @Test
    public void uploadMultipleFilesAndFields() throws Exception {
        String boundary = "-----1234";
        InputStream multipartFormContentsStream = new ByteArrayInputStream(new ValidMultipartFormBuilder(boundary)
            .file("file", "foo.tab", "text/whatever", "This is the content of the file\n")
            .field("field", "fieldValue" + CR_LF + "with cr lf")
            .field("multi", "value1")
            .file("anotherFile", "BAR.tab", "text/something", "This is another file\n")
            .field("multi", "value2")
            .build());
        StreamingMultipartFormParts form = StreamingMultipartFormParts.parse(boundary.getBytes(UTF_8), multipartFormContentsStream, UTF_8);
        try (Parts parts = MultipartFormMap.diskBackedFormMap(form, UTF_8, 1024, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<PartWithInputStream>> partMap = parts.partMap;

            assertThat(partMap.get("file").get(0).fileName, equalTo("foo.tab"));
            assertThat(partMap.get("anotherFile").get(0).fileName, equalTo("BAR.tab"));
//        assertThat(partMap.get("field").get(0).getInputStream(), equalTo("fieldValue" + CR_LF + "with cr lf"));
//        assertThat(partMap.get("multi").get(0).getInputStream(), equalTo("value1"));
//        assertThat(partMap.get("multi").get(1).getInputStream(), equalTo("value2"));
        }
    }

    @Test
    public void canLoadComplexRealLifeSafariExample() throws Exception {
        StreamingMultipartFormParts form = StreamingMultipartFormParts.parse(
            "------WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(UTF_8),
            new FileInputStream("examples/safari-example.multipart"),
            UTF_8
        );
        try (Parts parts = MultipartFormMap.diskBackedFormMap(form, UTF_8, 10240, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<PartWithInputStream>> partMap = parts.partMap;

            assertFileIsCorrect(partMap.get("uploadManuscript").get(0), "simple7bit.txt");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(2), "utf8\uD83D\uDCA9.file");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(3), "utf8\uD83D\uDCA9.txt");

//        assertFileIsCorrect(partMap.get("uploadManuscript").get(1), "starbucks.jpeg", new ByteArrayInputStream(partMap.get("uploadManuscript").get(1).getBytes()));
//
//        assertThat(partMap.get("articleType").get(0).content, equalTo("obituary"));
        }

    }

    @Test
    public void failsIfFormIsTooBig() throws Exception {
        StreamingMultipartFormParts form = StreamingMultipartFormParts.parse(
            "------WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(UTF_8),
            new FileInputStream("examples/safari-example.multipart"),
            UTF_8,
            1024
        );

        try {
            MultipartFormMap.diskBackedFormMap(form, UTF_8, 1024, TEMPORARY_FILE_DIRECTORY);
            fail("should have failed because the form is too big");
        } catch (StreamTooLongException e) {
            assertThat(e.getMessage(), containsString("Form contents was longer than 1024 bytes"));
        }
    }

    @Test
    public void savesToDisk() throws Exception {
        StreamingMultipartFormParts form = StreamingMultipartFormParts.parse(
            "------WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(UTF_8),
            new FileInputStream("examples/safari-example.multipart"),
            UTF_8
        );
        String[] files;
        try (Parts parts = MultipartFormMap.diskBackedFormMap(form, UTF_8, 100, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<PartWithInputStream>> partMap = parts.partMap;

            assertFileIsCorrect(partMap.get("uploadManuscript").get(0), "simple7bit.txt");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(2), "utf8\uD83D\uDCA9.file");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(3), "utf8\uD83D\uDCA9.txt");

            assertThat(TEMPORARY_FILE_DIRECTORY.list().length, equalTo(4));
        }
        assertThat(TEMPORARY_FILE_DIRECTORY.list().length, equalTo(0));
    }

    private void assertFileIsCorrect(PartWithInputStream filePart, String expectedFilename) throws IOException {
        assertFileIsCorrect(filePart, expectedFilename, filePart.getNewInputStream());
    }

    private void assertFileIsCorrect(Part filePart, String expectedFilename, InputStream inputStream) throws IOException {
        assertThat(filePart.fileName, equalTo(expectedFilename));
        compareStreamToFile(inputStream, filePart.getFileName());
    }
}
