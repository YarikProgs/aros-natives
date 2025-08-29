package net.aros.natives.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

@SuppressWarnings("preview")
public final class AnNativeValue<T> {
    private final MemCodec<T> codec;
    @Nullable
    private T value;
    private boolean immutable;

    AnNativeValue(MemCodec<T> codec, @Nullable T value) {
        this.codec = Objects.requireNonNull(codec);
        set(value);
    }

    public @NotNull MemorySegment allocateAndEncode(@NotNull Arena arena) {
        return codec.allocateAndEncode(arena, value);
    }

    public void decode(@NotNull MemorySegment segment) {
        set(codec.decodeStart(segment));
    }

    @Nullable
    public T get() {
        return value;
    }

    private void set(@Nullable T value) {
        this.value = value;
    }

    public AnNativeValue<T> immutable() {
        this.immutable = true;
        return this;
    }

    public boolean isImmutable() {
        return immutable;
    }
}
