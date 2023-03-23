package info.kgeorgiy.ja.chulkov.implementor;


import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
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

public class Implementor implements JarImpler {

    public static final String JAVA = ".java";
    public static final String IMPL = "Impl";
    public static final String JAR_PATH_SEPARATOR = "/";
    public static final String CLASS = ".class";
    private static final Map<Predicate<Class<?>>, String> EXCEPTIONS_REASONS = Map.of(
            Class::isPrimitive, "Token mustn't be primitive",
            Class::isArray, "Token mustn't be primitive",
//            Class::isRecord, "Can't extend records",
//            Class::isSealed, "Can't extend sealed class",
            it -> it.isAssignableFrom(Enum.class), "Token mustn't be enum",
            it -> Modifier.isPrivate(it.getModifiers()), "Can't implement private token",
            it -> !it.isInterface() && Modifier.isFinal(it.getModifiers()), "Superclass must be not final",
            it -> !it.isInterface() && ImplClassStructure.getNonPrivateConstructorsStream(it).findAny()
                    .isEmpty(), "Superclass must has not private constructor");
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

    public static void main(final String[] args) {
        Objects.requireNonNull(args);
        if (args.length != 1 && args.length != 3) {
            printUsages();
        }
        for (final var c : args) {
            Objects.requireNonNull(c);
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

    private static void printUsages() {
        System.err.println("""
                Usages:
                [java class file]
                -jar [java class file] [jar file]
                """);
    }

    private static String getTypeName(final Class<?> token) {
        return token.getSimpleName() + IMPL;
    }

    private static String getFileName(final Class<?> token, final String extension) {
        return getTypeName(token) + extension;
    }

    private static void tryCreateDirectories(final Path implemetationPath) {
        try {
            Files.createDirectories(implemetationPath);
        } catch (final IOException e) {
            System.err.println("Can't create directories");
        }
    }

    private static String packageNameToStringWithSep(final Class<?> token, final String separator) {
        return token.getPackageName().replace(".", separator);
    }

    private String getClassPath(final Class<?> token) throws ImplerException {
        try {
            if (token.getProtectionDomain() == null || token.getProtectionDomain().getCodeSource() == null) {
                return "";
            }
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Can't get class path", e);
        }
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        implementAndReturnFilePath(token, root);
    }

    private Path implementAndReturnFilePath(final Class<?> token, final Path root) throws ImplerException {
        Objects.requireNonNull(token);
        Objects.requireNonNull(root);
        for (final var reasonMessageEntry : EXCEPTIONS_REASONS.entrySet()) {
            if (reasonMessageEntry.getKey().test(token)) {
                throw new ImplerException(reasonMessageEntry.getValue());
            }
        }
        final Path implemetationPath = root.resolve(packageNameToStringWithSep(token, File.separator));
        tryCreateDirectories(implemetationPath);
        final String typeName = getTypeName(token);
        final Path filePath = implemetationPath.resolve(getFileName(token, JAVA));
        try (final BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            addPackage(token, writer);
            writer.write(System.lineSeparator());
            final ImplInterfaceStructure implementedClassStructure =
                    token.isInterface() ? new ImplInterfaceStructure(token, typeName)
                            : new ImplClassStructure(token, typeName);
            writer.write(implementedClassStructure.toString());
        } catch (final IOException e) {
            throw new ImplerException("Error on writing in file", e);
        }
        return filePath;
    }

    private void addPackage(final Class<?> token, final Writer writer) throws IOException {
        if (!token.getPackageName().isEmpty()) {
            writer.write("package " + token.getPackageName() + ";" + System.lineSeparator());
        }
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
           final String[] args =Stream.of(javaFilePath.toString(),
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
            try (final JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile),
                    manifest)) {
                jarOutputStream.putNextEntry(new JarEntry(
                        packageNameToStringWithSep(token, JAR_PATH_SEPARATOR) + JAR_PATH_SEPARATOR
                                + getFileName(token, CLASS)));
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
