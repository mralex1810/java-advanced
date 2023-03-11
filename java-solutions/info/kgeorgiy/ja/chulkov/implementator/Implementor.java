package info.kgeorgiy.ja.chulkov.implementator;

import static java.lang.System.lineSeparator;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Implementor implements Impler {

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Token mustn't be primitive");
        }
        if (token.isArray()) {
            throw new ImplerException("Token mustn't be array");
        }
        if (token.isAssignableFrom(Enum.class)) {
            throw new ImplerException("Token mustn't be enum");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't implement private token");
        }
        final Path classPath = root.resolve(token.getPackageName().replace(".", "/"));
        try {
            Files.createDirectories(classPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var typeName = token.getSimpleName() + "Impl";
        final Path filePath = classPath.resolve(typeName + ".java");
        try (var writer = Files.newBufferedWriter(filePath)) {
            addPackage(token, writer);
            if (token.isInterface()) {
                writer.write(new ImplInterfaceStructure(token, typeName).toString());
            } else {
                checkClass(token);
                writer.write(new ImplClassStructure(token, typeName).toString());
            }
            writer.flush();
            System.out.println(Files.readString(filePath));
        } catch (IOException e) {
            throw new ImplerException("Error on writing in file", e);
        }
    }

    private void checkClass(Class<?> token) throws ImplerException {
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Superclass must be not final");
        }
        if (Arrays.stream(token.getDeclaredConstructors())
                .filter(it -> !Modifier.isPrivate(it.getModifiers())).findAny().isEmpty()) {
            throw new ImplerException("Superclass must has not private constructor");
        }
    }

    private void addPackage(Class<?> token, Writer writer) throws IOException {
        writer.write("package " + token.getPackageName() + ";" + lineSeparator());
    }
}
