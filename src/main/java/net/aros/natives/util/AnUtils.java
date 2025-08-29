package net.aros.natives.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


public class AnUtils {
    @Contract("_, _ -> param1")
    public static <T> T make(T instance, @NotNull Consumer<T> maker) {
        maker.accept(instance);
        return instance;
    }
}
