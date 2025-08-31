package net.aros.natives.implementor.generators;

import net.aros.natives.implementor.utils.AnNamingUtils;
import net.aros.natives.implementor.utils.InternalMemoryUtils;
import net.aros.natives.implementor.utils.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("preview")
public class MethodsBytecodeGenerator implements Opcodes {
    private static final String TO_MEMORY_METHOD_NAME = "toMemoryUnsafe";
    private static final String FROM_MEMORY_METHOD_NAME = "fromMemoryUnsafe";
    private static final String RESULT_FROM_MEMORY_METHOD_NAME = "resultFromMemUnsafe";
    private static final String TO_MEMORY_METHOD_DESC = AnNamingUtils.getMethodDescriptor(Object.class, Arena.class, Object.class);
    private static final String FROM_MEMORY_METHOD_DESC = AnNamingUtils.getMethodDescriptor(void.class, Object.class, Object.class);
    private static final String RESULT_FROM_MEMORY_METHOD_DESC = AnNamingUtils.getMethodDescriptor(Object.class, Object.class, Object.class);

    private static final Type ARENA_TYPE = Type.getType(Arena.class);
    private static final Type INTERNAL_MEM_UTILS_TYPE = Type.getType(InternalMemoryUtils.class);
    private static final Type METHOD_HANDLE_TYPE = Type.getType(MethodHandle.class);
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type RUNTIME_EXCEPTION_TYPE = Type.getType(RuntimeException.class);
    private static final Type SEGMENT_ALLOCATOR_TYPE = Type.getType(SegmentAllocator.class);

    private final ClassWriter classWriter;
    private final String implementationInternalName;
    private final Class<?> interfaceClass;
    private final List<MethodData> methods;

    private GeneratorAdapter generatorAdapter;
    private List<Type> parameterCarrierTypes;
    private boolean hasReturn;
    private Type returnCarrierType;
    private Type methodReturnType;
    private Label tryStart;
    private int arenaLocal;
    private int[] memoryLocals;
    private int resultLocal;
    private int returnLocal;
    private int handleParamCount;


    MethodsBytecodeGenerator(@NotNull ClassWriter classWriter, String implementationInternalName, Class<?> interfaceClass, List<MethodData> methods) {
        this.classWriter = classWriter;
        this.implementationInternalName = implementationInternalName;
        this.interfaceClass = interfaceClass;
        this.methods = methods;

        classWriter.visit(V21, ACC_PUBLIC | ACC_SUPER, implementationInternalName, null, Type.getInternalName(Object.class), new String[]{Type.getInternalName(interfaceClass)});
    }

    public void addMethodsImplementation() {
        for (MethodData method : methods) {
            startMethod(method);
            generateMethodBody(method);
            generatorAdapter.endMethod();
        }
        classWriter.visitEnd();
    }

    private void startMethod(@NotNull MethodData method) {
        String methodDescriptor = method.descriptor();
        generatorAdapter = new GeneratorAdapter(
                ACC_PUBLIC,
                new Method(method.name(), methodDescriptor),
                null,
                new Type[]{Type.getType(Throwable.class)},
                classWriter
        );
        generatorAdapter.visitCode();
    }

    private void generateMethodBody(@NotNull MethodData method) {
        methodReturnType = Type.getReturnType(method.descriptor());
        prepareTypesAndLocals(method);
        createArenaAndStore();
        tryStart = generatorAdapter.mark();
//        loadAndStoreMethodHandle(method);
        convertParametersToMemory();
        invokeMethodHandle(method);
        revertMemoryToParameters();
        processResultIfPresent();
        Label tryEnd = generatorAdapter.mark();
        closeArena();
        emitReturn();
        handleExceptionBlock(method, tryEnd);
    }

    private void prepareTypesAndLocals(@NotNull MethodData method) {
        Type[] argumentTypes = Type.getArgumentTypes(method.descriptor());
        handleParamCount = argumentTypes.length - 1;

        MethodType handleMethodType = method.handleDescriptor().toMethodType();
        parameterCarrierTypes = new ArrayList<>(handleParamCount);
        for (int i = 0; i < handleParamCount; i++) {
            parameterCarrierTypes.add(getMappedTypeFor(handleMethodType.parameterType(i)));
        }
        hasReturn = method.handleDescriptor().returnLayout().isPresent();
        returnCarrierType = hasReturn ? getMappedTypeFor(handleMethodType.returnType()) : Type.VOID_TYPE;

        arenaLocal = generatorAdapter.newLocal(ARENA_TYPE);
        memoryLocals = new int[handleParamCount];
        for (int i = 0; i < handleParamCount; i++) {
            memoryLocals[i] = generatorAdapter.newLocal(parameterCarrierTypes.get(i));
        }
        resultLocal = hasReturn ? generatorAdapter.newLocal(returnCarrierType) : -1;
        returnLocal = hasReturn ? generatorAdapter.newLocal(methodReturnType) : -1;
    }

    private void createArenaAndStore() {
        generatorAdapter.visitMethodInsn(INVOKESTATIC, ARENA_TYPE.getInternalName(), "ofConfined", AnNamingUtils.getMethodDescriptor(Arena.class), true);
        generatorAdapter.storeLocal(arenaLocal);
    }

    private void convertParametersToMemory() {
        for (int i = 0; i < handleParamCount; i++) {
            int argIndex = i + 1;
            generatorAdapter.loadLocal(arenaLocal);
            generatorAdapter.loadArg(argIndex);
            Type carrierType = parameterCarrierTypes.get(i);
            if (carrierType.getSort() != Type.OBJECT && carrierType.getSort() != Type.ARRAY) {
                generatorAdapter.box(carrierType);
            }
            generatorAdapter.invokeStatic(INTERNAL_MEM_UTILS_TYPE, new Method(TO_MEMORY_METHOD_NAME, TO_MEMORY_METHOD_DESC));
            if (carrierType.getSort() == Type.OBJECT || carrierType.getSort() == Type.ARRAY) {
                generatorAdapter.checkCast(carrierType);
            } else {
                generatorAdapter.unbox(carrierType);
            }
            generatorAdapter.storeLocal(memoryLocals[i]);
        }
    }

    private void invokeMethodHandle(MethodData method) {
        generatorAdapter.loadThis();
        generatorAdapter.getStatic(Type.getObjectType(implementationInternalName), AnNamingUtils.getMethodHandleFieldName(method.name()), METHOD_HANDLE_TYPE);

        boolean withAllocator = isAllocatorNeeded(method);
        if (withAllocator) {
            generatorAdapter.loadLocal(arenaLocal);
            generatorAdapter.checkCast(SEGMENT_ALLOCATOR_TYPE);
        }

        for (int i = 0; i < handleParamCount; i++) {
            generatorAdapter.loadLocal(memoryLocals[i]);
        }

        generatorAdapter.invokeVirtual(METHOD_HANDLE_TYPE, new Method("invokeExact",
                method.getHandleDescriptor(withAllocator)
        ));
        if (hasReturn) {
            generatorAdapter.storeLocal(resultLocal);
        }
    }

    private boolean isAllocatorNeeded(@NotNull MethodData method) {
        Class<?> clazz = method.returnType();
        return !clazz.isPrimitive() && clazz != MemorySegment.class;
    }

    private void revertMemoryToParameters() {
        for (int i = 0; i < handleParamCount; i++) {
            int argIndex = i + 1;
            Type carrierType = parameterCarrierTypes.get(i);
            generatorAdapter.loadArg(argIndex);
            if (carrierType.getSort() != Type.OBJECT && carrierType.getSort() != Type.ARRAY) {
                generatorAdapter.box(carrierType);
            }
            generatorAdapter.loadLocal(memoryLocals[i]);
            if (carrierType.getSort() != Type.OBJECT && carrierType.getSort() != Type.ARRAY) {
                generatorAdapter.box(carrierType);
            }
            generatorAdapter.invokeStatic(INTERNAL_MEM_UTILS_TYPE, new Method(FROM_MEMORY_METHOD_NAME, FROM_MEMORY_METHOD_DESC));
        }
    }

    private void processResultIfPresent() {
        if (!hasReturn) {
            return;
        }
        generatorAdapter.loadLocal(resultLocal);
        if (returnCarrierType.getSort() != Type.OBJECT && returnCarrierType.getSort() != Type.ARRAY) {
            generatorAdapter.box(returnCarrierType);
        }
        generatorAdapter.loadArg(0);
        generatorAdapter.invokeStatic(INTERNAL_MEM_UTILS_TYPE, new Method(RESULT_FROM_MEMORY_METHOD_NAME, RESULT_FROM_MEMORY_METHOD_DESC));
        handleReturnValueWithoutReturn(methodReturnType);
        generatorAdapter.storeLocal(returnLocal);
    }

    private void emitReturn() {
        if (hasReturn) {
            generatorAdapter.loadLocal(returnLocal);
        }
        generatorAdapter.returnValue();
    }

    private void closeArena() {
        generatorAdapter.loadLocal(arenaLocal);
        generatorAdapter.invokeInterface(ARENA_TYPE, new Method("close", "()V"));
    }

    private void handleReturnValueWithoutReturn(Type returnType) {
        if (returnType == Type.VOID_TYPE) {
            generatorAdapter.pop();
            return;
        }

        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            generatorAdapter.checkCast(returnType);
            return;
        }

        switch (returnType.getSort()) {
            case Type.BOOLEAN -> unboxPrimitiveWithoutReturn(Boolean.class, "booleanValue", "()Z");
            case Type.BYTE -> unboxPrimitiveWithoutReturn(Byte.class, "byteValue", "()B");
            case Type.CHAR -> unboxPrimitiveWithoutReturn(Character.class, "charValue", "()C");
            case Type.SHORT -> unboxPrimitiveWithoutReturn(Short.class, "shortValue", "()S");
            case Type.INT -> unboxPrimitiveWithoutReturn(Integer.class, "intValue", "()I");
            case Type.LONG -> unboxPrimitiveWithoutReturn(Long.class, "longValue", "()J");
            case Type.FLOAT -> unboxPrimitiveWithoutReturn(Float.class, "floatValue", "()F");
            case Type.DOUBLE -> unboxPrimitiveWithoutReturn(Double.class, "doubleValue", "()D");
            default -> generatorAdapter.checkCast(Type.getType(Object.class));
        }
    }

    private void unboxPrimitiveWithoutReturn(Class<?> wrapperClass, String methodName, String descriptor) {
        generatorAdapter.checkCast(Type.getType(wrapperClass));
        generatorAdapter.invokeVirtual(Type.getType(wrapperClass), new Method(methodName, descriptor));
    }

    private void handleExceptionBlock(@NotNull MethodData method, Label tryEnd) {
        Label catchStart = new Label();
        generatorAdapter.visitTryCatchBlock(tryStart, tryEnd, catchStart, Type.getInternalName(Throwable.class));
        generatorAdapter.mark(catchStart);
        int throwableLocal = generatorAdapter.newLocal(THROWABLE_TYPE);
        generatorAdapter.storeLocal(throwableLocal);

        Label innerTryStart = new Label();
        Label innerTryEnd = new Label();
        Label innerHandler = new Label();
        Label afterInner = new Label();
        generatorAdapter.visitTryCatchBlock(innerTryStart, innerTryEnd, innerHandler, Type.getInternalName(Throwable.class));

        generatorAdapter.mark(innerTryStart);
        closeArena();
        generatorAdapter.mark(innerTryEnd);
        generatorAdapter.goTo(afterInner);

        generatorAdapter.mark(innerHandler);
        int suppressedLocal = generatorAdapter.newLocal(THROWABLE_TYPE);
        generatorAdapter.storeLocal(suppressedLocal);
        generatorAdapter.loadLocal(throwableLocal);
        generatorAdapter.loadLocal(suppressedLocal);
        generatorAdapter.invokeVirtual(THROWABLE_TYPE, new Method("addSuppressed", AnNamingUtils.getMethodDescriptor(void.class, Throwable.class)));
        generatorAdapter.mark(afterInner);

        generatorAdapter.newInstance(RUNTIME_EXCEPTION_TYPE);
        generatorAdapter.dup();
        generatorAdapter.push(STR."Failed to execute \{method.name()} from \{interfaceClass.getName()}");
        generatorAdapter.loadLocal(throwableLocal);
        generatorAdapter.invokeConstructor(RUNTIME_EXCEPTION_TYPE, new Method("<init>", AnNamingUtils.getMethodDescriptor(void.class, String.class, Throwable.class)));
        generatorAdapter.throwException();
    }

    private static Type getMappedTypeFor(@NotNull Class<?> clazz) {
        return clazz.isPrimitive() ? Type.getType(clazz) : Type.getType(MemorySegment.class);
    }
}