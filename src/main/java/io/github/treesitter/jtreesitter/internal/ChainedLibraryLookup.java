package io.github.treesitter.jtreesitter.internal;

import io.github.treesitter.jtreesitter.NativeLibraryLookup;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;

@SuppressWarnings("unused")
final class ChainedLibraryLookup implements NativeLibraryLookup {
    private ChainedLibraryLookup() {}

    static ChainedLibraryLookup INSTANCE = new ChainedLibraryLookup();

    @Override
    public SymbolLookup get(Arena arena) {
        var serviceLoader = ServiceLoader.load(NativeLibraryLookup.class);
        // NOTE: can't use _ because of palantir/palantir-java-format#934
        SymbolLookup lookup = (name) -> Optional.empty();
        for (var libraryLookup : serviceLoader) {
            lookup = lookup.or(libraryLookup.get(arena));
        }

        return lookup.or((name) -> findLibrary(arena).find(name)).or(Linker.nativeLinker().defaultLookup());
    }

    private static SymbolLookup findLibrary(Arena arena) {
        try {
            var library = System.mapLibraryName("tree-sitter");
            return SymbolLookup.libraryLookup(library, arena);
        } catch (IllegalArgumentException ex1) {
            try {
                findLibraryBundledInJar("tree-sitter");
            } catch (UnsatisfiedLinkError ex2) {
                try {
                    System.loadLibrary("tree-sitter");
                } catch (UnsatisfiedLinkError ex3) {
                    ex2.addSuppressed(ex3);
                    ex1.addSuppressed(ex2);
                    throw ex1;
                }
            }
            return SymbolLookup.loaderLookup();
        }
    }

    private static void findLibraryBundledInJar(String libBaseName) throws UnsatisfiedLinkError {
        /*
         * Strategy:
         * 1) Resolve os & arch and compute candidate resource names
         * 2) Try to locate resource inside JAR with several common layouts
         * 3) Extract to temp file and System.load it
         */

        final String mappedName = System.mapLibraryName(libBaseName); // platform-native file name (libtree-sitter.so, tree-sitter.dll, ...)
        final String os = detectOs();
        final String arch = detectArch();
        final String ext = extractExtension(mappedName); // ".so" or ".dll" or ".dylib"

        // Candidate resource paths inside the JAR. Adapt these to however you pack native libs.
        String[] candidates = new String[] {
            // platform-specific directories (most specific)
            "/natives/" + os + "-" + arch + "/" + mappedName,
            "/natives/" + arch + "/" + mappedName,
            "/native/" + os + "-" + arch + "/" + mappedName,
            "/native/" + arch + "/" + mappedName,
            // less specific
            "/natives/" + mappedName,
            "/native/" + mappedName,
            // fallback: just the file at root of jar (not recommended but sometimes used)
            "/" + mappedName
        };

        InputStream foundStream = null;
        String foundResource = null;
        for (String candidate : candidates) {
            InputStream is = ChainedLibraryLookup.class.getResourceAsStream(candidate);
            if (is != null) {
                foundStream = is;
                foundResource = candidate;
                break;
            }
        }

        if (foundStream == null) {
            // helpful message mentioning what we tried
            String tried = String.join(", ", candidates);
            throw new UnsatisfiedLinkError("Could not find bundled native library resource for '"
                + libBaseName + "'. Tried: " + tried);
        }

        // Create temp file and copy resource contents
        Path temp = null;
        try (InputStream in = foundStream) {
            String suffix = ext != null ? ext : null; // Files.createTempFile needs suffix with dot
            // create a predictable prefix but allow uniqueness
            String prefix = "jtreesitter-" + libBaseName + "-";
            if (suffix == null) {
                // fallback if we couldn't detect extension
                temp = Files.createTempFile(prefix, null);
            } else {
                temp = Files.createTempFile(prefix, suffix);
            }
            // Ensure cleanup on exit as best-effort
            temp.toFile().deleteOnExit();

            // Copy bytes
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);

            // On unix-like systems make executable (not strictly necessary for shared objects, but safe)
            try {
                temp.toFile().setExecutable(true, true);
            } catch (Exception ignored) {
            }

            // Load the native library from the extracted temp file
            System.load(temp.toAbsolutePath().toString());
        } catch (IOException e) {
            // wrap as UnsatisfiedLinkError to match calling code expectations
            UnsatisfiedLinkError ule = new UnsatisfiedLinkError("Failed to extract and load native library from JAR (resource: " + foundResource + "): " + e);
            ule.initCause(e);
            throw ule;
        }
    }

    private static String detectOs() {
        String osProp = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (osProp.contains("win")) return "windows";
        if (osProp.contains("mac") || osProp.contains("darwin") || osProp.contains("os x")) return "macos";
        if (osProp.contains("nux") || osProp.contains("nix") || osProp.contains("linux")) return "linux";
        // fallback
        return osProp.replaceAll("\\s+", "");
    }

    private static String detectArch() {
        String archProp = System.getProperty("os.arch", "").toLowerCase(Locale.ENGLISH);
        if (archProp.equals("x86_64") || archProp.equals("amd64")) return "x86_64";
        if (archProp.equals("aarch64") || archProp.equals("arm64")) return "aarch64";
        // other architectures we return raw (but normalized)
        return archProp.replaceAll("\\s+", "");
    }

    private static String extractExtension(String mappedName) {
        if (mappedName == null) return null;
        int idx = mappedName.lastIndexOf('.');
        if (idx == -1) return null;
        return mappedName.substring(idx); // includes dot, e.g. ".so"
    }
}
