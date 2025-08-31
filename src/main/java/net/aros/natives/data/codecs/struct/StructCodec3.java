package net.aros.natives.data.codecs.struct;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;
import net.aros.natives.data.util.Function3;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.function.Function;

@SuppressWarnings("preview")
public class StructCodec3<C, T1, T2, T3> implements MemCodec<C> {
    private final MemCodec<T1> codec1;
    private final Function<C, T1> from1;
    private final MemCodec<T2> codec2;
    private final Function<C, T2> from2;
    private final MemCodec<T3> codec3;
    private final Function<C, T3> from3;
    private final Function3<T1, T2, T3, C> to;

    public StructCodec3(MemCodec<T1> codec1, Function<C, T1> from1,
                        MemCodec<T2> codec2, Function<C, T2> from2,
                        MemCodec<T3> codec3, Function<C, T3> from3,
                        Function3<T1, T2, T3, C> to) {
        this.codec1 = codec1;
        this.from1 = from1;
        this.codec2 = codec2;
        this.from2 = from2;
        this.codec3 = codec3;
        this.from3 = from3;
        this.to = to;
    }

    @Override
    public C decode(MemorySegment segment, long startOffset) {
        long offset = startOffset;
        T1 r1 = codec1.decode(segment, offset);
        T2 r2 = codec2.decode(segment, offset += codec1.byteSize());
        T3 r3 = codec3.decode(segment, offset + codec2.byteSize());
        return to.apply(r1, r2, r3);
    }

    @Override
    public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
        long offset = startOffset;
        codec1.encode(arena, segment, offset, from1.apply(value));
        codec2.encode(arena, segment, offset += codec1.byteSize(), from2.apply(value));
        codec3.encode(arena, segment, offset + codec2.byteSize(), from3.apply(value));
    }

    @Override
    public long byteSize() {
        return codec1.byteSize() + codec2.byteSize() + codec3.byteSize();
    }

    @Override
    public MemoryLayout getLayout() {
        return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout(), codec3.getLayout());
    }

    @Override
    public Class<?> getNativeArgumentClass() {
        return AnNativeValue.class;
    }
}
