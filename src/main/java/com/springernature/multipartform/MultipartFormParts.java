package com.springernature.multipartform;

import com.springernature.multipartform.apache.ParameterParser;
import com.springernature.multipartform.exceptions.AlreadyClosedException;

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

    private final MfpBufferedInputStream buf;
    private Part currentPart;
    private boolean isEndOfStream;
    private boolean nextIsKnown;

    private byte[] boundary;
    private byte[] boundaryWithPrefix;
    private MultipartFormStreamState state = MultipartFormStreamState.findBoundary;
    private String mixedName = null;
    private byte[] oldBoundary = null;
    private byte[] oldBoundaryWithPrefix;

    public static MultipartFormParts parse(String boundary, InputStream inputStream) throws IOException {
        return new MultipartFormParts(boundary, inputStream, DEFAULT_BUFSIZE);
    }

    public MultipartFormParts(String boundary, InputStream inputStream, int bufSize) throws IOException {
        this.boundary = boundary.getBytes();
        if (bufSize < this.boundary.length + FIELD_SEPARATOR.length) {
            throw new IllegalArgumentException("bufSize must be bigger than the boundary");
        }
        buf = new MfpBufferedInputStream(inputStream, bufSize);

        this.boundaryWithPrefix = addPrefixToBoundary(this.boundary);

        parseNextPart();
    }

    private byte[] addPrefixToBoundary(byte[] boundary) {
        byte[] b = new byte[boundary.length + FIELD_SEPARATOR.length]; // in apache they just use BOUNDARY_PREFIX
        System.arraycopy(boundary, 0, b, 2, boundary.length);
        System.arraycopy(FIELD_SEPARATOR, 0, b, 0, FIELD_SEPARATOR.length);
        return b;
    }

    private void findBoundary() {
        assertStateIs(MultipartFormStreamState.findBoundary);

        if (!buf.dropFromBufferUntilFound(boundary)) {
            // what about when this isn't at the beginning of a line...
            throw new RuntimeException("BAD - couldn't find boundary <<" + new String(boundary) + ">> in <<" + new String(buf.lastBytes(boundary.length + 8)) + ">>");
        }
        state = MultipartFormStreamState.boundaryFound;
        if (buf.findInBuffer(STREAM_TERMINATOR) && buf.findInBuffer(FIELD_SEPARATOR)) {
            // what if the stream terminator is found but the field separator isn't...
            if (mixedName != null) {
                boundary = oldBoundary;
                boundaryWithPrefix = oldBoundaryWithPrefix;
                mixedName = null;

                state = MultipartFormStreamState.findBoundary;
                findBoundary();
            } else {
                state = MultipartFormStreamState.eos;
            }
        } else {
            if (!buf.findInBuffer(FIELD_SEPARATOR)) {
                throw new RuntimeException("WTF - no field separator following boundary");
            } else {
                state = MultipartFormStreamState.header;
            }
        }
    }

    @Override public boolean hasNext() {
        // but what if we use the part after the boundary has been found...
        if (nextIsKnown) {
            return !isEndOfStream;
        }
        nextIsKnown = true;

        if (state == MultipartFormStreamState.contents) {
            try {
                currentPart.close();
            } catch (Exception e) {
                /* ???? */
            }
        }

        parseNextPart();

        return !isEndOfStream;
    }

    @Override public Part next() {
        if (nextIsKnown) {
            nextIsKnown = false;
        } else {
            nextIsKnown = true;

            if (state == MultipartFormStreamState.contents) {
                try {
                    currentPart.close();
                } catch (Exception e) {
                /* ???? */
                }
            }

            parseNextPart();
        }
        return currentPart;
    }

    private void parseNextPart() {
        nextIsKnown = true;
        findBoundary();
        if (state == MultipartFormStreamState.header) {
            parsePart();
            isEndOfStream = false;
        } else {
            isEndOfStream = true;
        }
    }

    private void parsePart() {
        Map<String, String> headers = parseHeaderLines();

        String contentType = headers.get("Content-Type");
        if (contentType != null && contentType.startsWith("multipart/mixed")) {
            Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
            Map<String, String> contentTypeParams = new ParameterParser().parse(contentType, ';');

            mixedName = trim(contentDisposition.get("name"));

            oldBoundary = boundary;
            oldBoundaryWithPrefix = boundaryWithPrefix;
            boundary = ("--" + trim(contentTypeParams.get("boundary"))).getBytes();
            boundaryWithPrefix = addPrefixToBoundary(this.boundary);

            state = MultipartFormStreamState.findBoundary;

            parseNextPart();
        } else {
            Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
            if (contentDisposition.containsKey("form-data")) {
                String filename = contentDisposition.get("filename");
                currentPart = new Part(
                    trim(contentDisposition.get("name")),
                    !contentDisposition.containsKey("filename"),
                    contentType,
                    trim(filename == null ? "" : filename),
                    new BoundedInputStream(), headers);
            } else if (contentDisposition.containsKey("attachment")) {
                String filename = contentDisposition.get("filename");
                currentPart = new Part(
                    mixedName,
                    !contentDisposition.containsKey("filename"),
                    contentType,
                    trim(filename == null ? "" : filename),
                    new BoundedInputStream(), headers);
            }
        }
    }


    private String trim(String string) {
        if (string != null) {
            return string.trim();
        }
        return null;
    }

    private Map<String, String> parseHeaderLines() {
        assertStateIs(MultipartFormStreamState.header);

        Map<String, String> result = new HashMap<>();
        String previousHeaderName = null;
        while (true) {
            String header = buf.readStringFromBufferUntil(FIELD_SEPARATOR);
            if (header.equals("")) {
                state = MultipartFormStreamState.contents;
                return result;
            }
            if (header.matches("\\s+.*")) {
                result.put(previousHeaderName, result.get(previousHeaderName) + "; " + header.trim());
            } else {
                int index = header.indexOf(":");
                previousHeaderName = header.substring(0, index).trim();
                if (result.containsKey(previousHeaderName)) {
                    result.put(previousHeaderName, result.get(previousHeaderName) + "; " + header.substring(index + 1).trim());
                } else {
                    result.put(previousHeaderName, header.substring(index + 1).trim());
                }
            }
        }
    }

    private class BoundedInputStream extends InputStream {

        boolean endOfStream = false;
        boolean closed = false;

        @Override public int read() throws IOException {
            if (closed) {
                throw new AlreadyClosedException();
            }

            if (endOfStream) {
                return -1;
            }

            int result = buf.readByteFromBufferUntil(boundaryWithPrefix);
            if (result == -1) {
                state = MultipartFormStreamState.findBoundary;
                endOfStream = true;
                return -1;
            }
            return result;
        }

        @Override public void close() {
            closed = true;
            if (state == MultipartFormStreamState.contents) {
                state = MultipartFormStreamState.findBoundary;
            }
        }
    }

    private void assertStateIs(MultipartFormStreamState expectedState) {
        if (expectedState != state) {
            throw new IllegalStateException("Expected state " + expectedState + " but got " + state);
        }
    }

    private enum MultipartFormStreamState {
        findBoundary, boundaryFound, eos, header, contents, error
    }
}
