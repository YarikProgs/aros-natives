package net.aros.natives.data.codecs.array;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@SuppressWarnings("preview")
public class FloatArrayCodec implements MemCodec<float[]> {
    private final int length;

    public FloatArrayCodec(int length) {
        this.length = length;
    }

    @Override
    public float[] decode(@NotNull MemorySegment segment, long offset) {
        return segment.asSlice(offset, (long) length * Float.BYTES).toArray(ValueLayout.JAVA_FLOAT);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long offset, float[] element) {
        if (element == null || element.length != length) {
            throw new IllegalArgumentException("Array must be non-null and with length of %d".formatted(length));
        }
        segment.asSlice(offset, (long) length * Float.BYTES).copyFrom(MemorySegment.ofArray(element));
    }

    @Override
    public long byteSize() {
        return (long) length * Float.BYTES;
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.sequenceLayout(length, ValueLayout.JAVA_FLOAT);
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
