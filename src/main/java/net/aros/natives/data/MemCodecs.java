package net.aros.natives.data;

import net.aros.natives.data.codecs.primitive.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@SuppressWarnings("preview")
public final class MemCodecs {

    private MemCodecs() {
    }

    public static final MemPrimitiveByteCodec BYTE = new MemPrimitiveByteCodec();
    public static final MemPrimitiveCharCodec CHAR = new MemPrimitiveCharCodec();
    public static final MemPrimitiveShortCodec SHORT = new MemPrimitiveShortCodec();
    public static final MemPrimitiveIntCodec INT = new MemPrimitiveIntCodec();
    public static final MemPrimitiveLongCodec LONG = new MemPrimitiveLongCodec();
    public static final MemPrimitiveFloatCodec FLOAT = new MemPrimitiveFloatCodec();
    public static final MemPrimitiveDoubleCodec DOUBLE = new MemPrimitiveDoubleCodec();
    public static final MemCodec<Boolean> BOOLEAN = MemCodec.of(boolean.class, ValueLayout.JAVA_BOOLEAN);
    public static final MemCodec<MemorySegment> ADDRESS = MemCodec.of(MemorySegment.class, ValueLayout.ADDRESS);

    public static final MemCodec<String> STRING_128_UTF8 = stringUtf8(128);
    public static final MemCodec<String> STRING_256_UTF8 = stringUtf8(256);
    public static final MemCodec<String> STRING_512_UTF8 = stringUtf8(512);
    public static final MemCodec<String> STRING_1024_UTF8 = stringUtf8(1024);
    public static final MemCodec<String> STRING_2048_UTF8 = stringUtf8(2048);

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull MemCodec<String> stringUtf8(int maxLength) {
        return new MemCodec<>() {
            @Override
            public long byteSize() {
                return maxLength * ValueLayout.JAVA_BYTE.byteSize();
            }

            @Override
            public MemoryLayout getLayout() {
                return MemoryLayout.sequenceLayout(ValueLayout.JAVA_CHAR);
            }

            @Override
            public String decode(MemorySegment segment, long offset) {
                return segment.getUtf8String(offset);
            }

            @Override
            public void encode(Arena arena, MemorySegment segment, long offset, String element) {
                if (element == null || element.length() > maxLength - 1) {
                    throw new IllegalArgumentException("Array must be non-null and not bigger than %d".formatted(maxLength - 1));
                }
                segment.setUtf8String(offset, element);
            }

            @Override
            public Class<?> getNativeArgumentClass() {
                return MemorySegment.class;
            }
        };
    }
}