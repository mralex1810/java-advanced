package info.kgeorgiy.ja.chulkov.implementator;

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
                Implementor.getTypeName(superType),
                Stream.concat(
                        Arrays.stream(superType.getDeclaredConstructors())
                                .filter(it -> !Modifier.isPrivate(it.getModifiers()))
                                .map(it -> new ConstructorStructure(it, name)),
                        Stream.concat(
                                getAllAbstractMethodsFromSuperClasses(superType).stream()
                                        .filter(method -> !Modifier.isPublic(method.modifiers)),
                                Arrays.stream(superType.getMethods())
                                        .filter(it -> Modifier.isAbstract(it.getModifiers()))
                                        .map(MethodStructure::new))
                ).toList());
    }

    private static Set<MethodStructure> getAllAbstractMethodsFromSuperClasses(Class<?> superType) {
        if (superType == null) {
            return new HashSet<>();
        }
        Set<MethodStructure> set = getAllAbstractMethodsFromSuperClasses(superType.getSuperclass());
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
    protected String superType() {
        return "extends " + superType;
    }

}
