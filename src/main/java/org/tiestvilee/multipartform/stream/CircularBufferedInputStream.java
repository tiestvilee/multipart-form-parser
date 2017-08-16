package org.tiestvilee.multipartform.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.InvalidMarkException;

public class CircularBufferedInputStream extends InputStream {
    private final int bufferSize;
    private final long bufferIndexMask;
    private final byte[] buffer;
    private final InputStream inputStream;

    private long cursor;
    private long rightBounds;
    private long leftBounds;
    private long readLimit;
    private boolean markInvalid;
    private boolean EOS;

    public CircularBufferedInputStream(InputStream inputStream, int maxExpectedBufSize) {
        this.bufferSize = Integer.highestOneBit(maxExpectedBufSize);
        this.bufferIndexMask = bufferSize - 1;
//        System.out.println("CREATING " + maxExpectedBufSize + " -> " + bufferSize + " -> " + (bufferSize - 1));
        this.buffer = new byte[bufferSize];
        this.inputStream = inputStream;
        this.cursor = 0;
        this.rightBounds = 0;
        this.leftBounds = 0;
        this.readLimit = 0;
        this.markInvalid = false;
        this.EOS = false;
    }

    @Override public int read() throws IOException {
//        System.out.println(">>> READ");
//        dumpState();
        if (EOS) {
            return -1;
        }

        while (cursor == rightBounds) {
            if (!readMore()) {
                return -1;
            }
        }
        return buffer[(int) (cursor++ & bufferIndexMask)] & 0x0FF;
    }

    private boolean readMore() throws IOException {
        long rightIndex = rightBounds & bufferIndexMask;
        long leftIndex = leftBounds & bufferIndexMask;
        int readBytes = inputStream.read(
            buffer,
            (int) rightIndex,
            leftIndex > rightIndex ? (int) (leftIndex - rightIndex) : (int) (buffer.length - rightIndex)
        );

        if (readBytes < 0) {
            EOS = true;
            return false;
        }
        rightBounds += readBytes;

        // move mark if past readLimit
        if (cursor - leftBounds > readLimit) {
            cursor = leftBounds - readLimit;
            markInvalid = true;
        }

        return true;
    }

    @Override public int available() throws IOException {
        return (int) (rightBounds - cursor);
    }

    @Override public boolean markSupported() {
        return true;
    }

    @Override public synchronized void reset() throws IOException {
//        System.out.println(">>> RESET");
//        dumpState();
        if (markInvalid) {
            // The mark has been moved because you have read past your readlimit
            throw new InvalidMarkException();
        }
        cursor = leftBounds;
    }

    @Override public synchronized void mark(int readlimit) {
//        System.out.println(">>> MARK");
//        dumpState();
        leftBounds = cursor;
        markInvalid = false;
        this.readLimit = readlimit;
    }

    private void dumpState() {
        System.out.println("l=" + leftBounds + " c=" + cursor + " r=" + rightBounds + " '" +
            (char) buffer[(int) (cursor & bufferIndexMask)] + "'");
    }
}
