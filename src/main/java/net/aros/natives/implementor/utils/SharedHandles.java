package net.aros.natives.implementor.utils;

import net.aros.natives.util.AnUtils;
import org.jetbrains.annotations.ApiStatus;

import java.lang.foreign.FunctionDescriptor;

@SuppressWarnings("preview")
@ApiStatus.Internal
public class SharedHandles {
    private static FunctionDescriptor[] storedHandles;

    public static void set(FunctionDescriptor[] value) {
        storedHandles = value;
    }

    public static FunctionDescriptor[] get() {
        return AnUtils.make(storedHandles, _ -> storedHandles = null);
    }
}
