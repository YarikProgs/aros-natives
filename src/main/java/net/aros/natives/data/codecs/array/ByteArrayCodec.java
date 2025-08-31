package net.aros.natives.data.codecs.array;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@SuppressWarnings("preview")
public class ByteArrayCodec implements MemCodec<byte[]> {
    private final int length;

    public ByteArrayCodec(int length) {
        this.length = length;
    }

    @Override
    public byte[] decode(@NotNull MemorySegment segment, long offset) {
        return segment.asSlice(offset, length * Byte.BYTES).toArray(ValueLayout.JAVA_BYTE);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long offset, byte[] element) {
        if (element == null || element.length != length) {
            throw new IllegalArgumentException("Array must be non-null and with length of %d".formatted(length));
        }
        segment.asSlice(offset, length * Byte.BYTES).copyFrom(MemorySegment.ofArray(element));
    }

    @Override
    public long byteSize() {
        return (long) length * Byte.BYTES;
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.sequenceLayout(length, ValueLayout.JAVA_BYTE);
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
