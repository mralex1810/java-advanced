package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Structure that parse and keeps information about constructor to implement it. Generates string for constructor
 * implementation by {@link ConstructorStructure#toString()}
 */
public class ConstructorStructure extends MethodStructure {

    /**
     * Creates {@link MethodStructure} from {@link Method} and creating class name
     *
     * @param constructor token of method that needs to be implemented
     * @param name        name of class, creating by this constructor
     */
    public ConstructorStructure(final Constructor<?> constructor, final String name) {
        this(name, constructor.getParameterTypes(), constructor.getExceptionTypes(),
                constructor.getModifiers());
    }

    /**
     * Creates {@link MethodStructure} by fields
     *
     * @param name           name of class that constructs by this constructor structure
     * @param typeParameters types of parameters of this constructor
     * @param exceptions     exceptions that throws by this constructor
     * @param modifiers      modifiers of this constructor
     */
    public ConstructorStructure(final String name, final Class<?>[] typeParameters, final Class<?>[] exceptions,
            final int modifiers) {
        super(name, null, typeParameters, exceptions, modifiers);
    }

    /**
     * @return empty string, because we can't override constructors
     */
    @Override
    protected String overrideRepresentation() {
        return "";
    }

    /**
     * @return empty string, because constructor haven't return type
     */
    @Override
    protected String returnTypeRepresentation() {
        return "";
    }

    /**
     * @return correct body of constructor, variable names generating by {@link MethodStructure#getVarName(int)}
     */
    @Override
    protected String bodyRepresentation() {
        return "super(" +
                IntStream.range(0, parameterTypes.size())
                        .mapToObj(this::getVarName)
                        .collect(Collectors.joining(", ")) + ");";
    }
}
