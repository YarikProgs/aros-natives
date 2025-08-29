package net.aros.natives.implementor;

import net.aros.natives.implementor.generators.ClassImplBytecodeGenerator;
import net.aros.natives.implementor.utils.AnNamingUtils;
import net.aros.natives.implementor.utils.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.FunctionDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("preview")
public class AnImplementor implements Opcodes {
    private AnImplementor() {
        throw new UnsupportedOperationException("utility class");
    }

    public static <T> @NotNull T implementAndInitialize(Class<T> interfaceClazz, List<MethodData> methods) {
        Class<? extends T> implementedClass;
        try {
            implementedClass = implement(interfaceClazz, methods);
        } catch (Throwable t) {
            throw new RuntimeException(STR."Failed to implement interface \{interfaceClazz.getName()}", t);
        }
        try {
            return implementedClass.getConstructor(FunctionDescriptor[].class).newInstance((Object) methods.stream().map(MethodData::handleDescriptor).toArray(FunctionDescriptor[]::new));
        } catch (Throwable t) {
            throw new RuntimeException(STR."Failed to instantiate class \{interfaceClazz.getName()}", t);
        }
    }

    public static <T> Class<? extends T> implement(@NotNull Class<T> interfaceClazz, List<MethodData> methods) {
        //noinspection unchecked
        return (Class<? extends T>) new GeneratedClassLoader(interfaceClazz.getClassLoader()).defineClass(
                AnNamingUtils.getImplClassName(interfaceClazz),
                generateBytecode(interfaceClazz, methods)
        );
    }

    public static void implementAndExportToFile(Class<?> interfaceClazz, List<MethodData> methods, Path path) {
        byte[] bytecode = generateBytecode(interfaceClazz, methods);
        try (OutputStream stream = Files.newOutputStream(path)) {
            stream.write(bytecode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> byte[] generateBytecode(@NotNull Class<T> interfaceClazz, List<MethodData> methods) {
        return new ClassImplBytecodeGenerator(interfaceClazz, methods).generateImplementationBytecode();
    }

    private static final class GeneratedClassLoader extends ClassLoader {
        GeneratedClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> defineClass(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
