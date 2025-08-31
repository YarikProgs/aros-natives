package net.aros.natives.data.codecs.array;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.function.IntFunction;

@SuppressWarnings("preview")
public class ArrayCodec<T> implements MemCodec<T[]> {
    private final MemCodec<T> elementCodec;
    private final IntFunction<T[]> factory;
    private final int length;

    public ArrayCodec(MemCodec<T> elementCodec, IntFunction<T[]> factory, int length) {
        this.elementCodec = elementCodec;
        this.factory = factory;
        this.length = length;
    }

    @Override
    public T[] decode(MemorySegment segment, long offset) {
        T[] array = factory.apply(length);
        for (int i = 0; i < length; i++) {
            array[i] = elementCodec.decode(segment, offset + elementCodec.byteSize() * i);
        }
        return array;
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long offset, T[] element) {
        if (element == null || element.length != length) {
            throw new IllegalArgumentException("Array must be non-null and with length of %d".formatted(length));
        }
        for (int i = 0; i < length; i++) {
            elementCodec.encode(arena, segment, offset + elementCodec.byteSize() * i, element[i]);
        }
    }

    @Override
    public long byteSize() {
        return length * elementCodec.byteSize();
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.sequenceLayout(length, elementCodec.getLayout());
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
