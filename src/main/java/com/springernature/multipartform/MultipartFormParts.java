package com.springernature.multipartform;

import com.springernature.multipartform.apache.ParameterParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultipartFormParts implements Iterator<Part> {
    private static final int DEFAULT_BUFSIZE = 4096;


    /**
     * The Carriage Return ASCII character value.
     */
    public static final byte CR = 0x0D;

    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;

    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;

    /**
     * The maximum length of <code>header-part</code> that will be
     * processed (10 kilobytes = 10240 bytes.).
     */
    public static final int HEADER_PART_SIZE_MAX = 10240;

    /**
     * A byte sequence that marks the end of <code>header-part</code>
     * (<code>CRLFCRLF</code>).
     */
    protected static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};

    /**
     * A byte sequence that that follows a delimiter that will be
     * followed by an encapsulation (<code>CRLF</code>).
     */
    protected static final byte[] FIELD_SEPARATOR = {CR, LF};

    /**
     * A byte sequence that that follows a delimiter of the last
     * encapsulation in the stream (<code>--</code>).
     */
    protected static final byte[] STREAM_TERMINATOR = {DASH, DASH};

    /**
     * A byte sequence that precedes a boundary (<code>CRLF--</code>).
     */
    protected static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};

    private final byte[] boundary;
    private final byte[] boundaryWithPrefix;
    private final InputStream inputStream;
    private final byte[] buffer;
    private Part currentPart;
    private boolean hasNext;
    private boolean nextIsKnown;

    int bufferLeft = 0;
    int bufferRight = 0;

    public static MultipartFormParts parse(String boundary, InputStream inputStream) throws IOException {
        return new MultipartFormParts(boundary, inputStream, DEFAULT_BUFSIZE);
    }

    public MultipartFormParts(String boundary, InputStream inputStream, int bufSize) throws IOException {
        this.boundary = boundary.getBytes();
        if (bufSize < this.boundary.length + FIELD_SEPARATOR.length) {
            throw new IllegalArgumentException("bufSize must be bigger than the boundary");
        }
        this.inputStream = inputStream;
        this.buffer = new byte[bufSize];

        this.boundaryWithPrefix = addPrefixToBoundary(this.boundary);

        readIntoBuffer();
        nextIsKnown = true;
        if (findBoundary()) {
            parsePart();
            hasNext = true;
        }
    }

    private byte[] addPrefixToBoundary(byte[] boundary) {
        byte[] b = new byte[boundary.length + FIELD_SEPARATOR.length]; // in apache they just use BOUNDARY_PREFIX
        System.arraycopy(boundary, 0, b, 2, boundary.length);
        System.arraycopy(FIELD_SEPARATOR, 0, b, 0, FIELD_SEPARATOR.length);
        return b;
    }

    private void readIntoBuffer() throws IOException {
        bufferLeft = 0;
        bufferRight = inputStream.read(buffer, 0, buffer.length);
    }

    private boolean findBoundary() {
        if (!dropFromBufferUntilFound(boundary)) {
            // what about when this isn't at the beginning of a line...
            throw new RuntimeException("BAD - couldn't find boundary");
        }
        if (findInBuffer(STREAM_TERMINATOR) && findInBuffer(FIELD_SEPARATOR)) {
            // what if the stream terminator is found but the field separator isn't...
            currentPart = null;
            return false;
        } else {
            if (!findInBuffer(FIELD_SEPARATOR)) {
                throw new RuntimeException("WTF - no field separator following boundary");
            } else {
                return true;
            }
        }
    }

    private void parsePart() {
        Map<String, String> headers = parseHeaders();
        Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
        if (contentDisposition.containsKey("form-data")) {
            // something about ATTACHMENT
            String filename = contentDisposition.get("filename");
            currentPart = new Part(
                trim(contentDisposition.get("name")),
                !contentDisposition.containsKey("filename"),
                headers.get("Content-Type"),
                trim(filename == null ? "" : filename),
                new BoundedInputStream());
        }
    }

    private String trim(String string) {
        if (string != null) {
            return string.trim();
        }
        return null;
    }

    private Map<String, String> parseHeaders() {
        Map<String, String> result = new HashMap<>();
        while (true) {
            String header = readStringFromBufferUntil(FIELD_SEPARATOR);
            if (header.equals("")) {
                return result;
            }
            int index = header.indexOf(":");
            result.put(header.substring(0, index).trim(), header.substring(index + 1).trim());
        }
    }

    private boolean dropFromBufferUntilFound(byte[] eol) {
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

    private String readStringFromBufferUntil(byte[] eol) {
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

    private boolean findInBuffer(byte[] query) {
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

    @Override public boolean hasNext() {
        // but what if we use the part after the boundary has been found...
        if (nextIsKnown) {
            return hasNext;
        }

        if (findBoundary()) {
            parsePart();
            hasNext = true;
        } else {
            hasNext = false;
        }
        nextIsKnown = true;
        return hasNext;
    }

    @Override public Part next() {
        nextIsKnown = false;
        return currentPart;
    }

    private class BoundedInputStream extends InputStream {
        @Override public int read() throws IOException {
            int boundaryIndex = 0;
            while (boundaryIndex < boundaryWithPrefix.length && buffer[bufferLeft + boundaryIndex] == boundaryWithPrefix[boundaryIndex]) {boundaryIndex++;}
            if (boundaryIndex == boundaryWithPrefix.length) {
                return -1;
            }
            byte result = buffer[bufferLeft];
            bufferLeft++;
            return result;
        }
    }
}
