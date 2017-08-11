package org.tiestvilee.multipartform;

import org.junit.Test;
import org.tiestvilee.multipartform.exceptions.StreamTooLongException;
import org.tiestvilee.multipartform.exceptions.TokenNotFoundException;
import org.tiestvilee.multipartform.part.Part;
import org.tiestvilee.multipartform.part.Parts;
import org.tiestvilee.multipartform.part.StreamingPart;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MultipartFormMapTest {
    public static final File TEMPORARY_FILE_DIRECTORY = new File("./out/tmp");

    static {
        TEMPORARY_FILE_DIRECTORY.mkdirs();
    }

    @Test
    public void uploadMultipleFilesAndFields() throws Exception {
        String boundary = "-----1234";
        InputStream multipartFormContentsStream = new ByteArrayInputStream(new ValidMultipartFormBuilder(boundary)
            .file("file", "foo.tab", "text/whatever", "This is the content of the file\n")
            .field("field", "fieldValue" + StreamingMultipartFormHappyTests.CR_LF + "with cr lf")
            .field("multi", "value1")
            .file("anotherFile", "BAR.tab", "text/something", "This is another file\n")
            .field("multi", "value2")
            .build());
        Iterable<StreamingPart> form = StreamingMultipartFormParts.parse(boundary.getBytes(UTF_8), multipartFormContentsStream, UTF_8);

        try (Parts parts = MultipartFormMap.formMap(form, UTF_8, 1024, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<Part>> partMap = parts.partMap;

            assertThat(partMap.get("file").get(0).fileName, equalTo("foo.tab"));
            assertThat(partMap.get("anotherFile").get(0).fileName, equalTo("BAR.tab"));
            StreamingMultipartFormHappyTests.compareOneStreamToAnother(partMap.get("field").get(0).getNewInputStream(), new ByteArrayInputStream(("fieldValue" + StreamingMultipartFormHappyTests.CR_LF + "with cr lf").getBytes()));
            StreamingMultipartFormHappyTests.compareOneStreamToAnother(partMap.get("multi").get(0).getNewInputStream(), new ByteArrayInputStream("value1".getBytes()));
            StreamingMultipartFormHappyTests.compareOneStreamToAnother(partMap.get("multi").get(1).getNewInputStream(), new ByteArrayInputStream("value2".getBytes()));
        }
    }

    @Test
    public void canLoadComplexRealLifeSafariExample() throws Exception {
        Iterable<StreamingPart> form = safariExample();

        try (Parts parts = MultipartFormMap.formMap(form, UTF_8, 1024000, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<Part>> partMap = parts.partMap;

            allFieldsAreLoadedCorrectly(partMap, true, true, true, true);

        }

    }

    @Test
    public void throwsExceptionIfFormIsTooBig() throws Exception {
        Iterable<StreamingPart> form = StreamingMultipartFormParts.parse(
            "------WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(UTF_8),
            new FileInputStream("examples/safari-example.multipart"),
            UTF_8,
            1024
        );

        try {
            MultipartFormMap.formMap(form, UTF_8, 1024, TEMPORARY_FILE_DIRECTORY);
            fail("should have failed because the form is too big");
        } catch (StreamTooLongException e) {
            assertThat(e.getMessage(), containsString("Form contents was longer than 1024 bytes"));
        }
    }

    @Test
    public void savesAllPartsToDisk() throws Exception {
        Iterable<StreamingPart> form = safariExample();

        try (Parts parts = MultipartFormMap.formMap(form, UTF_8, 100, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<Part>> partMap = parts.partMap;

            allFieldsAreLoadedCorrectly(partMap, false, false, false, false);

            assertThat(temporaryFileList().length, equalTo(4));
        }
        assertThat(temporaryFileList().length, equalTo(0));
    }

    @Test
    public void savesSomePartsToDisk() throws Exception {
        Iterable<StreamingPart> form = safariExample();

        try (Parts parts = MultipartFormMap.formMap(form, UTF_8, 1024 * 4, TEMPORARY_FILE_DIRECTORY)) {
            Map<String, List<Part>> partMap = parts.partMap;

            allFieldsAreLoadedCorrectly(partMap, false, true, true, false);

            String[] files = temporaryFileList();
            assertPartSaved("simple7bit.txt", files);
            assertPartSaved("starbucks.jpeg", files);
            assertThat(files.length, equalTo(2));
        }
        assertThat(temporaryFileList().length, equalTo(0));
    }

    @Test
    public void throwsExceptionIfMultipartMalformed() throws Exception {
        Iterable<StreamingPart> form = StreamingMultipartFormParts.parse(
            "-----2345".getBytes(UTF_8),
            new ByteArrayInputStream(("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
                "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
                "" + StreamingMultipartFormHappyTests.CR_LF +
                "value" + // no CR_LF
                "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes()),
            UTF_8);

        try (Parts parts = MultipartFormMap.formMap(form, UTF_8, 1024 * 4, TEMPORARY_FILE_DIRECTORY)) {
            fail("Should have thrown an Exception");
        } catch (TokenNotFoundException e) {
            assertThat(e.getMessage(), equalTo("Boundary must be proceeded by field separator, but didn't find it"));
        }
    }

    private Iterable<StreamingPart> safariExample() throws IOException {
        return StreamingMultipartFormParts.parse(
            "------WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(UTF_8),
            new FileInputStream("examples/safari-example.multipart"),
            UTF_8
        );
    }

    private void allFieldsAreLoadedCorrectly(Map<String, List<Part>> partMap, boolean simple7bit, boolean file, boolean txt, boolean jpeg) throws IOException {

        assertFileIsCorrect(partMap.get("uploadManuscript").get(0), "simple7bit.txt", simple7bit);
        assertFileIsCorrect(partMap.get("uploadManuscript").get(2), "utf8\uD83D\uDCA9.file", file);

        Part articleType = partMap.get("articleType").get(0);
        assertTrue("articleType", articleType.isInMemory());
        assertThat(articleType.getString(), equalTo("obituary"));

        assertFileIsCorrect(partMap.get("uploadManuscript").get(3), "utf8\uD83D\uDCA9.txt", txt);
        assertFileIsCorrect(partMap.get("uploadManuscript").get(1), "starbucks.jpeg", jpeg);

    }

    private void assertPartSaved(final String fileName, String[] files) {
        assertTrue(
            "couldn't find " + fileName + " in " + Arrays.toString(files),
            files[0].contains(fileName) || files[1].contains(fileName));
    }

    private void assertFileIsCorrect(Part filePart, String expectedFilename, boolean inMemory) throws IOException {
        assertFileIsCorrect(filePart, expectedFilename, filePart.getNewInputStream(), inMemory);
    }

    private void assertFileIsCorrect(Part filePart, String expectedFilename, InputStream inputStream, boolean inMemory) throws IOException {
        assertThat(expectedFilename + " in memory?", filePart.isInMemory(), equalTo(inMemory));
        assertThat(filePart.fileName, equalTo(expectedFilename));
        StreamingMultipartFormHappyTests.compareStreamToFile(inputStream, filePart.getFileName());
    }

    private String[] temporaryFileList() {
        return TEMPORARY_FILE_DIRECTORY.list();
    }
}
