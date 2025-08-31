package net.aros.natives.implementor.utils;

import net.aros.natives.data.AnNativeValue;
import net.aros.natives.data.MemCodec;
import net.aros.natives.util.AnUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
            method = ofClass.getMethod(javaMethodName, getParamTypesOfCodecs(paramCodecs, returnCodec));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Given method was not found", e);
        }
        validateMethod(method);

        return new MethodData(nativeMethodName, Type.getMethodDescriptor(method), makeHandleDescriptor(returnCodec, paramCodecs), method.getReturnType());
    }

    @SuppressWarnings("preview")
    private static void validateMethod(Method method) {
        if (method.getReturnType().isPrimitive()) {
            if (method.getParameterCount() > 0 && method.getParameterTypes()[0] == MemCodec.class)
                throw new IllegalStateException("❗ Your method is returning primitive, remove MemCodec from parameters");
        } else {
            if (method.getParameterCount() <= 0 || method.getParameterTypes()[0] != MemCodec.class)
                throw new IllegalStateException("❗ Your method is returning non-primitive, add MemCodec for return type as first parameter");
        }
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType().isPrimitive() || parameter.getType() == AnNativeValue.class) continue;
            String anNativeValue = AnNativeValue.class.getName();
            throw new IllegalStateException(STR."❗ Illegal parameter `\{parameter.getType().getName()} \{parameter.getName()}`: can't have non-primitive nor \{anNativeValue} as parameter. Replace with primitive or wrap via \{anNativeValue}");
        }
    }

    private static Class<?>[] getParamTypesOfCodecs(MemCodec<?>[] paramCodecs, MemCodec<?> retCodec) {
        return AnUtils.make(new ArrayList<Class<?>>(), list -> {
            if (retCodec != null && !retCodec.getNativeArgumentClass().isPrimitive()) list.add(MemCodec.class);
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
