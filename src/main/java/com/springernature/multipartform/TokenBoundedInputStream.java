package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.TokenNotFoundException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class TokenBoundedInputStream {
    private final BufferedInputStream inputStream;
    private final Charset encoding;

    public TokenBoundedInputStream(InputStream inputStream, int bufSize, Charset encoding) throws IOException {
        this.encoding = encoding;
        this.inputStream = new BufferedInputStream(inputStream, bufSize);
    }

    /**
     * Consumes all bytes up to and including the matched endOfToken bytes.
     *
     * @param endOfToken bytes that indicate the end of this token.
     * @return true if token found, false if not.
     * @throws IOException
     */
    public boolean dropFromStreamUntilMatched(byte[] endOfToken) throws IOException {
        // very inefficient search!
        int b;
        while ((b = inputStream.read()) > -1) {
            inputStream.mark(endOfToken.length);
            if (matchToken(endOfToken, b) == endOfToken.length) {
                return true;
            }
            inputStream.reset();
        }
        return false;
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
        while ((b = inputStream.read()) > -1 && bufferIndex < maxStringSizeInBytes) {
            byte originalB = (byte) b;
            inputStream.mark(endOfToken.length);
            if (matchToken(endOfToken, b) == endOfToken.length) {
                return new String(buffer, 0, bufferIndex, encoding);
            }
            buffer[bufferIndex++] = originalB;
            inputStream.reset();
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
            initialCharacter = inputStream.read();
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
        inputStream.mark(token.length);

        if (matchToken(token, inputStream.read()) == token.length) {
            return true;
        }

        inputStream.reset();
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
        inputStream.mark(endOfToken.length);
        int b = inputStream.read();
        int eotIndex = 0;
        while (eotIndex < endOfToken.length && ((byte) b == endOfToken[eotIndex])) {
            b = inputStream.read();
            eotIndex++;
        }
        if (eotIndex == endOfToken.length) {
            inputStream.reset();
            return -1;
        }
        inputStream.reset();
        return inputStream.read();
    }
}
