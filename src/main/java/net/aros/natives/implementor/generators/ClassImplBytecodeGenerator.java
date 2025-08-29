package net.aros.natives.implementor.generators;

import net.aros.natives.implementor.utils.AnNamingUtils;
import net.aros.natives.implementor.utils.MethodData;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.foreign.FunctionDescriptor;
import java.util.List;
import java.util.Objects;

import static net.aros.natives.implementor.utils.AnNamingUtils.getMethodDescriptor;

@SuppressWarnings("preview")
public class ClassImplBytecodeGenerator implements Opcodes {
    public static final String LIB_FIELD_NAME = "LIB";
    public static final String DESC_INIT = getMethodDescriptor(void.class, FunctionDescriptor[].class);

    private final Class<?> interfaceClass;
    private final List<MethodData> methods;
    private final ClassWriter classWriter;

    public ClassImplBytecodeGenerator(Class<?> interfaceClass, List<MethodData> methods) {
        validate(interfaceClass, methods);
        this.interfaceClass = interfaceClass;
        this.methods = methods;
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }

    public byte[] generateImplementationBytecode() {
        String internalName = AnNamingUtils.getImplClassName(interfaceClass).replace('.', '/');
        beginWriting(internalName);
        writeContents(internalName);
        return endWriting();
    }

    private void beginWriting(String internalName) {
        classWriter.visit(V21, ACC_PUBLIC | ACC_SUPER, internalName, null, Type.getInternalName(Object.class), new String[]{Type.getInternalName(interfaceClass)});
        classWriter.visitSource(AnNamingUtils.getImplClassFilename(interfaceClass), null);
    }

    private void writeContents(String internalName) {
        List.<Runnable>of(
                new FieldsBytecodeGenerator(classWriter, methods)::addHandleFields,
                new StaticInitializerBytecodeGenerator(classWriter, interfaceClass.getName().replace('.', '_'), internalName)::addStaticInitializer,
                new InitializerBytecodeGenerator(classWriter, internalName, methods)::addInitializer,
                new MethodsBytecodeGenerator(classWriter, internalName, interfaceClass, methods)::addMethodsImplementation
        ).forEach(Runnable::run);
    }

    private byte[] endWriting() {
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void validate(Class<?> interfaceClass, List<MethodData> methods) {
        Objects.requireNonNull(interfaceClass, "interfaceClass");
        Objects.requireNonNull(methods, "methods");
        if (!interfaceClass.isInterface()) throw new IllegalArgumentException("interfaceClass must be an interface");
    }
}
