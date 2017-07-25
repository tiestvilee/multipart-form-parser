package com.springernature.multipartform;

import java.io.IOException;
import java.io.InputStream;

public class MfpBufferedInputStream {
    private final InputStream inputStream;
    private final byte[] buffer;

    private int bufferLeft = 0;
    private int bufferRight = 0;

    public MfpBufferedInputStream(InputStream inputStream, int bufSize) throws IOException {
        this.inputStream = inputStream;
        this.buffer = new byte[bufSize];

        readIntoBuffer();
    }

    private void readIntoBuffer() throws IOException {
        bufferLeft = 0;
        bufferRight = inputStream.read(buffer, 0, buffer.length);
    }

    public boolean dropFromBufferUntilFound(byte[] eol) {
        // very inefficient search!
        while (bufferLeft < bufferRight) {
            int eolIndex = 0;
            while (eolIndex < eol.length && buffer[bufferLeft + eolIndex] == eol[eolIndex]) {eolIndex++;}
            if (eolIndex == eol.length) {
                bufferLeft += eolIndex;
                return true;
            }
            bufferLeft++;
        }
        return false;
    }

    public String readStringFromBufferUntil(byte[] eol) {
        // very inefficient search!
        int start = bufferLeft;
        while (bufferLeft < bufferRight) {
            int eolIndex = 0;
            while (eolIndex < eol.length && buffer[bufferLeft + eolIndex] == eol[eolIndex]) {eolIndex++;}
            if (eolIndex == eol.length) {
                bufferLeft += eolIndex;
                return new String(buffer, start, bufferLeft - eolIndex - start);
            }
            bufferLeft++;
        }
        throw new RuntimeException("BAD got to end of stream before finishing header");
    }

    public boolean findInBuffer(byte[] query) {
        int i = 0;
        for (; (i < query.length) && (bufferLeft + i < bufferRight); i++) {
            if (buffer[bufferLeft + i] != query[i]) {
                return false;
            }
        }
        if (i == query.length) {
            bufferLeft += i;
            return true;
        }
        return false;
    }

    public int readByteFromBufferUntil(byte[] boundaryWithPrefix) {
        int boundaryIndex = 0;
        while (boundaryIndex < boundaryWithPrefix.length && buffer[bufferLeft + boundaryIndex] == boundaryWithPrefix[boundaryIndex]) {boundaryIndex++;}
        if (boundaryIndex == boundaryWithPrefix.length) {
            return -1;
        }
        byte result = buffer[bufferLeft];
        bufferLeft++;
        return result;
    }

    public byte[] lastBytes(int numberOfBytes) {
        numberOfBytes = Math.min(numberOfBytes, bufferLeft);
        byte[] result = new byte[numberOfBytes];
        System.arraycopy(buffer, bufferLeft - numberOfBytes, result, 0, numberOfBytes);
        return result;
    }
}
