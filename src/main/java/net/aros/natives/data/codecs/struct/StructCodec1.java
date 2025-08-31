package net.aros.natives.data.codecs.struct;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.function.Function;

@SuppressWarnings("preview")
public class StructCodec1<C, T1> implements MemCodec<C> {
    private final MemCodec<T1> codec1;
    private final Function<C, T1> from1;
    private final Function<T1, C> to;

    public StructCodec1(MemCodec<T1> codec1, Function<C, T1> from1, Function<T1, C> to) {
        this.codec1 = codec1;
        this.from1 = from1;
        this.to = to;
    }

    @Override
    public C decode(MemorySegment segment, long startOffset) {
        T1 r1 = codec1.decode(segment, startOffset);
        return to.apply(r1);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
        codec1.encode(arena, segment, startOffset, from1.apply(value));
    }

    @Override
    public long byteSize() {
        return codec1.byteSize();
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.structLayout(codec1.getLayout());
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
