package net.aros.natives.implementor.utils;

import net.aros.natives.data.MemCodec;
import net.aros.natives.util.AnUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NativeMethodBuilder<T> {
    private final List<MethodData> data = new ArrayList<>();
    private final Class<?> ofClass;

    public NativeMethodBuilder(Class<?> ofClass) {
        this.ofClass = ofClass;
    }

    public NativeMethodBuilder<T> withReturn(String name, MemCodec<?> returnCodec, MemCodec<?>... paramCodecs) {
        return withReturn(name, name, returnCodec, paramCodecs);
    }

    public NativeMethodBuilder<T> withReturn(String javaName, String nativeName, MemCodec<?> returnCodec, MemCodec<?>... paramCodecs) {
        data.add(dataOfMethod(ofClass, javaName, nativeName, returnCodec, paramCodecs));
        return this;
    }

    public NativeMethodBuilder<T> withoutReturn(String name, MemCodec<?>... paramCodecs) {
        return withoutReturn(name, name, paramCodecs);
    }

    public NativeMethodBuilder<T> withoutReturn(String javaName, String nativeName, MemCodec<?>... paramCodecs) {
        return withReturn(javaName, nativeName, null, paramCodecs);
    }

    public List<MethodData> build() {
        return data;
    }

    public static @NotNull MethodData dataOfMethod(@NotNull Class<?> ofClass, @NotNull String javaMethodName, @NotNull String nativeMethodName, @Nullable MemCodec<?> returnCodec, MemCodec<?>... paramCodecs) {
        if (!ofClass.isInterface()) throw new IllegalArgumentException("Class must be interface");

        Method method;
        try {
            method = ofClass.getMethod(javaMethodName, getParamTypesOfCodecs(paramCodecs));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Given method was not found", e);
        }

        return new MethodData(nativeMethodName, Type.getMethodDescriptor(method), makeHandleDescriptor(returnCodec, paramCodecs), method.getReturnType());
    }

    private static Class<?>[] getParamTypesOfCodecs(MemCodec<?>[] paramCodecs) {
        return AnUtils.make(new ArrayList<Class<?>>(), list -> {
            list.add(MemCodec.class);
            list.addAll(Arrays.stream(paramCodecs).map(MemCodec::getNativeArgumentClass).toList());
        }).toArray(Class<?>[]::new);
    }

    @SuppressWarnings("preview")
    private static @NotNull FunctionDescriptor makeHandleDescriptor(MemCodec<?> returnCodec, MemCodec<?>[] paramCodecs) {
        MemoryLayout[] params = Arrays.stream(paramCodecs).map(MemCodec::getLayout).toArray(MemoryLayout[]::new);
        return returnCodec == null
                ? FunctionDescriptor.ofVoid(params)
                : FunctionDescriptor.of(returnCodec.getLayout(), params);
    }
}
