package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Structure that parse and keeps information about class to implement it. Generates string for class implementation by
 * {@link ImplClassStructure#toString()}
 */
public class ImplClassStructure extends ImplInterfaceStructure {


    /**
     * Predicate to check non-private or private constructors. Returns true if method is non-private
     */
    private static final Predicate<Constructor<?>> NON_PRIVATE_CONSTRUCTOR_PREDICATE =
            it -> !Modifier.isPrivate(it.getModifiers());

    /**
     * Compares {@link MethodStructure} by return types in covariant context. Returns 1 if {@code a.returnType} is
     * assignable from {@code b.returnType}
     */
    private static final Comparator<MethodStructure> RETURN_TYPES_COVARIANT_COMPARATOR =
            (a, b) -> a == b ? 0 : a.returnType.isAssignableFrom(b.returnType) ? 1 : -1;

    /**
     * Directly creates class by fields
     *
     * @param typeName  same as {@link ImplInterfaceStructure#typeName}
     * @param superType same as {@link ImplInterfaceStructure#superType}
     * @param methods   same as {@link ImplInterfaceStructure#methods}
     */
    public ImplClassStructure(final String typeName, final String superType,
            final List<? extends MethodStructure> methods
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
        this(
                name,
                superType.getCanonicalName(),
                getRequiredForImplementationMethods(superType, name)
        );
    }

    /**
     * Generates list of required to implement methods and constructors. Merges three types of methods by signature:
     * <ul>
     *    <li> all non-private constructors </li>
     *    <li> all public abstract methods declared in all superclasses and interfaces </li>
     *    <li> all non-public abstract methods, declared in all superclasses </li>
     * </ul>
     *
     * @param token super class token, that needs to be implemented
     * @param name  of implemented class
     * @return list of required to implement methods
     */
    private static List<? extends MethodStructure> getRequiredForImplementationMethods(final Class<?> token,
            final String name) {
        final var methods = compressReturnedTypes(Arrays.stream(token.getMethods())
                .map(MethodStructure::new))
                .collect(Collectors.toSet());
        addAllParentsMethodsToSet(methods, token);
        return Stream.concat(
                methods.stream().filter(ABSTRACT_METHOD_STRUCTURE_PREDICATE),
                getNonPrivateConstructorsStream(token)
                        .map(it -> new ConstructorStructure(it, name))
        ).toList();
    }

    /**
     * Compress methods with same signature. Return one method with covariant type.
     *
     * @param methods stream of methods
     * @return stream of compressed methods
     */
    @SuppressWarnings("DataFlowIssue")
    private static Stream<MethodStructure> compressReturnedTypes(final Stream<MethodStructure> methods) {
        return methods
                .collect(Collectors.groupingBy(Function.identity(), Collectors.toList()))
                .values()
                .stream()
                .map(Collection::stream)
                .map(it -> it.min(RETURN_TYPES_COVARIANT_COMPARATOR))
                .map(Optional::get);
    }

    /**
     * Gets stream of non-private constructors from {@code token} class.
     *
     * @param token class token to get constructors
     * @return stream of non-private constructors
     */
    static Stream<Constructor<?>> getNonPrivateConstructorsStream(final Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(NON_PRIVATE_CONSTRUCTOR_PREDICATE);
    }

    /**
     * Recursive get all methods that are abstract and non-public in {@code token} from parents classes.
     *
     * @param token token to get methods for
     * @return set of {@link MethodStructure} that are abstract and non-public methods in {@code token}
     */
    private static Set<MethodStructure> addAllParentsMethodsToSet(final Set<MethodStructure> methods,
            final Class<?> token) {
        if (token == null) {
            return methods;
        }
        Arrays.stream(token.getDeclaredMethods())
                .map(MethodStructure::new)
                .forEach(methods::add);
        // :NOTE: здесь точно есть что удалять?
        /* :NOTE-ANSWER: Да. Вот пример: {class A, class B extends A}
            abstract A.bar()
            final B.bar() {}
            В этом случае мы должны исключить bar() из множества абстрактных, так как не можем его реализовать
         */
        return addAllParentsMethodsToSet(methods, token.getSuperclass());
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
