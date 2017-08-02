package com.springernature.multipartform;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class MultipartFormMap {

    public Map<String, InMemoryPart> allPartsInMemory(StreamingMultipartFormParts parts, Charset encoding, int maxPartContentSize) throws IOException {
        Map<String, InMemoryPart> partMap = new HashMap<>();
        for (Part part : parts) {
            partMap.put(part.getFieldName(), part.realise(encoding, maxPartContentSize));
        }
        return partMap;
    }
}
