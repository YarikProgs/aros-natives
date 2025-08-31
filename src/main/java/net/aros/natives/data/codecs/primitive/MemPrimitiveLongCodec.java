package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.LongArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveLongCodec extends BaseCodec<Long> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_LONG);

    public MemPrimitiveLongCodec() {
        super(long.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (long) HANDLE.get(segment, offset), ValueLayout.JAVA_LONG);
    }

    public @NotNull LongArrayCodec array(int length) {
        return new LongArrayCodec(length);
    }
}
