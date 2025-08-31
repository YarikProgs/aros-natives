package net.aros.natives.implementor.generators;

import net.aros.natives.AnLib;
import net.aros.natives.implementor.utils.AnNamingUtils;
import net.aros.natives.implementor.utils.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.List;

public class FieldsBytecodeGenerator implements Opcodes {
    private final ClassWriter cw;
    private final Collection<MethodData> methods;

    FieldsBytecodeGenerator(ClassWriter cw, List<MethodData> methods) {
        this.cw = cw;
        this.methods = methods;
    }

    public void addHandleFields() {
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, ClassImplBytecodeGenerator.LIB_FIELD_NAME, Type.getDescriptor(AnLib.class), null, null).visitEnd();
        methods.forEach(this::addHandleField);
    }

    private void addHandleField(@NotNull MethodData method) {
        cw.visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                AnNamingUtils.getMethodHandleFieldName(method.name()),
                Type.getDescriptor(MethodHandle.class), null, null
        ).visitEnd();
    }
}
