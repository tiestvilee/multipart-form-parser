package org.tiestvilee.multipartform.stream;

import org.tiestvilee.multipartform.exceptions.StreamTooLongException;
import org.tiestvilee.multipartform.exceptions.TokenNotFoundException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class TokenBoundedInputStream {
    private final BufferedInputStream inputStream;
    private final Charset encoding;
    private final int maxStreamLength;
    private long currentByteIndex;
    private long currentMark;

    public TokenBoundedInputStream(InputStream inputStream, int bufSize, Charset encoding) {
        this(inputStream, bufSize, encoding, -1);
    }

    public TokenBoundedInputStream(InputStream inputStream, int bufSize, Charset encoding, int maxStreamLength) {
        this.encoding = encoding;
        this.maxStreamLength = maxStreamLength;
        this.inputStream = new BufferedInputStream(inputStream, bufSize);
        currentByteIndex = 0;
    }

    /**
     * Consumes all bytes up to and including the matched endOfToken bytes. Returns a
     * String made up of those bytes, excluding the matched endOfToken bytes.
     *
     * @param endOfToken           bytes that indicate the end of this token
     * @param maxStringSizeInBytes maximum size of String to return
     * @return a String made of all the bytes consumed, excluding the endOfToken bytes
     * @throws IOException
     */
    public String readStringFromStreamUntilMatched(byte[] endOfToken, int maxStringSizeInBytes) throws IOException {
        // very inefficient search!
        byte[] buffer = new byte[maxStringSizeInBytes];
        int bufferIndex = 0;

        int b;
        while ((b = readFromStream()) > -1 && bufferIndex < maxStringSizeInBytes) {
            byte originalB = (byte) b;
            markStream(endOfToken);
            if (matchToken(endOfToken, b) == endOfToken.length) {
                return new String(buffer, 0, bufferIndex, encoding);
            }
            buffer[bufferIndex++] = originalB;
            resetToMarkInStream();
        }

        if (bufferIndex >= maxStringSizeInBytes) {
            throw new TokenNotFoundException("Didn't find end of Token <<" + new String(endOfToken, encoding) + ">> " +
                "within " + maxStringSizeInBytes + " bytes");
        }

        throw new TokenNotFoundException(
            "Didn't find Token <<" + new String(endOfToken, encoding) + ">>. " +
                "Last " + endOfToken.length + " bytes read were " +
                "<<" + getBytesRead(endOfToken, buffer, bufferIndex) + ">>");
    }

    private String getBytesRead(byte[] endOfToken, byte[] buffer, int bufferIndex) {
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

    private int matchToken(byte[] token, int initialCharacter) throws IOException {
        int eotIndex = 0;
        while (initialCharacter > -1 && ((byte) initialCharacter == token[eotIndex]) && (++eotIndex) < token.length) {
            initialCharacter = readFromStream();
        }
        return eotIndex;
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
        markStream(token);

        if (matchToken(token, readFromStream()) == token.length) {
            return true;
        }

        resetToMarkInStream();
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
        markStream(endOfToken);
        int b = readFromStream();
        int eotIndex = 0;
        while (eotIndex < endOfToken.length && ((byte) b == endOfToken[eotIndex])) {
            b = readFromStream();
            eotIndex++;
        }
        if (eotIndex == endOfToken.length) {
            resetToMarkInStream();
            return -1;
        }
        resetToMarkInStream();
        return readFromStream();
    }


    private void resetToMarkInStream() throws IOException {
        currentByteIndex = currentMark;
//        System.out.println(">> RESET <<");
        inputStream.reset();
    }

    private void markStream(byte[] endOfToken) {
        currentMark = currentByteIndex;
        inputStream.mark(endOfToken.length);
    }

    private int readFromStream() throws IOException {
        currentByteIndex++;
        if (maxStreamLength > -1 && currentByteIndex >= maxStreamLength) {
            throw new StreamTooLongException("Form contents was longer than " + maxStreamLength + " bytes");
        }
        int read = inputStream.read();
//        System.out.write(read);
        return read;
    }

    public long currentByteIndex() {
        return currentByteIndex;
    }
}
