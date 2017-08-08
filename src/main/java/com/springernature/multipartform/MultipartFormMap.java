package com.springernature.multipartform;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.springernature.multipartform.stream.StreamUtil.readAllBytesFromInputStream;

public class MultipartFormMap {

    public static Map<String, List<InMemoryPart>> inMemoryFormMap(StreamingMultipartFormParts parts, Charset encoding, int maxPartSize) throws IOException {
        Map<String, List<InMemoryPart>> partMap = new HashMap<>();

        for (StreamingPart part : parts) {
            List<InMemoryPart> valueParts = partMap.containsKey(part.getFieldName()) ?
                partMap.get(part.getFieldName()) :
                new ArrayList<>();
            valueParts.add(new InMemoryPart(part, readAllBytesFromInputStream(part.inputStream, maxPartSize), encoding));
            partMap.put(part.getFieldName(), valueParts);
        }

        return partMap;
    }

    public static Parts diskBackedFormMap(StreamingMultipartFormParts parts, Charset encoding, int writeToDiskThreshold, File temporaryFileDirectory) throws IOException {
        Map<String, List<PartWithInputStream>> partMap = new HashMap<>();
        byte[] bytes = new byte[writeToDiskThreshold];

        for (StreamingPart part : parts) {
            List<PartWithInputStream> keyParts = partMap.containsKey(part.getFieldName()) ?
                partMap.get(part.getFieldName()) :
                new ArrayList<>();

            keyParts.add(diskBackedPart(encoding, writeToDiskThreshold, temporaryFileDirectory, part, part.inputStream, bytes));
            partMap.put(part.getFieldName(), keyParts);
        }
        return new Parts(partMap);
    }

    private static PartWithInputStream diskBackedPart(Charset encoding, int writeToDiskThreshold, File temporaryFileDirectory, StreamingPart part, InputStream partInputStream, byte[] bytes) throws IOException {
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

    @NotNull private static File writeToDisk(String fileName, int writeToDiskThreshold, File temporaryFileDirectory, byte[] bytes, int length, InputStream partInputStream) throws IOException {
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
