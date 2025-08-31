package net.aros.natives.implementor.utils;

import net.aros.natives.util.AnUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

@SuppressWarnings("preview")
public class AnNamingUtils {
    @Contract(pure = true)
    public static @NotNull String getMethodHandleFieldName(@NotNull String methodName) {
        return STR."\{methodName.toUpperCase()}_HANDLE";
    }

    public static String getImplClassName(@NotNull Class<?> clazz) {
        return STR."\{clazz.getName()}_Impl";
    }

    public static String getImplClassFilename(@NotNull Class<?> clazz) {
        return STR."\{clazz.getSimpleName()}_Impl.java";
    }

    public static @NotNull String getMethodDescriptor(Class<?> returnType, Class<?> @NotNull ... paramTypes) {
        return AnUtils.make(new StringBuilder(), builder -> {
            builder.append("(");
            for (Class<?> paramType : paramTypes) builder.append(Type.getDescriptor(paramType));
            builder.append(")").append(Type.getDescriptor(returnType));
        }).toString();
    }
}
