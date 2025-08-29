package net.aros.natives.data.util;

import java.lang.foreign.MemorySegment;

@SuppressWarnings("preview")
public interface MemDecoder<T> {
    T decode(MemorySegment segment, long offset);

    default T decodeStart(MemorySegment segment) {
        return decode(segment, 0L);
    }
}

