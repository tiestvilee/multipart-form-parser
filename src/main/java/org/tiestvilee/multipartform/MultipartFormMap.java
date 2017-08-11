package org.tiestvilee.multipartform;

import org.tiestvilee.multipartform.exceptions.ParseError;
import org.tiestvilee.multipartform.part.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartFormMap {

    /**
     * Returns a Parts object containing a map of FieldName -> Part, serialised from parts using the encoding
     * and maximum Part size specified. If a Part is bigger than the writeToDiskThreshold, then it will be written to
     * disk in temporaryFileDirectory (or the default temp dir if null).
     * <p>
     * To limit the overall size of the stream, pass the appropriate parameter to StreamingMultipartFormParts
     * <p>
     * The Parts object must be closed when finished with so that the files that have been written to disk can be
     * deleted.
     *
     * @param parts                  streaming parts
     * @param encoding               encoding of the stream
     * @param writeToDiskThreshold   if a Part is bigger than this threshold it will be purged from memory
     *                               and written to disk
     * @param temporaryFileDirectory where to write the files for Parts that are too big. Uses the default
     *                               temporary directory if null.
     * @return Parts object, which contains the Map of Fieldname to List of Parts. This object must
     * be closed so that it is cleaned up after.
     * @throws IOException
     */
    public static Parts formMap(Iterable<StreamingPart> parts, Charset encoding, int writeToDiskThreshold, File temporaryFileDirectory) throws IOException {
        try {
            Map<String, List<Part>> partMap = new HashMap<>();
            byte[] bytes = new byte[writeToDiskThreshold];

            for (StreamingPart part : parts) {
                List<Part> keyParts = partMap.containsKey(part.getFieldName()) ?
                    partMap.get(part.getFieldName()) :
                    new ArrayList<>();

                keyParts.add(serialisePart(encoding, writeToDiskThreshold, temporaryFileDirectory, part, part.inputStream, bytes));
                partMap.put(part.getFieldName(), keyParts);
            }
            return new Parts(partMap);
        } catch (ParseError e) {
            // stupid... cos 'iterator' doesn't throw exceptions
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private static Part serialisePart(Charset encoding, int writeToDiskThreshold, File temporaryFileDirectory, StreamingPart part, InputStream partInputStream, byte[] bytes) throws IOException {
        int length = 0;

        while (true) {
            int count = partInputStream.read(bytes, length, writeToDiskThreshold - length);
            if (count < 0) {
                return new InMemoryPart(
                    part,
                    storeInMemory(bytes, length, partInputStream), encoding);
            }
            length += count;
            if (length >= writeToDiskThreshold) {
                return new DiskBackedPart(
                    part,
                    writeToDisk(part.fileName, writeToDiskThreshold, temporaryFileDirectory, bytes, length, partInputStream));
            }
        }
    }

    private static byte[] storeInMemory(byte[] bytes, int length, InputStream partInputStream) throws IOException {
        partInputStream.close();

        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }

    private static File writeToDisk(String fileName, int writeToDiskThreshold, File temporaryFileDirectory, byte[] bytes, int length, InputStream partInputStream) throws IOException {
        File tempFile = File.createTempFile(fileName + "-", ".tmp", temporaryFileDirectory);
        tempFile.deleteOnExit();
        OutputStream outputStream = new FileOutputStream(tempFile);
        outputStream.write(bytes, 0, length);
        while (true) {
            int readLength = partInputStream.read(bytes, 0, writeToDiskThreshold);
            if (readLength < 0) {
                break;
            }
            outputStream.write(bytes, 0, readLength);
        }
        partInputStream.close();
        return tempFile;
    }
}
