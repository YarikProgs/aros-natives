package net.aros.natives;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@SuppressWarnings({"preview", "StringTemplateMigration"})
public class AnLib {
    private final Linker linker;
    private final Arena arena;
    private final SymbolLookup lookup;
    private boolean available;

    public AnLib(String libraryDir) {
        String path = loadLibFromResources(libraryDir);

        this.linker = Linker.nativeLinker();
        this.arena = Arena.ofConfined();
        this.lookup = SymbolLookup.libraryLookup(path, arena);
        this.available = true;
    }

    @SuppressWarnings("unused") // in bytecode
    public MethodHandle getHandle(@NotNull String name, @NotNull FunctionDescriptor desc) {
        if (!available) throw new IllegalStateException("Library is not available");

        MethodHandle handle = linker.downcallHandle(lookup.find(name).orElseThrow(), desc);
        if (handle == null) throw new NoSuchMethodError("Couldn't downcall handle " + name);

        return handle;
    }

    public void free() {
        arena.close();
        available = false;
    }

    private static @NotNull String loadLibFromResources(String dir) {
        try {
            String libName = System.mapLibraryName("lib");
            InputStream in = AnLib.class.getResourceAsStream("/cLibs/" + dir + "/" + libName);
            if (in == null) throw new IOException();

            File tempFile = Files.createTempFile(dir, libName).toFile();
            Files.copy(in, tempFile.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.load(tempFile.getAbsolutePath());
            tempFile.deleteOnExit();
            System.out.printf("Library %s/%s loaded!\n", dir, libName);
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load library", e);
        }
    }
}
