package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.FloatArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveFloatCodec extends BaseCodec<Float> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_FLOAT);

    public MemPrimitiveFloatCodec() {
        super(float.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (float) HANDLE.get(segment, offset), ValueLayout.JAVA_FLOAT);
    }

    public @NotNull FloatArrayCodec array(int length) {
        return new FloatArrayCodec(length);
    }
}
