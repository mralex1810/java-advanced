package info.kgeorgiy.ja.chulkov.implementor;


import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class Implementor implements Impler {

    private static final Map<Predicate<Class<?>>, String> EXCEPTIONS_REASONS = Map.of(
            Class::isPrimitive, "Token mustn't be primitive",
            Class::isArray, "Token mustn't be primitive",
            it -> it.isAssignableFrom(Enum.class), "Token mustn't be enum",
            it -> Modifier.isPrivate(it.getModifiers()), "Can't implement private token",
            it -> !it.isInterface() && Modifier.isFinal(it.getModifiers()), "Superclass must be not final",
            it -> !it.isInterface() && ImplClassStructure.getNonPrivateConstructorsStream(it).findAny().isEmpty(),
            "Superclass must has not private constructor"
    );
    public static final String JAVA = ".java";
    public static final String IMPL = "Impl";

    private static String getTypeName(Class<?> token) {
        return token.getSimpleName() + IMPL;
    }

    private static void createDirectories(Path implemetationPath) {
        try {
            Files.createDirectories(implemetationPath);
        } catch (IOException e) {
            System.err.println("Can't create directories");
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        Objects.requireNonNull(token);
        Objects.requireNonNull(root);
        for (var reasonMessageEntry : EXCEPTIONS_REASONS.entrySet()) {
            if (reasonMessageEntry.getKey().test(token)) {
                throw new ImplerException(reasonMessageEntry.getValue());
            }
        }
        final Path implemetationPath = root.resolve(token.getPackageName().replace(".", File.separator));
        createDirectories(implemetationPath);
        var typeName = getTypeName(token);
        final Path filePath = implemetationPath.resolve(typeName + JAVA);
        try (var writer = Files.newBufferedWriter(filePath)) {
            addPackage(token, writer);
            if (token.isInterface()) {
                writer.write(new ImplInterfaceStructure(token, typeName).toString());
            } else {
                writer.write(new ImplClassStructure(token, typeName).toString());
            }
        } catch (IOException e) {
            throw new ImplerException("Error on writing in file", e);
        }
    }

    private void addPackage(Class<?> token, Writer writer) throws IOException {
        writer.write("package " + token.getPackageName() + ";" + System.lineSeparator());
    }
}
