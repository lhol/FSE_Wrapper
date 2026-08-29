package io.github.lhol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Extracts a platform-native shared library from the classpath (bundled inside a JAR)
 * to a temporary directory and loads it via {@link System#load(String)}.
 *
 * <p>Library lookup order:
 * <ol>
 *   <li>{@code /natives/{rid}/{libname}} — multi-platform distribution JAR</li>
 *   <li>{@code /natives/local/{libname}} — local developer build JAR</li>
 *   <li>{@link System#loadLibrary(String)} — falls back to {@code java.library.path}</li>
 * </ol>
 *
 * <p>Where {@code {rid}} is the runtime identifier for the current platform, e.g.
 * {@code linux-x64}, {@code linux-arm64}, {@code osx-arm64}, {@code win-x64},
 * {@code win-arm64}.
 *
 * <p>Thread safety: {@link #load(String)} is {@code synchronized} and idempotent.
 */
public final class NativeLoader {

    private static final Path TEMP_DIR;

    static {
        try {
            TEMP_DIR = Files.createTempDirectory("fse-wrapper-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { deleteRecursive(TEMP_DIR); } catch (IOException ignored) {}
        }));
    }

    private NativeLoader() {}

    /**
     * Loads the named native library, extracting it from the JAR if present.
     *
     * @param name bare library name without OS prefix/suffix, e.g. {@code "huff0"}
     * @throws UnsatisfiedLinkError if the library cannot be found or loaded
     */
    public static synchronized void load(String name) {
        String libFileName = mapLibName(name);
        String rid = detectRid();

        for (String resourceDir : new String[]{"natives/" + rid, "natives/local"}) {
            String resourcePath = "/" + resourceDir + "/" + libFileName;
            try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    continue;
                }
                // Prefix with RID so parallel class loaders don't collide
                Path target = TEMP_DIR.resolve(rid + "_" + libFileName);
                if (!Files.exists(target)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                System.load(target.toAbsolutePath().toString());
                return;
            } catch (IOException e) {
                throw new UnsatisfiedLinkError(
                        "Failed to extract native library from " + resourcePath + ": " + e.getMessage());
            }
        }

        // No bundled library found — fall back to java.library.path (e.g. IDE / surefire)
        System.loadLibrary(name);
    }

    /**
     * Maps a bare library name to the OS-specific filename.
     * E.g. {@code "huff0"} → {@code "huff0.dll"} on Windows,
     * {@code "libhuff0.so"} on Linux, {@code "libhuff0.dylib"} on macOS.
     */
    static String mapLibName(String name) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))  return name + ".dll";
        if (os.contains("mac"))  return "lib" + name + ".dylib";
        return "lib" + name + ".so";
    }

    /**
     * Detects the current platform as a runtime identifier string compatible with
     * the JAR's {@code natives/} directory layout and the NuGet RID convention.
     *
     * @return one of: {@code linux-x64}, {@code linux-arm64}, {@code osx-arm64},
     *         {@code win-x64}, {@code win-arm64}
     * @throws UnsatisfiedLinkError if the platform is not supported
     */
    static String detectRid() {
        String os   = System.getProperty("os.name",   "").toLowerCase();
        String arch = System.getProperty("os.arch",   "").toLowerCase();
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("linux")) return arm ? "linux-arm64" : "linux-x64";
        if (os.contains("mac"))   return "osx-arm64";
        if (os.contains("win"))   return arm ? "win-arm64"   : "win-x64";
        throw new UnsatisfiedLinkError("Unsupported platform: os.name=" + os + ", os.arch=" + arch);
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
    }
}
