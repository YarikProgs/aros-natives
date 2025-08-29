package net.aros.natives.implementor.utils;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

@SuppressWarnings("preview")
public class InternalMemoryUtils {
    @SuppressWarnings("unused") // in bytecode
    public static Object toMemoryUnsafe(Arena arena, Object obj) {
        if (obj instanceof AnNativeValue<?> value) return value.allocateAndEncode(arena);
        return obj;
    }

    @SuppressWarnings("unused") // in bytecode
    public static void fromMemoryUnsafe(Object holder, Object got) {
        if (!(holder instanceof AnNativeValue<?> ptr) || ptr.isImmutable() || !(got instanceof MemorySegment segment)) return;

        try {
            ptr.decode(segment);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set value in pointer. Ptr: `%s`, Value: `%s`".formatted(holder.toString(), got.toString()), t);
        }
    }

    @SuppressWarnings("unused") // in bytecode
    public static Object resultFromMemUnsafe(Object object, Object objCodec) {
        if (!(object instanceof MemorySegment segment)) return object;
        if (!(objCodec instanceof MemCodec<?> codec))
            throw new IllegalArgumentException("Expected second parameter to be codec, got: %s".formatted(objCodec));

        try {
            return codec.decodeStart(segment);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to decode value", t);
        }
    }
}
