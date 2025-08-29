package net.aros.natives.implementor.generators;

import net.aros.natives.AnLib;
import net.aros.natives.implementor.utils.AnNamingUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class StaticInitializerBytecodeGenerator implements Opcodes {
    private final ClassWriter classWriter;
    private final String internalName;
    private final String libName;

    StaticInitializerBytecodeGenerator(ClassWriter classWriter, String libName, String internalName) {
        this.classWriter = classWriter;
        this.libName = libName;
        this.internalName = internalName;
    }

    public void addStaticInitializer() {
        MethodVisitor staticMethodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        staticMethodVisitor.visitCode();

        staticMethodVisitor.visitTypeInsn(NEW, Type.getInternalName(AnLib.class));
        staticMethodVisitor.visitInsn(DUP);
        staticMethodVisitor.visitLdcInsn(libName);
        staticMethodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(AnLib.class), "<init>", AnNamingUtils.getMethodDescriptor(void.class, String.class), false);
        staticMethodVisitor.visitFieldInsn(PUTSTATIC, internalName, ClassImplBytecodeGenerator.LIB_FIELD_NAME, Type.getDescriptor(AnLib.class));

        staticMethodVisitor.visitInsn(RETURN);
        staticMethodVisitor.visitMaxs(0, 0);
        staticMethodVisitor.visitEnd();
    }
}
