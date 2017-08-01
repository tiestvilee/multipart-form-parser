package com.springernature.multipartform;

import com.springernature.multipartform.apache.ParameterParser;
import com.springernature.multipartform.exceptions.AlreadyClosedException;
import com.springernature.multipartform.exceptions.ParseError;
import com.springernature.multipartform.exceptions.TokenNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class StreamingMultipartFormParts implements Iterator<Part> {
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
     * The maximum length of each header line
     */
    public static final int HEADER_LINE_SIZE_MAX = 4096;

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

    private final TokenBoundedInputStream buf;
    private final Charset encoding;
    private Part currentPart;
    private boolean nextIsKnown;

    private byte[] boundary;
    private byte[] boundaryWithPrefix;
    private MultipartFormStreamState state;
    private String mixedName = null;
    private byte[] oldBoundary = null;
    private byte[] oldBoundaryWithPrefix;

    public static StreamingMultipartFormParts parse(byte[] boundary, InputStream inputStream, Charset encoding) throws IOException {
        return new StreamingMultipartFormParts(boundary, inputStream, DEFAULT_BUFSIZE, encoding);
    }

    public StreamingMultipartFormParts(byte[] boundary, InputStream inputStream, int bufSize, Charset encoding) throws IOException {
        this.boundary = boundary;
        this.encoding = encoding;
        if (bufSize < this.boundary.length + FIELD_SEPARATOR.length) {
            throw new IllegalArgumentException("bufSize must be bigger than the boundary");
        }
        buf = new TokenBoundedInputStream(inputStream, bufSize, encoding);

        this.boundaryWithPrefix = addPrefixToBoundary(this.boundary);

        nextIsKnown = false;
        currentPart = null;
        state = MultipartFormStreamState.findBoundary;
        //        parseNextPart();
    }

    private byte[] addPrefixToBoundary(byte[] boundary) {
        byte[] b = new byte[boundary.length + FIELD_SEPARATOR.length]; // in apache they just use BOUNDARY_PREFIX
        System.arraycopy(boundary, 0, b, 2, boundary.length);
        System.arraycopy(FIELD_SEPARATOR, 0, b, 0, FIELD_SEPARATOR.length);
        return b;
    }

    private void findBoundary() throws IOException {
        if (state == MultipartFormStreamState.findPrefix) {
            if (!buf.matchInStream(FIELD_SEPARATOR)) {
                throw new TokenNotFoundException("Boundary must be proceeded by field separator, but didn't find it");
            }
            state = MultipartFormStreamState.findBoundary;
        }

        assertStateIs(MultipartFormStreamState.findBoundary);

        if (!buf.dropFromStreamUntilMatched(boundary)) {
            // what about when this isn't at the beginning of a line...
            throw new TokenNotFoundException("Boundary not found <<" + new String(boundary, encoding) + ">>");
        }
        state = MultipartFormStreamState.boundaryFound;
        if (buf.matchInStream(STREAM_TERMINATOR)) {
            if (buf.matchInStream(FIELD_SEPARATOR)) {
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
                throw new TokenNotFoundException("Stream terminator must be followed by field separator, but didn't find it");
            }
        } else {
            if (!buf.matchInStream(FIELD_SEPARATOR)) {
                throw new TokenNotFoundException("Boundary must be followed by field separator, but didn't find it");
            } else {
                state = MultipartFormStreamState.header;
            }
        }
    }

    @Override public boolean hasNext() {
        if (nextIsKnown) {
            return !isEndOfStream();
        }
        nextIsKnown = true;

        if (state == MultipartFormStreamState.contents) {
            try {
                currentPart.close();
            } catch (Exception e) {
                /* ???? */
            }
        }

        currentPart = safelyParseNextPart();

        return !isEndOfStream();
    }

    @Override public Part next() {
        if (nextIsKnown) {
            if (isEndOfStream()) {
                throw new NoSuchElementException("No more parts in this MultipartForm");
            }
            nextIsKnown = false;
        } else {

            if (state == MultipartFormStreamState.contents) {
                try {
                    currentPart.close();
                } catch (Exception e) {
                /* ???? */
                }
            }

            currentPart = safelyParseNextPart();
            if (isEndOfStream()) {
                throw new NoSuchElementException("No more parts in this MultipartForm");
            }
        }
        return currentPart;
    }

    private Part safelyParseNextPart() {
        try {
            return parseNextPart();
        } catch (IOException e) {
            nextIsKnown = true;
            currentPart = null;
            throw new ParseError(e);
        }
    }

    private Part parseNextPart() throws IOException {
        findBoundary();
        if (state == MultipartFormStreamState.header) {
            return parsePart();
        } else {
            return null;
        }
    }

    private Part parsePart() throws IOException {
        Map<String, String> headers = parseHeaderLines();

        String contentType = headers.get("Content-Type");
        if (contentType != null && contentType.startsWith("multipart/mixed")) {
            Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
            Map<String, String> contentTypeParams = new ParameterParser().parse(contentType, ';');

            mixedName = trim(contentDisposition.get("name"));

            oldBoundary = boundary;
            oldBoundaryWithPrefix = boundaryWithPrefix;
            boundary = (new String(STREAM_TERMINATOR, encoding) + trim(contentTypeParams.get("boundary"))).getBytes(encoding);
            boundaryWithPrefix = addPrefixToBoundary(this.boundary);

            state = MultipartFormStreamState.findBoundary;

            return parseNextPart();
        } else {
            Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
            String fieldName = contentDisposition.containsKey("attachment") ? mixedName : trim(contentDisposition.get("name"));
            String filename = filenameFromMap(contentDisposition);

            return new Part(
                fieldName,
                !contentDisposition.containsKey("filename"),
                contentType,
                filename,
                new BoundedInputStream(), headers);
        }
    }

    private String filenameFromMap(Map<String, String> contentDisposition) {
        if (contentDisposition.containsKey("filename")) {
            String filename = contentDisposition.get("filename");
            return trim(filename == null ? "" : filename);
        } else {
            return null;
        }
    }


    private String trim(String string) {
        if (string != null) {
            return string.trim();
        }
        return null;
    }

    private Map<String, String> parseHeaderLines() throws IOException {
        assertStateIs(MultipartFormStreamState.header);

        Map<String, String> result = new HashMap<>();
        String previousHeaderName = null;
        while (true) {
            String header = buf.readStringFromStreamUntilMatched(FIELD_SEPARATOR, HEADER_LINE_SIZE_MAX);
            if (header.equals("")) {
                state = MultipartFormStreamState.contents;
                return result;
            }
            if (header.matches("\\s+.*")) {
                result.put(previousHeaderName, result.get(previousHeaderName) + "; " + header.trim());
            } else {
                int index = header.indexOf(":");
                if (index < 0) {
                    throw new ParseError("Header didn't include a colon <<" + header + ">>");
                } else {
                    previousHeaderName = header.substring(0, index).trim();
                    result.put(previousHeaderName, header.substring(index + 1).trim());
                }
            }
        }
    }

    public boolean isEndOfStream() {
        return currentPart == null;
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

            return readNextByte();
        }

        private int readNextByte() throws IOException {
            int result = buf.readByteFromStreamUntilMatched(boundaryWithPrefix);
            if (result == -1) {
                state = MultipartFormStreamState.findPrefix;
                endOfStream = true;
            }
            return result;
        }

        @Override public void close() {
            closed = true;
            if (!endOfStream) {
                try {
                    while (readNextByte() > -1) {
                        // do nothing
                    }
                } catch (IOException e) {
                    endOfStream = true;
                    throw new ParseError(e);
                }
            }
        }
    }

    private void assertStateIs(MultipartFormStreamState expectedState) {
        if (expectedState != state) {
            throw new IllegalStateException("Expected state " + expectedState + " but got " + state);
        }
    }

    private enum MultipartFormStreamState {
        findPrefix, findBoundary, boundaryFound, eos, header, contents, error
    }
}