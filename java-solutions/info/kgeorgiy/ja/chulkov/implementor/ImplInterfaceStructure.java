package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Structure that parse and keeps information about interface to implement it. Generates string for interface
 * implementation by {@link ImplInterfaceStructure#toString()}
 */
public class ImplInterfaceStructure {

    /**
     * Predicate to check abstract or non-abstract method.
     * Returns true on abstract method
     */
    protected static final Predicate<Method> ABSTRACT_METHOD_PREDICATE =
            method -> Modifier.isAbstract(method.getModifiers());


    /**
     * Keeps name of implemented class
     */
    protected final String typeName;
    /**
     * Keeps name of type to be implemented
     */
    protected final String superType;
    /**
     * Keeps list of {@link MethodStructure}, that keeps information about methods and constructors of implemented
     * class
     */
    protected final List<? extends MethodStructure> methods;

    /**
     * Directly creates class
     *
     * @param typeName  same as {@link ImplInterfaceStructure#typeName}
     * @param superType same as {@link ImplInterfaceStructure#superType}
     * @param methods   same as {@link ImplInterfaceStructure#methods}
     */
    public ImplInterfaceStructure(final String typeName, final String superType,
            final List<? extends MethodStructure> methods
    ) {
        this.typeName = typeName;
        this.superType = superType;
        this.methods = methods;
    }

    /**
     * Parse interface token to create {@link ImplInterfaceStructure}
     *
     * @param superType interface to be implemented
     * @param name      implemented class name
     */
    public ImplInterfaceStructure(final Class<?> superType, final String name) {
        this.typeName = name;
        this.superType = superType.getCanonicalName();
        this.methods = Arrays.stream(superType.getMethods())
                .filter(ABSTRACT_METHOD_PREDICATE)
                .map(MethodStructure::new)
                .toList();
    }


    /**
     * Generates string presentation of implemented class
     *
     * @return string for implemented class
     */
    @Override
    public String toString() {
        return String.format("""
                        public class %s %s {
                                                
                        %s}
                        """,
                typeNamePresentation(),
                implementingSuperTypePresentation(),
                methodsPresentation()
        ).replaceAll("\n", System.lineSeparator());
    }

    /**
     * Gets type name.
     *
     * @return type name as string
     */
    protected String typeNamePresentation() {
        return typeName;
    }

    /**
     * Generates string of implementing interface in field {@link ImplInterfaceStructure#superType}
     *
     * @return string for implement interface
     */
    protected String implementingSuperTypePresentation() {
        return "implements " + superType;
    }


    /**
     * Generates string presentation of implemented class body with all methods (including constructors) concatenating
     * implementation of methods (produced by {@link MethodStructure#toString()}) with correct formatting.
     *
     * @return class body with realisation of all methods
     */
    protected String methodsPresentation() {
        return methods.stream()
                .map(MethodStructure::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
