package net.aros.natives.data.codecs;

import net.aros.natives.data.MemCodec;
import net.aros.natives.data.util.MemDecoder;
import net.aros.natives.data.util.MemEncoder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

@SuppressWarnings("preview")
public class BaseCodec<T> implements MemCodec<T> {
    private final Class<?> type;
    private final MemEncoder<T> encoder;
    private final MemDecoder<T> decoder;
    private final MemoryLayout layout;

    public BaseCodec(Class<?> type, MemEncoder<T> encoder, MemDecoder<T> decoder, MemoryLayout layout) {
        this.type = type;
        this.encoder = encoder;
        this.decoder = decoder;
        this.layout = layout;
    }

    @Override
    public T decode(MemorySegment segment, long offset) {
        return decoder.decode(segment, offset);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long offset, T element) {
        encoder.encode(arena, segment, offset, element);
    }

    @Override
    public long byteSize() {
        return layout.byteSize();
    }

    @Override
    public MemoryLayout getLayout() {
        return layout;
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return type;
    }
}
