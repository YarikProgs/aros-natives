package net.aros.natives.data.codecs;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;
import net.aros.natives.data.MemCodecs;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@SuppressWarnings("preview")
public class PointerCodec<T> implements MemCodec<T> {
    private final MemCodec<T> elementCodec;

    public PointerCodec(MemCodec<T> elementCodec) {
        this.elementCodec = elementCodec;
    }

    @Override
    public long byteSize() {
        return ValueLayout.ADDRESS.byteSize();
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }

    @Override
    public MemoryLayout getLayout() {
        return ValueLayout.ADDRESS;
    }

    @Override
    public T decode(MemorySegment segment, long offset) {
        MemorySegment value = MemCodecs.ADDRESS.decode(segment, offset);
        return value == MemorySegment.NULL ? null : elementCodec.decodeStart(value.reinterpret(elementCodec.byteSize()));
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long offset, T element) {
        MemorySegment value = element == null ? MemorySegment.NULL : elementCodec.allocateAndEncode(arena, element);
        MemCodecs.ADDRESS.encode(arena, segment, offset, value);
    }
}
