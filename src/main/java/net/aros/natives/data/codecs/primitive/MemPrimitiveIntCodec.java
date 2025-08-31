package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.IntArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveIntCodec extends BaseCodec<Integer> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_INT);

    public MemPrimitiveIntCodec() {
        super(int.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (int) HANDLE.get(segment, offset), ValueLayout.JAVA_INT);
    }

    public @NotNull IntArrayCodec array(int length) {
        return new IntArrayCodec(length);
    }
}
