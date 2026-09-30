package com.github.lunatrius.schematica.compat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

final class VisualStreams {
    static final int MAX_BYTES = 2 * 1024 * 1024;

    private VisualStreams() {}

    static byte[] writeMultipart(Object part) throws Exception {
        ByteBuf buffer = Unpooled.buffer(256, MAX_BYTES + 1);
        try {
            Object stream = multipartStream(part, buffer.writeByte(1));
            Class<?> output = Class.forName("codechicken.lib.data.MCDataOutput", false, part.getClass().getClassLoader());
            Reflect.call(part, "writeDesc", new Class<?>[] {output}, stream);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally { buffer.release(); }
    }

    static void readMultipart(Object part, byte[] bytes) throws Exception {
        checkSize(bytes.length);
        ByteBuf buffer = Unpooled.buffer(bytes.length + 1, bytes.length + 1);
        try {
            Object stream = multipartStream(part, buffer.writeByte(1).writeBytes(bytes));
            Class<?> input = Class.forName("codechicken.lib.data.MCDataInput", false, part.getClass().getClassLoader());
            Reflect.call(part, "readDesc", new Class<?>[] {input}, stream);
            if (buffer.isReadable()) throw new IOException("Unconsumed multipart visual data");
        } finally { buffer.release(); }
    }

    private static Object multipartStream(Object part, ByteBuf buffer) throws ReflectiveOperationException {
        return Class.forName("codechicken.lib.packet.PacketCustom", false, part.getClass().getClassLoader())
            .getConstructor(ByteBuf.class).newInstance(buffer);
    }

    static byte[] writeBuffer(Object target, String method) throws ReflectiveOperationException {
        return writeBuffer(target, method, null);
    }

    static byte[] writeBuffer(Object target, String method, String wrapperClass) throws ReflectiveOperationException {
        ByteBuf buffer = Unpooled.buffer(256, MAX_BYTES);
        try {
            callBuffer(target, method, wrapperClass, buffer);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    static void readBuffer(Object target, String method, byte[] bytes) throws Exception {
        readBuffer(target, method, null, bytes);
    }

    static void readBuffer(Object target, String method, String wrapperClass, byte[] bytes) throws Exception {
        checkSize(bytes.length);
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        try {
            callBuffer(target, method, wrapperClass, buffer);
            if (buffer.isReadable()) throw new IOException("Unconsumed visual update data");
        } finally {
            buffer.release();
        }
    }

    private static void callBuffer(Object target, String method, String wrapperClass, ByteBuf buffer)
        throws ReflectiveOperationException {
        Class<?> type = wrapperClass == null ? ByteBuf.class
            : Class.forName(wrapperClass, false, target.getClass().getClassLoader());
        Object stream = wrapperClass == null ? buffer : type.getConstructor(ByteBuf.class).newInstance(buffer);
        Reflect.call(target, method, new Class<?>[] {type}, stream);
    }

    static byte[] writeStream(Object target, String method, String streamClass) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream() {
            @Override public synchronized void write(int value) {
                checkSize((long) count + 1);
                super.write(value);
            }
            @Override public synchronized void write(byte[] data, int offset, int length) {
                checkSize((long) count + length);
                super.write(data, offset, length);
            }
        };
        Class<?> type = Class.forName(streamClass, false, target.getClass().getClassLoader());
        try (OutputStream stream = (OutputStream) type.getConstructor(OutputStream.class).newInstance(bytes)) {
            Reflect.call(target, method, new Class<?>[] { type }, stream);
        }
        return bytes.toByteArray();
    }

    static void readStream(Object target, String method, String streamClass, byte[] bytes) throws Exception {
        checkSize(bytes.length);
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        Class<?> type = Class.forName(streamClass, false, target.getClass().getClassLoader());
        try (InputStream stream = (InputStream) type.getConstructor(InputStream.class).newInstance(input)) {
            Reflect.call(target, method, new Class<?>[] { type }, stream);
            if (input.available() != 0) throw new IOException("Unconsumed visual update data");
        }
    }

    private static void checkSize(long size) {
        if (size < 0 || size > MAX_BYTES) throw new IllegalArgumentException("Visual update exceeds 2 MiB");
    }
}
