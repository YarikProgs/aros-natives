package net.aros.natives.data.codecs.primitive;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.array.CharArrayCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@SuppressWarnings("preview")
public class MemPrimitiveCharCodec extends BaseCodec<Character> {
    private static final VarHandle HANDLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_CHAR);

    public MemPrimitiveCharCodec() {
        super(char.class,
                (_, segment, offset, value) -> HANDLE.set(segment, offset, value),
                (segment, offset) -> (char) HANDLE.get(segment, offset), ValueLayout.JAVA_CHAR);
    }

    public @NotNull CharArrayCodec array(int length) {
        return new CharArrayCodec(length);
    }
}
