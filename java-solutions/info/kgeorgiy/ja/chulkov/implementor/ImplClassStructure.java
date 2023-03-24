package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Structure that parse and keeps information about class to implement it. Generates string for class implementation by
 * {@link ImplClassStructure#toString()}
 */
public class ImplClassStructure extends ImplInterfaceStructure {


    /**
     * Predicate to check abstract and non-public. Returns true if method is abstract and non-public
     */
    private static final Predicate<Method> ABSTRACT_AND_NONPUBLIC_METHOD_PREDICATE =
            ABSTRACT_METHOD_PREDICATE.and(method -> !Modifier.isPublic(method.getModifiers()));
    /**
     * Predicate to check non-private or private constructors. Returns true if method is non-private
     */
    private static final Predicate<Constructor<?>> NONPRIVATE_CONSTRUCTOR_PREDICATE =
            it -> !Modifier.isPrivate(it.getModifiers());

    /**
     * Directly creates class
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
        return Stream.of(
                getNonPrivateConstructorsStream(token)
                        .map(it -> (MethodStructure) new ConstructorStructure(it, name)),
                getAllAbstractNonPublicMethodStructures(token).stream(),
                Arrays.stream(token.getMethods())
                        .filter(ABSTRACT_METHOD_PREDICATE)
                        .map(MethodStructure::new)
                ).flatMap(Function.identity()).toList();

    }

    /**
     * Gets stream of non-private constructors from {@code token} class.
     *
     * @param token class token to get constructors
     * @return stream of non-private constructors
     */
    static Stream<Constructor<?>> getNonPrivateConstructorsStream(final Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(NONPRIVATE_CONSTRUCTOR_PREDICATE);
    }

    /**
     * Recursive get all methods that are abstract and non-public in {@code superType} from parents classes.
     *
     * @param superType token to get methods for
     * @return set of {@link MethodStructure} that are abstract and non-public methods in {@code superType}
     */
    private static Set<MethodStructure> getAllAbstractNonPublicMethodStructures(final Class<?> superType) {
        if (superType == null) {
            return new HashSet<>();
        }
        final Set<MethodStructure> set = getAllAbstractNonPublicMethodStructures(superType.getSuperclass());
        Arrays.stream(superType.getDeclaredMethods())
                .filter(ABSTRACT_AND_NONPUBLIC_METHOD_PREDICATE)
                .map(MethodStructure::new)
                .forEach(set::add);
        // :NOTE: здесь точно есть что удалять?
        /* :NOTE-ANSWER: Да. Вот пример: {class A, class B extends A}
            abstract A.bar()
            final B.bar() {}
            В этом случае мы должны исключить bar() из множества абстрактных, так как не можем его реализовать
         */
        Arrays.stream(superType.getDeclaredMethods())
                .filter(ABSTRACT_AND_NONPUBLIC_METHOD_PREDICATE.negate())
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
