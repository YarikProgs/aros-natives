package net.aros.natives.data.util;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

@SuppressWarnings("preview")
public interface MemEncoder<T> {
    void encode(Arena arena, MemorySegment segment, long offset, T element);

    default void encodeStart(Arena arena, MemorySegment segment, T element) {
        encode(arena, segment, 0L, element);
    }
}