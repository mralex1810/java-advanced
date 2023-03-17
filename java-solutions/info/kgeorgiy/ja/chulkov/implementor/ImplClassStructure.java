package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ImplClassStructure extends ImplInterfaceStructure {

    public ImplClassStructure(String typeName, String superType,
            List<MethodStructure> methods
    ) {
        super(typeName, superType, methods);
    }

    public ImplClassStructure(Class<?> superType, String name) {
        this(name,
                superType.getCanonicalName(),
                Stream.concat(
                        getNonPrivateConstructorsStream(superType)
                                .map(it -> new ConstructorStructure(it, name)),
                        Stream.concat(
                                getAllAbstractMethodStructures(superType).stream()
                                        .filter(method -> !Modifier.isPublic(method.modifiers)),
                                Arrays.stream(superType.getMethods())
                                        .filter(it -> Modifier.isAbstract(it.getModifiers()))
                                        .map(MethodStructure::new))
                ).toList());
    }

    static Stream<Constructor<?>> getNonPrivateConstructorsStream(Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(it -> !Modifier.isPrivate(it.getModifiers()));
    }

    private static Set<MethodStructure> getAllAbstractMethodStructures(Class<?> superType) {
        if (superType == null) {
            return new HashSet<>();
        }
        Set<MethodStructure> set = getAllAbstractMethodStructures(superType.getSuperclass());
        Arrays.stream(superType.getDeclaredMethods())
                .filter(it -> Modifier.isAbstract(it.getModifiers()))
                .map(MethodStructure::new)
                .forEach(set::add);
        Arrays.stream(superType.getDeclaredMethods())
                .filter(it -> !Modifier.isAbstract(it.getModifiers()))
                .map(MethodStructure::new)
                .forEach(set::remove);
        return set;
    }

    @Override
    protected String implementingSuperType() {
        return "extends " + superType;
    }
}
