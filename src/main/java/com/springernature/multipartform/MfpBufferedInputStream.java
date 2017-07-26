package com.springernature.multipartform;

import com.springernature.multipartform.exceptions.TokenNotFoundException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MfpBufferedInputStream {
    private final BufferedInputStream inputStream;

    public MfpBufferedInputStream(InputStream inputStream, int bufSize) throws IOException {
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
        while ((b = inputStream.read()) > -1) {
            byte originalB = (byte) b;
            inputStream.mark(endOfToken.length);
            if (matchToken(endOfToken, b) == endOfToken.length) {
                return new String(buffer, 0, bufferIndex);
            }
            buffer[bufferIndex++] = originalB;
            inputStream.reset();
        }

        throw new TokenNotFoundException(
            "Didn't find Token <<" + new String(endOfToken) + ">>. " +
                "Last " + endOfToken.length + " bytes read were " +
                "<<" + new String(buffer, bufferIndex - endOfToken.length, endOfToken.length) + ">>");
    }

    private int matchToken(byte[] endOfToken, int b) throws IOException {
        int eotIndex = 0;
        while (b > -1 && b == endOfToken[eotIndex] && (++eotIndex) < endOfToken.length) {
            b = inputStream.read();
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
        while (eotIndex < endOfToken.length && b == endOfToken[eotIndex]) { b = inputStream.read(); eotIndex++;}
        if (eotIndex == endOfToken.length) {
            inputStream.reset();
            return -1;
        }
        inputStream.reset();
        return inputStream.read();
    }

    public byte[] lastBytes(int numberOfBytes) {
        return "Sorry, not available".getBytes();
    }
}
