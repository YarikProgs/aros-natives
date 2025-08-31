package net.aros.natives.implementor.utils;

import org.objectweb.asm.Type;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.SegmentAllocator;

@SuppressWarnings("preview")
public record MethodData(String name, String descriptor, FunctionDescriptor handleDescriptor, Class<?> returnType) {
    public String getHandleDescriptor(boolean withAllocator) {
        String descriptor = handleDescriptor.toMethodType().descriptorString();
        if (!withAllocator) return descriptor;

        return STR."(\{Type.getDescriptor(SegmentAllocator.class)}\{descriptor.substring(1)}";
    }
}