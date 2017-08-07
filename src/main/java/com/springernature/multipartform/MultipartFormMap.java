package com.springernature.multipartform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartFormMap {

    public static Map<String, List<InMemoryPart>> inMemoryFormMap(StreamingMultipartFormParts parts, Charset encoding, int maxPartSize) throws IOException {
        Map<String, List<InMemoryPart>> partMap = new HashMap<>();

        for (StreamingPart part : parts) {
            List<InMemoryPart> valueParts = partMap.containsKey(part.getFieldName()) ?
                partMap.get(part.getFieldName()) :
                new ArrayList<>();
            valueParts.add(new InMemoryPart(part, getBytes(part, maxPartSize), encoding));
            partMap.put(part.getFieldName(), valueParts);
        }

        return partMap;
    }

    private static byte[] getBytes(StreamingPart part, int maxPartSize) throws IOException {
        byte[] bytes = new byte[maxPartSize];
        int length = part.getContentsAsBytes(maxPartSize, bytes);
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }

    public static Map<String, List<DiskBackedPart>> diskBackedFormMap(StreamingMultipartFormParts parts, Charset encoding, int writeToDiskThreshold) throws IOException {
        Map<String, List<DiskBackedPart>> partMap = new HashMap<>();
        for (StreamingPart part : parts) {
            List<DiskBackedPart> keyParts = partMap.containsKey(part.getFieldName()) ?
                partMap.get(part.getFieldName()) :
                new ArrayList<>();
            byte[] bytes = new byte[writeToDiskThreshold];
            int length = 0;

            InputStream partInputStream = part.inputStream;
            while (true) {
                int count = partInputStream.read(bytes, length, writeToDiskThreshold - length);
                if (count < 0) {
                    partInputStream.close();
                    break;
                }
                if (length >= writeToDiskThreshold) {
                    File tempFile = File.createTempFile("download-", "tmp");
                    FileOutputStream outputStream = new FileOutputStream(tempFile);
                    outputStream.write(bytes, 0, length);
//                    while (true) {
//
//                    }
                }
                length += count;
            }
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, 0, length);
            keyParts.add(new DiskBackedPart(part, result, encoding));
            partMap.put(part.getFieldName(), keyParts);
        }
        return partMap;
    }
}
