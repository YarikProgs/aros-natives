package net.aros.natives.implementor.generators;

import net.aros.natives.AnLib;
import net.aros.natives.implementor.utils.AnNamingUtils;
import net.aros.natives.implementor.utils.MethodData;
import net.aros.natives.implementor.utils.SharedHandles;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import java.util.List;

import static net.aros.natives.implementor.utils.AnNamingUtils.getMethodDescriptor;

@SuppressWarnings("preview")
public class StaticInitializerBytecodeGenerator implements Opcodes {
    private static final String AN_LIB_INTERNAL_NAME = Type.getInternalName(AnLib.class);
    private static final String AN_LIB_INIT_DESC = AnNamingUtils.getMethodDescriptor(void.class, String.class);
    private static final String AN_LIB_DESC = Type.getDescriptor(AnLib.class);
    private static final String GET_DESC = getMethodDescriptor(FunctionDescriptor[].class);

    private final ClassWriter classWriter;
    private final String implInternalName;
    private final String libName;
    private final List<MethodData> methods;

    private MethodVisitor staticMethodVisitor;

    StaticInitializerBytecodeGenerator(ClassWriter classWriter, String libName, String implInternalName, List<MethodData> methods) {
        this.classWriter = classWriter;
        this.libName = libName;
        this.implInternalName = implInternalName;
        this.methods = methods;
    }

    public void addStaticInitializer() {
        startInit();

        addLib();
        getHandles();
        for (int i = 0; i < methods.size(); i++) makeHandle(methods.get(i).name(), i);

        endInit();
    }

    private void getHandles() {
        staticMethodVisitor.visitMethodInsn(INVOKESTATIC,
                Type.getInternalName(SharedHandles.class),
                "get",
                GET_DESC,
                false
        );
        staticMethodVisitor.visitVarInsn(ASTORE, 0);
    }

    private void makeHandle(String name, int index) {
        staticMethodVisitor.visitFieldInsn(GETSTATIC, implInternalName, ClassImplBytecodeGenerator.LIB_FIELD_NAME, Type.getDescriptor(AnLib.class));
        staticMethodVisitor.visitLdcInsn(name);
        staticMethodVisitor.visitVarInsn(ALOAD, 0);
        pushIntConst(index);
        staticMethodVisitor.visitInsn(AALOAD);
        staticMethodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(AnLib.class), "getHandle", getMethodDescriptor(MethodHandle.class, String.class, FunctionDescriptor.class), false);
        staticMethodVisitor.visitFieldInsn(PUTSTATIC, implInternalName, AnNamingUtils.getMethodHandleFieldName(name), Type.getDescriptor(MethodHandle.class));
    }

    private void pushIntConst(int constant) {
        if (constant >= -1 && constant <= 5)
            staticMethodVisitor.visitInsn(ICONST_0 + constant);
        else if (constant >= Byte.MIN_VALUE && constant <= Byte.MAX_VALUE)
            staticMethodVisitor.visitIntInsn(BIPUSH, constant);
        else if (constant >= Short.MIN_VALUE && constant <= Short.MAX_VALUE)
            staticMethodVisitor.visitIntInsn(SIPUSH, constant);
        else
            staticMethodVisitor.visitLdcInsn(constant);
    }

    private void addLib() {
        staticMethodVisitor.visitTypeInsn(NEW, AN_LIB_INTERNAL_NAME);
        staticMethodVisitor.visitInsn(DUP);
        staticMethodVisitor.visitLdcInsn(libName);
        staticMethodVisitor.visitMethodInsn(INVOKESPECIAL, AN_LIB_INTERNAL_NAME, "<init>", AN_LIB_INIT_DESC, false);
        staticMethodVisitor.visitFieldInsn(PUTSTATIC, implInternalName, ClassImplBytecodeGenerator.LIB_FIELD_NAME, AN_LIB_DESC);
    }

    private void startInit() {
        staticMethodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        staticMethodVisitor.visitCode();
    }

    private void endInit() {
        staticMethodVisitor.visitInsn(RETURN);
        staticMethodVisitor.visitMaxs(0, 0);
        staticMethodVisitor.visitEnd();
    }
}
