package info.kgeorgiy.ja.chulkov.implementor;


import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Implementation of {@link JarImpler}, that generates implementations of interfaces and classes with default behavior.
 */
public class Implementor implements JarImpler {


    /**
     * Suffix for Java files
     */
    private static final String JAVA = ".java";

    /**
     * Suffix for class files
     */
    private static final String CLASS = ".class";
    /**
     * Suffix for implemented classes and interfaces
     */
    private static final String IMPL = "Impl";
    /**
     * Default path separator in jar format
     */
    private static final String JAR_PATH_SEPARATOR = "/";
    /**
     * Map with reasons to reject implementation and messages about it.
     */
    private static final Map<Predicate<Class<?>>, String> EXCEPTIONS_REASONS = Map.of(
            Class::isPrimitive, "Token mustn't be primitive",
            Class::isArray, "Token mustn't be primitive",
            Class::isRecord, "Can't extend records",
            Class::isSealed, "Can't extend sealed class",
            it -> it.isAssignableFrom(Enum.class), "Token mustn't be enum",
            it -> Modifier.isPrivate(it.getModifiers()), "Can't implement private token",
            it -> !it.isInterface() && Modifier.isFinal(it.getModifiers()), "Superclass must be not final",
            it -> !it.isInterface() && ImplClassStructure.getNonPrivateConstructorsStream(it).findAny()
                    .isEmpty(), "Superclass must has not private constructor");


    /**
     * Simple file visitor to recursive delete all files in directory. Good way to use it in method
     * {@link Files#walkFileTree(Path, FileVisitor)}
     */
    private static final SimpleFileVisitor<Path> DELETE = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };


    /**
     * Method that process cli users requests. From Java prefer to use {@link Implementor#implement(Class, Path)} or
     * {@link Implementor#implementJar(Class, Path)}
     *
     * @param args array with users cli arguments
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args);
        if (args.length != 1 && args.length != 3) {
            printUsages();
        }
        for (final String arg : args) {
            Objects.requireNonNull(arg);
        }
        try {
            final Class<?> token = Class.forName(args.length == 1 ? args[0] : args[1]);
            try {
                if (args.length == 3) {
                    if (!Objects.equals(args[0], "-jar")) {
                        printUsages();
                        return;
                    }
                    new Implementor().implementJar(token, Path.of(args[2]));
                } else {
                    new Implementor().implement(token, Path.of(""));
                }
            } catch (final ImplerException e) {
                System.err.println("Error: " + e.getMessage());
            }
        } catch (final ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " not found");
        }
    }


    /**
     * Method prints usages of {@link Implementor#main(String[])} for users in stdout
     */
    private static void printUsages() {
        System.out.println("""
                Variants of command line arguments:
                [java class file]
                -jar [java class file] [jar file]
                """);
    }


    /**
     * Generate type name for implementation
     *
     * @param token type for implementation
     * @return generated type name
     */
    private static String getTypeName(final Class<?> token) {
        return token.getSimpleName() + IMPL;
    }

    /**
     * Generate file name for implementation specified by the extension For generating type name uses
     * {@link Implementor#getTypeName(Class)}
     *
     * @param token     type for implementation
     * @param extension file extension
     * @return generated file name
     */
    private static String getFileName(final Class<?> token, final String extension) {
        return getTypeName(token) + extension;
    }

    /**
     * Generates classpath for given {@code token}. To generate uses location of {@code token} code source.
     * If code source location is unavailable returns empty string.
     * @param token Class for finding classpath of it
     * @return classpath or empty string if classpath is unavailable
     */
    private String getClassPath(final Class<?> token) {
        try {
            if (token.getProtectionDomain() == null || token.getProtectionDomain().getCodeSource() == null) {
                return "";
            }
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            return "";
        }
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        implementAndReturnFilePath(token, root);
    }

    /**
     * Similar to {@link Implementor#implement}, but returns path to implemented java file.
     */
    private Path implementAndReturnFilePath(final Class<?> token, final Path root) throws ImplerException {
        Objects.requireNonNull(token);
        Objects.requireNonNull(root);
        for (final var reasonMessageEntry : EXCEPTIONS_REASONS.entrySet()) {
            if (reasonMessageEntry.getKey().test(token)) {
                throw new ImplerException(reasonMessageEntry.getValue());
            }
        }
        final Path implemetationPath = root.resolve(token.getPackageName().replace(".", File.separator));
        try {
            Files.createDirectories(implemetationPath);
        } catch (final IOException e) {
            System.err.println("Can't create directories");
        }
        final String typeName = getTypeName(token);
        final Path filePath = implemetationPath.resolve(getFileName(token, JAVA));
        try (final BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(generatePackage(token));
            final ImplInterfaceStructure implementedClassStructure =
                    token.isInterface() ? new ImplInterfaceStructure(token, typeName)
                            : new ImplClassStructure(token, typeName);
            writer.write(implementedClassStructure.toString());
        } catch (final IOException e) {
            throw new ImplerException("Error on writing in file", e);
        }
        return filePath;
    }

    /**
     * Generate package line about {@code token}.
     * If package isn't present, returns empty string.
     * If {@code token} has package {@code my.best.package}, returns package line with two line separators after
     * <pre>
     *     {@code package my.best.package;}
     *
     *
     * </pre>
     * @param token type to getting package
     * @return package lines string if package is present, or empty string otherwise
     */
    private String generatePackage(final Class<?> token) {
        if (!token.getPackageName().isEmpty()) {
            return "package " + token.getPackageName() + ";" + System.lineSeparator().repeat(2);
        }
        return "";
    }


    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        Objects.requireNonNull(token);
        Objects.requireNonNull(jarFile);
        try {
            final Path tempDir = Files.createTempDirectory("temp");
            final Path javaFilePath = implementAndReturnFilePath(token, tempDir);
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new ImplerException("Needs compiler to compile implementation");
            }
            final String[] args = Stream.of(javaFilePath.toString(),
                            "-sourcepath", tempDir.toString(),
                            "-cp", getClassPath(token))
                    .toArray(String[]::new);
            if (compiler.run(null, null, null, args) != 0) {
                throw new ImplerException("Error on compilation of implementation");
            }
            final Path classFilePath = javaFilePath.getParent().resolve(getFileName(token, CLASS));
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Name.IMPLEMENTATION_VERSION, "1.0.0");
            manifest.getMainAttributes().put(Name.IMPLEMENTATION_VENDOR, "Chulkov Alexey");
            try (
                    final JarOutputStream jarOutputStream =
                            new JarOutputStream(Files.newOutputStream(jarFile), manifest)
            ) {
                jarOutputStream.putNextEntry(
                        new JarEntry(
                                token.getPackageName().replace(".", JAR_PATH_SEPARATOR)
                                        + JAR_PATH_SEPARATOR
                                        + getFileName(token, CLASS)
                        ));
                Files.copy(classFilePath, jarOutputStream);
            } catch (final IOException e) {
                throw new ImplerException("Error on writing in jar-file", e);
            }
            Files.walkFileTree(tempDir, DELETE);
        } catch (final IOException e) {
            System.err.println("Can't create temporary directory");
        }
    }
}
