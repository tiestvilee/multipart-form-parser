package com.springernature.multipartform;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Parts implements AutoCloseable {
    public final Map<String, List<PartWithInputStream>> partMap;

    public Parts(Map<String, List<PartWithInputStream>> partMap) {
        this.partMap = Collections.unmodifiableMap(partMap);
    }

    @Override public void close() throws IOException {
        for (List<PartWithInputStream> parts : partMap.values()) {
            for (PartWithInputStream part : parts) {
                part.close();
            }
        }

    }
}
