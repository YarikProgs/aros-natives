package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.ShortArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveShortCodec extends BaseCodec<Short> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_SHORT);

    public MemPrimitiveShortCodec() {
        super(short.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (short) HANDLE.get(segment, offset), ValueLayout.JAVA_SHORT);
    }

    public @NotNull ShortArrayCodec array(int length) {
        return new ShortArrayCodec(length);
    }
}
