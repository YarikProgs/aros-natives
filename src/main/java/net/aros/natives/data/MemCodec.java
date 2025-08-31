package net.aros.natives.data;

import net.aros.natives.data.codecs.BaseCodec;
import net.aros.natives.data.codecs.PointerCodec;
import net.aros.natives.data.codecs.array.ArrayCodec;
import net.aros.natives.data.codecs.struct.*;
import net.aros.natives.data.util.*;
import net.aros.natives.util.AnUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings("preview")
public interface MemCodec<T> extends MemEncoder<T>, MemDecoder<T> {
    @SuppressWarnings("unchecked")
    static <T> @NotNull MemCodec<T> of(Class<T> type, ValueLayout layout) {
        if (layout != ValueLayout.ADDRESS && !type.isPrimitive()) {
            throw new IllegalArgumentException("Illegal type provided for %s: %s. Either use primitive type or address layout".formatted(type.getName(), layout.toString()));
        }
        VarHandle handle = MethodHandles.memorySegmentViewVarHandle(layout);
        return of(
                type.isPrimitive() ? type : AnNativeValue.class,
                (_, segment, offset, value) -> handle.set(segment, offset, value),
                (segment, offset) -> (T) handle.get(segment, offset),
                layout
        );
    }

    @Contract(value = "_, _, _, _ -> new", pure = true)
    static <T> @NotNull MemCodec<T> of(Class<?> type, MemEncoder<T> encoder, MemDecoder<T> decoder, MemoryLayout layout) {
        return new BaseCodec<>(type, encoder, decoder, layout);
    }

    default <O> MemCodec<O> map(Function<? super T, ? extends O> to, Function<? super O, ? extends T> from) {
        return new MemCodec<>() {
            @Override
            public O decode(MemorySegment segment, long offset) {
                return to.apply(MemCodec.this.decode(segment, offset));
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long offset, O element) {
                MemCodec.this.encode(arena, segment, offset, from.apply(element));
            }

            @Override
            public long byteSize() {
                return MemCodec.this.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemCodec.this.getLayout();
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return MemCodec.this.getNativeArgumentClass();
            }
        };
    }

    default MemCodec<T> pointer() {
        return new PointerCodec<>(this);
    }

    default @NotNull MemCodec<T[]> array(IntFunction<T[]> arrayFactory, int length) {
        if (length < 0) throw new IllegalArgumentException("Length must be non-negative");
        return new ArrayCodec<>(this, arrayFactory, length);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    static <C, T1> @NotNull MemCodec<C> struct(MemCodec<T1> codec1, Function<C, T1> from1,
                                              Function<T1, C> to) {
        return new StructCodec1<>(codec1, from1, to);
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    static <C, T1, T2> @NotNull MemCodec<C> struct(MemCodec<T1> codec1, Function<C, T1> from1,
                                                  MemCodec<T2> codec2, Function<C, T2> from2,
                                                  BiFunction<T1, T2, C> to) {
        return new StructCodec2<>(codec1, from1, codec2, from2, to);
    }

    @Contract(value = "_, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3> @NotNull MemCodec<C> struct(MemCodec<T1> codec1, Function<C, T1> from1,
                                                      MemCodec<T2> codec2, Function<C, T2> from2,
                                                      MemCodec<T3> codec3, Function<C, T3> from3,
                                                      Function3<T1, T2, T3, C> to) {
        return new StructCodec3<>(codec1, from1, codec2, from2, codec3, from3, to);
    }

    @Contract(value = "_, _, _, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3, T4> @NotNull MemCodec<C> struct(MemCodec<T1> codec1, Function<C, T1> from1,
                                                          MemCodec<T2> codec2, Function<C, T2> from2,
                                                          MemCodec<T3> codec3, Function<C, T3> from3,
                                                          MemCodec<T4> codec4, Function<C, T4> from4,
                                                          Function4<T1, T2, T3, T4, C> to) {
        return new StructCodec4<>(codec1, from1, codec2, from2, codec3, from3, codec4, from4, to);
    }

    @Contract(value = "_, _, _, _, _, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3, T4, T5> @NotNull MemCodec<C> struct(MemCodec<T1> codec1, Function<C, T1> from1,
                                                              MemCodec<T2> codec2, Function<C, T2> from2,
                                                              MemCodec<T3> codec3, Function<C, T3> from3,
                                                              MemCodec<T4> codec4, Function<C, T4> from4,
                                                              MemCodec<T5> codec5, Function<C, T5> from5,
                                                              Function5<T1, T2, T3, T4, T5, C> to
    ) {
        return new StructCodec5<>(codec1, from1, codec2, from2, codec3, from3, codec4, from4, codec5, from5, to);
    }

    @Contract(value = "_, _, _, _, _, _, _, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3, T4, T5, T6> @NotNull MemCodec<C> struct(MemCodec<T1> codec1, Function<C, T1> from1,
                                                                  MemCodec<T2> codec2, Function<C, T2> from2,
                                                                  MemCodec<T3> codec3, Function<C, T3> from3,
                                                                  MemCodec<T4> codec4, Function<C, T4> from4,
                                                                  MemCodec<T5> codec5, Function<C, T5> from5,
                                                                  MemCodec<T6> codec6, Function<C, T6> from6,
                                                                  Function6<T1, T2, T3, T4, T5, T6, C> to
    ) {
        return new StructCodec6<>(codec1, from1, codec2, from2, codec3, from3, codec4, from4, codec5, from5, codec6, from6, to);
    }

    default MemorySegment allocateAndEncode(@NotNull Arena arena, T element) {
        return AnUtils.make(arena.allocate(byteSize()), segment -> encodeStart(arena, segment, element));
    }

    default AnNativeValue<T> nullPointer() {
        return new AnNativeValue<>(this, null);
    }

    default AnNativeValue<T> value(T value) {
        return new AnNativeValue<>(this, value);
    }

    long byteSize();

    Class<?> getNativeArgumentClass();

    MemoryLayout getLayout();
}