package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Structure that parse and keeps information about class to implement it. Generates string for class implementation by
 * {@link ImplClassStructure#toString()}
 */
public class ImplClassStructure extends ImplInterfaceStructure {


    /**
     * Directly creates class
     *
     * @param typeName  same as {@link ImplInterfaceStructure#typeName}
     * @param superType same as {@link ImplInterfaceStructure#superType}
     * @param methods   same as {@link ImplInterfaceStructure#methods}
     */
    public ImplClassStructure(final String typeName, final String superType,
            final List<MethodStructure> methods
    ) {
        super(typeName, superType, methods);
    }


    /**
     * Parse class token to create {@link ImplClassStructure}. Gets all non-private constructors and all non-private
     * abstract and public methods
     *
     * @param superType class to be implemented
     * @param name      implemented class name
     */
    public ImplClassStructure(final Class<?> superType, final String name) {
        // :NOTE: очень сложно и не читаемо
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

    /**
     * Gets stream of non-private constructors.
     *
     * @param token class token to get constructors
     * @return stream of non-private constructors
     */
    static Stream<Constructor<?>> getNonPrivateConstructorsStream(final Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(it -> !Modifier.isPrivate(it.getModifiers()));
    }

    /**
     * Recursive get all methods that are abstract in {@code superType} from parents classes.
     *
     * @param superType token to get abstract methods for
     * @return set of {@link MethodStructure} that are abstract methods in {@code superType}
     */
    private static Set<MethodStructure> getAllAbstractMethodStructures(final Class<?> superType) {
        if (superType == null) {
            return new HashSet<>();
        }
        final Set<MethodStructure> set = getAllAbstractMethodStructures(superType.getSuperclass());
        Arrays.stream(superType.getDeclaredMethods())
                .filter(it -> Modifier.isAbstract(it.getModifiers()))
                .map(MethodStructure::new)
                .forEach(set::add);
        // :NOTE: здесь точно есть что удалять?
        /* :NOTE-ANSWER: Да. Вот пример: (class A, class B extends A)
            abstract A.bar()
            final B.bar() {}
            В этом случае мы должны исключить bar() из множества абстрактных, так как не можем его реализовать
         */
        Arrays.stream(superType.getDeclaredMethods())
                .filter(it -> !Modifier.isAbstract(it.getModifiers()))
                .map(MethodStructure::new)
                .forEach(set::remove);
        return set;
    }

    /**
     * Generate string for extends super class
     *
     * @return generated string
     */
    @Override
    protected String implementingSuperTypePresentation() {
        return "extends " + superType;
    }
}
