package net.aros.natives.implementor.generators;

import net.aros.natives.AnLib;
import net.aros.natives.implementor.utils.AnNamingUtils;
import net.aros.natives.implementor.utils.MethodData;
import org.objectweb.asm.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import java.util.List;

import static net.aros.natives.implementor.utils.AnNamingUtils.getMethodDescriptor;

@SuppressWarnings("preview")
public class InitializerBytecodeGenerator implements Opcodes {
    private final ClassWriter classWriter;
    private final String implInternalName;
    private final List<MethodData> methods;

    private MethodVisitor methodVisitor;
    private Label startLabel;

    InitializerBytecodeGenerator(ClassWriter classWriter, String implInternalName, List<MethodData> methods) {
        this.classWriter = classWriter;
        this.implInternalName = implInternalName;
        this.methods = methods;
    }

    public void addInitializer() {
        startInitMethod(); // public constructor
        for (int i = 0; i < methods.size(); i++) makeHandle(methods.get(i).name(), i); // init handles
        visitReturnThis(); // return this
    }

    private void startInitMethod() {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", ClassImplBytecodeGenerator.DESC_INIT, null, null);
        methodVisitor.visitCode();
        methodVisitor.visitLabel(startLabel = new Label());
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
    }

    private void visitReturnThis() {
        Label endLabel = new Label();
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitLabel(endLabel);
        methodVisitor.visitLocalVariable("this", STR."L\{implInternalName};", null, startLabel, endLabel, 0);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private void makeHandle(String name, int index) {
        methodVisitor.visitVarInsn(ALOAD, 0); // this
        methodVisitor.visitFieldInsn(GETSTATIC, implInternalName, ClassImplBytecodeGenerator.LIB_FIELD_NAME, Type.getDescriptor(AnLib.class));
        methodVisitor.visitLdcInsn(name);
        methodVisitor.visitVarInsn(ALOAD, 1); // array
        pushIntConst(index);                          // array[index]
        methodVisitor.visitInsn(AALOAD);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(AnLib.class), "getHandle", getMethodDescriptor(MethodHandle.class, String.class, FunctionDescriptor.class), false);
        methodVisitor.visitFieldInsn(PUTFIELD, implInternalName, AnNamingUtils.getMethodHandleFieldName(name), Type.getDescriptor(MethodHandle.class));
    }

    private void pushIntConst(int constant) {
        if (constant >= -1 && constant <= 5)
            methodVisitor.visitInsn(ICONST_0 + constant);
        else if (constant >= Byte.MIN_VALUE && constant <= Byte.MAX_VALUE)
            methodVisitor.visitIntInsn(BIPUSH, constant);
        else if (constant >= Short.MIN_VALUE && constant <= Short.MAX_VALUE)
            methodVisitor.visitIntInsn(SIPUSH, constant);
        else
            methodVisitor.visitLdcInsn(constant);
    }
}
