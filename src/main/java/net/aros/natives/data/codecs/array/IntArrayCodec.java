package net.aros.natives.data.codecs.array;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;
import net.aros.natives.data.MemCodecs;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@SuppressWarnings("preview")
public class IntArrayCodec implements MemCodec<int[]> {
    private final int length;

    public IntArrayCodec(int length) {
        this.length = length;
    }

    @Override
    public int[] decode(@NotNull MemorySegment segment, long offset) {
        return segment.asSlice(offset, (long) length * Integer.BYTES).toArray(ValueLayout.JAVA_INT);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long offset, int[] element) {
        if (element == null || element.length != length) {
            throw new IllegalArgumentException("Array must be non-null and with length of %d".formatted(length));
        }
        segment.asSlice(offset, (long) length * Integer.BYTES).copyFrom(MemorySegment.ofArray(element));
    }

    @Override
    public long byteSize() {
        return (long) length * Integer.BYTES;
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.sequenceLayout(length, ValueLayout.JAVA_INT);
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
