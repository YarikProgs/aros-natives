package net.aros.natives.data;

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
        return new MemCodec<>() {
            @Override
            public T decode(MemorySegment segment, long offset) {
                return decoder.decode(segment, offset);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long offset, T element) {
                encoder.encode(arena, segment, offset, element);
            }

            @Override
            public long byteSize() {
                return layout.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return layout;
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return type;
            }
        };
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
        long size = byteSize();
        return new MemCodec<>() {
            @Override
            public long byteSize() {
                return ValueLayout.ADDRESS.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return ValueLayout.ADDRESS;
            }

            @Override
            public T decode(MemorySegment segment, long offset) {
                MemorySegment value = MemCodecs.ADDRESS.decode(segment, offset);
                return value == MemorySegment.NULL ? null : MemCodec.this.decodeStart(value.reinterpret(size));
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long offset, T element) {
                MemorySegment value = element == null ? MemorySegment.NULL : MemCodec.this.allocateAndEncode(arena, element);
                MemCodecs.ADDRESS.encode(arena, segment, offset, value);
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    default @NotNull MemCodec<T[]> array(IntFunction<T[]> arrayFactory, int length) {
        if (length < 0) throw new IllegalArgumentException("Length must be non-negative");
        long size = byteSize();
        return new MemCodec<>() {
            @Override
            public T[] decode(MemorySegment segment, long offset) {
                T[] array = arrayFactory.apply(length);
                for (int i = 0; i < length; i++) {
                    array[i] = MemCodec.this.decode(segment, offset + size * i);
                }
                return array;
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long offset, T[] element) {
                if (element == null || element.length != length) {
                    throw new IllegalArgumentException("Array must be non-null and with length of %d".formatted(length));
                }
                for (int i = 0; i < length; i++) {
                    MemCodec.this.encode(arena, segment, offset + size * i, element[i]);
                }
            }

            @Override
            public long byteSize() {
                return length * size;
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.sequenceLayout(length, MemCodec.this.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    static <C, T1> @NotNull MemCodec<C> tuple(
            MemCodec<T1> codec1, Function<C, T1> from1,
            Function<T1, C> to
    ) {
        return new MemCodec<>() {
            @Override
            public C decode(MemorySegment segment, long startOffset) {
                T1 r1 = codec1.decode(segment, startOffset);
                return to.apply(r1);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
                codec1.encode(arena, segment, startOffset, from1.apply(value));
            }

            @Override
            public long byteSize() {
                return codec1.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.structLayout(codec1.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    static <C, T1, T2> @NotNull MemCodec<C> tuple(
            MemCodec<T1> codec1, Function<C, T1> from1,
            MemCodec<T2> codec2, Function<C, T2> from2,
            BiFunction<T1, T2, C> to
    ) {
        return new MemCodec<>() {
            @Override
            public C decode(MemorySegment segment, long startOffset) {
                T1 r1 = codec1.decode(segment, startOffset);
                T2 r2 = codec2.decode(segment, startOffset + codec1.byteSize());
                return to.apply(r1, r2);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
                codec1.encode(arena, segment, startOffset, from1.apply(value));
                codec2.encode(arena, segment, startOffset + codec1.byteSize(), from2.apply(value));
            }

            @Override
            public long byteSize() {
                return codec1.byteSize() + codec2.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    @Contract(value = "_, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3> @NotNull MemCodec<C> tuple(
            MemCodec<T1> codec1, Function<C, T1> from1,
            MemCodec<T2> codec2, Function<C, T2> from2,
            MemCodec<T3> codec3, Function<C, T3> from3,
            Function3<T1, T2, T3, C> to
    ) {
        return new MemCodec<>() {
            @Override
            public C decode(MemorySegment segment, long startOffset) {
                long offset = startOffset;
                T1 r1 = codec1.decode(segment, offset);
                T2 r2 = codec2.decode(segment, offset += codec1.byteSize());
                T3 r3 = codec3.decode(segment, offset + codec2.byteSize());
                return to.apply(r1, r2, r3);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
                long offset = startOffset;
                codec1.encode(arena, segment, offset, from1.apply(value));
                codec2.encode(arena, segment, offset += codec1.byteSize(), from2.apply(value));
                codec3.encode(arena, segment, offset + codec2.byteSize(), from3.apply(value));
            }

            @Override
            public long byteSize() {
                return codec1.byteSize() + codec2.byteSize() + codec3.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout(), codec3.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    @Contract(value = "_, _, _, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3, T4> @NotNull MemCodec<C> tuple(
            MemCodec<T1> codec1, Function<C, T1> from1,
            MemCodec<T2> codec2, Function<C, T2> from2,
            MemCodec<T3> codec3, Function<C, T3> from3,
            MemCodec<T4> codec4, Function<C, T4> from4,
            Function4<T1, T2, T3, T4, C> to
    ) {
        return new MemCodec<>() {
            @Override
            public C decode(MemorySegment segment, long startOffset) {
                long offset = startOffset;
                T1 r1 = codec1.decode(segment, offset);
                T2 r2 = codec2.decode(segment, offset += codec1.byteSize());
                T3 r3 = codec3.decode(segment, offset += codec2.byteSize());
                T4 r4 = codec4.decode(segment, offset + codec3.byteSize());
                return to.apply(r1, r2, r3, r4);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
                long offset = startOffset;
                codec1.encode(arena, segment, offset, from1.apply(value));
                codec2.encode(arena, segment, offset += codec1.byteSize(), from2.apply(value));
                codec3.encode(arena, segment, offset += codec2.byteSize(), from3.apply(value));
                codec4.encode(arena, segment, offset + codec3.byteSize(), from4.apply(value));
            }

            @Override
            public long byteSize() {
                return codec1.byteSize() + codec2.byteSize() + codec3.byteSize() + codec4.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout(), codec3.getLayout(), codec4.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    @Contract(value = "_, _, _, _, _, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3, T4, T5> @NotNull MemCodec<C> tuple(
            MemCodec<T1> codec1, Function<C, T1> from1,
            MemCodec<T2> codec2, Function<C, T2> from2,
            MemCodec<T3> codec3, Function<C, T3> from3,
            MemCodec<T4> codec4, Function<C, T4> from4,
            MemCodec<T5> codec5, Function<C, T5> from5,
            Function5<T1, T2, T3, T4, T5, C> to
    ) {
        return new MemCodec<>() {
            @Override
            public C decode(MemorySegment segment, long startOffset) {
                long offset = startOffset;
                T1 r1 = codec1.decode(segment, offset);
                T2 r2 = codec2.decode(segment, offset += codec1.byteSize());
                T3 r3 = codec3.decode(segment, offset += codec2.byteSize());
                T4 r4 = codec4.decode(segment, offset += codec3.byteSize());
                T5 r5 = codec5.decode(segment, offset + codec4.byteSize());
                return to.apply(r1, r2, r3, r4, r5);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
                long offset = startOffset;
                codec1.encode(arena, segment, offset, from1.apply(value));
                codec2.encode(arena, segment, offset += codec1.byteSize(), from2.apply(value));
                codec3.encode(arena, segment, offset += codec2.byteSize(), from3.apply(value));
                codec4.encode(arena, segment, offset += codec3.byteSize(), from4.apply(value));
                codec5.encode(arena, segment, offset + codec4.byteSize(), from5.apply(value));
            }

            @Override
            public long byteSize() {
                return codec1.byteSize() + codec2.byteSize() + codec3.byteSize() + codec4.byteSize() + codec5.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout(), codec3.getLayout(), codec4.getLayout(), codec5.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
    }

    @Contract(value = "_, _, _, _, _, _, _, _, _, _, _, _, _ -> new", pure = true)
    static <C, T1, T2, T3, T4, T5, T6> @NotNull MemCodec<C> tuple(
            MemCodec<T1> codec1, Function<C, T1> from1,
            MemCodec<T2> codec2, Function<C, T2> from2,
            MemCodec<T3> codec3, Function<C, T3> from3,
            MemCodec<T4> codec4, Function<C, T4> from4,
            MemCodec<T5> codec5, Function<C, T5> from5,
            MemCodec<T6> codec6, Function<C, T6> from6,
            Function6<T1, T2, T3, T4, T5, T6, C> to
    ) {
        return new MemCodec<>() {
            @Override
            public C decode(MemorySegment segment, long startOffset) {
                long offset = startOffset;
                T1 r1 = codec1.decode(segment, offset);
                T2 r2 = codec2.decode(segment, offset += codec1.byteSize());
                T3 r3 = codec3.decode(segment, offset += codec2.byteSize());
                T4 r4 = codec4.decode(segment, offset += codec3.byteSize());
                T5 r5 = codec5.decode(segment, offset += codec4.byteSize());
                T6 r6 = codec6.decode(segment, offset + codec5.byteSize());
                return to.apply(r1, r2, r3, r4, r5, r6);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long startOffset, C value) {
                long offset = startOffset;
                codec1.encode(arena, segment, offset, from1.apply(value));
                codec2.encode(arena, segment, offset += codec1.byteSize(), from2.apply(value));
                codec3.encode(arena, segment, offset += codec2.byteSize(), from3.apply(value));
                codec4.encode(arena, segment, offset += codec3.byteSize(), from4.apply(value));
                codec5.encode(arena, segment, offset += codec4.byteSize(), from5.apply(value));
                codec6.encode(arena, segment, offset + codec5.byteSize(), from6.apply(value));
            }

            @Override
            public long byteSize() {
                return codec1.byteSize() + codec2.byteSize() + codec3.byteSize() + codec4.byteSize() + codec5.byteSize() + codec6.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.structLayout(codec1.getLayout(), codec2.getLayout(), codec3.getLayout(), codec4.getLayout(), codec5.getLayout(), codec6.getLayout());
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return AnNativeValue.class;
            }
        };
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