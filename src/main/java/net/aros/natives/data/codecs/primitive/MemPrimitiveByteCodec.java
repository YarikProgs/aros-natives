package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.ByteArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveByteCodec extends BaseCodec<Byte> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_BYTE);

    public MemPrimitiveByteCodec() {
        super(byte.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (byte) HANDLE.get(segment, offset), ValueLayout.JAVA_BYTE);
    }

    public @NotNull ByteArrayCodec array(int length) {
        return new ByteArrayCodec(length);
    }
}
