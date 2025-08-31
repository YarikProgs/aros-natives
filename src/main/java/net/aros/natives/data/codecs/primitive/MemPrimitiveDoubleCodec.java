package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.DoubleArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveDoubleCodec extends BaseCodec<Double> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_DOUBLE);

    public MemPrimitiveDoubleCodec() {
        super(double.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (double) HANDLE.get(segment, offset), ValueLayout.JAVA_DOUBLE);
    }

    public @NotNull DoubleArrayCodec array(int length) {
        return new DoubleArrayCodec(length);
    }
}
