package org.tiestvilee.multipartform.stream;

import org.tiestvilee.multipartform.exceptions.StreamTooLongException;
import org.tiestvilee.multipartform.exceptions.TokenNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class TokenBoundedInputStream extends CircularBufferedInputStream {
    private final int maxStreamLength;

    public TokenBoundedInputStream(InputStream inputStream, int bufSize) {
        this(inputStream, bufSize, -1);
    }

    public TokenBoundedInputStream(InputStream inputStream, int bufSize, int maxStreamLength) {
        super(inputStream, bufSize);
        this.maxStreamLength = maxStreamLength;
    }

    /**
     * Consumes all bytes up to and including the matched endOfToken bytes.
     * Fills the buffer with all bytes excluding the endOfToken bytes.
     * Returns the number of bytes inserted into the buffer.
     *
     * @param endOfToken bytes that indicate the end of this token
     * @param buffer     fills this buffer with bytes _excluding_ the endOfToken
     * @param encoding   Charset for formatting error messages
     * @return number of bytes inserted into buffer
     * @throws IOException
     */
    public int getBytesUntil(byte[] endOfToken, byte[] buffer, Charset encoding) throws IOException {
        int bufferIndex = 0;
        int bufferLength = buffer.length;

        int b;
        while (true) {
            b = readFromStream();
            if (b <= -1) {
                throw new TokenNotFoundException(
                    "Reached end of stream before finding Token <<" + new String(endOfToken, encoding) + ">>. " +
                        "Last " + endOfToken.length + " bytes read were " +
                        "<<" + getBytesRead(endOfToken, buffer, bufferIndex, encoding) + ">>");
            }
            if (bufferIndex >= bufferLength) {
                throw new TokenNotFoundException("Didn't find end of Token <<" + new String(endOfToken, encoding) + ">> " +
                    "within " + bufferLength + " bytes");
            }
            byte originalB = (byte) b;
            mark(endOfToken.length);
            if (matchToken(endOfToken, b)) {
                return bufferIndex;
            }
            buffer[bufferIndex++] = originalB;
            reset();
        }
    }

    private String getBytesRead(byte[] endOfToken, byte[] buffer, int bufferIndex, Charset encoding) {
        int index, length;
        if (bufferIndex - endOfToken.length > 0) {
            index = bufferIndex - endOfToken.length;
            length = endOfToken.length;
        } else {
            index = 0;
            length = bufferIndex;
        }
        return new String(buffer, index, length, encoding);
    }

    private boolean matchToken(byte[] token, int initialCharacter) throws IOException {
        int eotIndex = 0;
        while (initialCharacter > -1 && ((byte) initialCharacter == token[eotIndex]) && (++eotIndex) < token.length) {
            initialCharacter = readFromStream();
        }
        return eotIndex == token.length;
    }

    /**
     * Tries to match the token bytes at the current position. Only consumes bytes
     * if there is a match, otherwise the stream is unaffected.
     *
     * @param token The token being matched
     * @return true if the token is found (and the bytes have been consumed),
     * false if it isn't found (and the stream is unchanged)
     */
    public boolean matchInStream(byte[] token) throws IOException {
        mark(token.length);

        if (matchToken(token, readFromStream())) {
            return true;
        }

        reset();
        return false;
    }

    /**
     * returns a single byte from the Stream until the token is found. When the token is found,
     * -1 will be returned, and the token will still be available on the stream. Inconsistent.
     *
     * @param endOfToken bytes that indicate the end of this token
     * @return the next byte in the stream, or -1 if the token is found. The token is NOT consumed
     * when it is matched.
     */
    public int readByteFromStreamUntilMatched(byte[] endOfToken) throws IOException {
        mark(endOfToken.length);
        int b = readFromStream();
        int eotIndex = 0;
        while (eotIndex < endOfToken.length && ((byte) b == endOfToken[eotIndex])) {
            b = readFromStream();
            eotIndex++;
        }
        if (eotIndex == endOfToken.length) {
            reset();
            return -1;
        }
        reset();
        return readFromStream();
    }


    private int readFromStream() throws IOException {
        if (maxStreamLength > -1 && cursor >= maxStreamLength) {
            throw new StreamTooLongException("Form contents was longer than " + maxStreamLength + " bytes");
        }
        int read = read();
//        System.out.write(read);
        return read;
    }

    public long currentByteIndex() {
        return cursor;
    }
}
