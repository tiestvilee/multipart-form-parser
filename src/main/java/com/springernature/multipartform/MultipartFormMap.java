package com.springernature.multipartform;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartFormMap {

    public static Map<String, List<InMemoryPart>> allPartsInMemory(StreamingMultipartFormParts parts, Charset encoding, int maxStreamSize) throws IOException {
        Map<String, List<InMemoryPart>> partMap = new HashMap<>();
        for (Part part : parts) {
            List<InMemoryPart> keyParts = partMap.containsKey(part.getFieldName()) ?
                partMap.get(part.getFieldName()) :
                new ArrayList<>();
            keyParts.add(part.realise(encoding, maxStreamSize));
            partMap.put(part.getFieldName(), keyParts);
        }
        return partMap;
    }
}
