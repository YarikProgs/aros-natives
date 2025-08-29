package net.aros.natives.implementor.utils;

import net.aros.natives.data.MemCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SegmentAllocator;
import java.lang.reflect.Method;
import java.util.Arrays;

@SuppressWarnings("preview")
public record MethodData(String name, String descriptor, FunctionDescriptor handleDescriptor, Class<?> returnType) {
    public static @NotNull MethodData ofCodecs(@NotNull Method method, @Nullable MemCodec<?> returnCodec, MemCodec<?> @NotNull ... paramCodecs) {
        validateMethod(method, returnCodec, paramCodecs);
        return new MethodData(
                method.getName(),
                Type.getMethodDescriptor(method),
                makeDescriptor(returnCodec, paramCodecs),
                method.getReturnType()
        );
    }

    private static void validateMethod(@NotNull Method method, @Nullable MemCodec<?> returnCodec, MemCodec<?> @NotNull [] paramCodecs) {
        // TODO: 28.08.2025
    }

    private static @NotNull FunctionDescriptor makeDescriptor(MemCodec<?> returnCodec, MemCodec<?>[] paramCodecs) {
        MemoryLayout[] params = Arrays.stream(paramCodecs).map(MemCodec::getLayout).toArray(MemoryLayout[]::new);
        return returnCodec == null
                ? FunctionDescriptor.ofVoid(params)
                : FunctionDescriptor.of(returnCodec.getLayout(), params);
    }

    public String getHandleDescriptor(boolean withAllocator) {
        return STR."(\{withAllocator ? Type.getDescriptor(SegmentAllocator.class) : ""}\{handleDescriptor.toMethodType().descriptorString().substring(1)}";
    }
}