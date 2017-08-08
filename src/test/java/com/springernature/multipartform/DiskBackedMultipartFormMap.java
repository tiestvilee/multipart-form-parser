package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.StreamTooLongException;
import com.springernature.multipartform.part.InMemoryPart;
import com.springernature.multipartform.part.Part;
import com.springernature.multipartform.part.PartWithInputStream;
import com.springernature.multipartform.part.Parts;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.springernature.multipartform.StreamingMultipartFormHappyTests.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DiskBackedMultipartFormMap {
    public static final File TEMPORARY_FILE_DIRECTORY = new File("./out/tmp");

    static {
        TEMPORARY_FILE_DIRECTORY.mkdirs();
    }

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
            compareOneStreamToAnother(partMap.get("field").get(0).getNewInputStream(), new ByteArrayInputStream(("fieldValue" + CR_LF + "with cr lf").getBytes()));
            compareOneStreamToAnother(partMap.get("multi").get(0).getNewInputStream(), new ByteArrayInputStream("value1".getBytes()));
            compareOneStreamToAnother(partMap.get("multi").get(1).getNewInputStream(), new ByteArrayInputStream("value2".getBytes()));
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
            assertFileIsCorrect(partMap.get("uploadManuscript").get(1), "starbucks.jpeg");

            assertThat(((InMemoryPart) partMap.get("articleType").get(0)).getContent(), equalTo("obituary"));
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

        try (Parts parts = MultipartFormMap.diskBackedFormMap(form, UTF_8, 100, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<PartWithInputStream>> partMap = parts.partMap;

            assertFileIsCorrect(partMap.get("uploadManuscript").get(0), "simple7bit.txt");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(2), "utf8\uD83D\uDCA9.file");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(3), "utf8\uD83D\uDCA9.txt");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(1), "starbucks.jpeg");

            assertThat(TEMPORARY_FILE_DIRECTORY.list().length, equalTo(4));
        }
        assertThat(TEMPORARY_FILE_DIRECTORY.list().length, equalTo(0));
    }

    @Test
    public void savesSomeToDisk() throws Exception {
        StreamingMultipartFormParts form = StreamingMultipartFormParts.parse(
            "------WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(UTF_8),
            new FileInputStream("examples/safari-example.multipart"),
            UTF_8
        );

        try (Parts parts = MultipartFormMap.diskBackedFormMap(form, UTF_8, 1024 * 4, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<PartWithInputStream>> partMap = parts.partMap;

            assertFileIsCorrect(partMap.get("uploadManuscript").get(0), "simple7bit.txt");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(2), "utf8\uD83D\uDCA9.file");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(3), "utf8\uD83D\uDCA9.txt");
            assertFileIsCorrect(partMap.get("uploadManuscript").get(1), "starbucks.jpeg");

            String[] files = TEMPORARY_FILE_DIRECTORY.list();
            assertTrue("couldn't find simple7bit.txt in " + Arrays.toString(files), files[0].contains("simple7bit.txt") || files[1].contains("simple7bit.txt"));
            assertTrue("couldn't find starbucks.jpeg in " + Arrays.toString(files), files[0].contains("starbucks.jpeg") || files[1].contains("starbucks.jpeg"));
            assertThat(files.length, equalTo(2));
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
