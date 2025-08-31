package net.aros.natives.data.codecs.struct;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("preview")
public class StructCodec2<C, T1, T2> implements MemCodec<C> {
    private final MemCodec<T1> codec1;
    private final Function<C, T1> from1;
    private final MemCodec<T2> codec2;
    private final Function<C, T2> from2;
    private final BiFunction<T1, T2, C> to;

    public StructCodec2(MemCodec<T1> codec1, Function<C, T1> from1,
                        MemCodec<T2> codec2, Function<C, T2> from2,
                        BiFunction<T1, T2, C> to) {
        this.codec1 = codec1;
        this.from1 = from1;
        this.codec2 = codec2;
        this.from2 = from2;
        this.to = to;
    }

    @Override
    public C decode(MemorySegment segment, long startOffset) {
        T1 r1 = codec1.decode(segment, startOffset);
        T2 r2 = codec2.decode(segment, startOffset + codec1.byteSize());
        return to.apply(r1, r2);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
        codec1.encode(arena, segment, startOffset, from1.apply(value));
        codec2.encode(arena, segment, startOffset + codec1.byteSize(), from2.apply(value));
    }

    @Override
    public long byteSize() {
        return codec1.byteSize() + codec2.byteSize();
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout());
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
