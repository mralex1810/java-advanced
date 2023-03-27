package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Structure that parse and keeps information about method to implement it. Generates string for method implementation
 * by {@link MethodStructure#toString()}
 */
public class MethodStructure {

    /**
     * Prefix of variables names in implemented methods
     */
    private static final String VAR = "var";

    /**
     * Number of whitespaces by Google Style Guide x2 as tabulation
     */
    private static final int TABULATION_SIZE = 4;

    /**
     * Tabulation requires by Google Style Guide x2
     */
    private static final String TABULATION = " ".repeat(TABULATION_SIZE);


    /**
     * Keeps name of method
     */
    protected final String name;
    /**
     * Keeps Class of return type
     */
    protected final Class<?> returnType;
    /**
     * Keeps Classes of method parameters
     */
    protected final List<Class<?>> parameterTypes;
    /**
     * Keeps Classes of exceptions throws by this method
     */
    protected final List<Class<?>> exceptions;
    /**
     * Keeps int view of method modifiers
     */
    protected final int modifiers;

    /**
     * Creates {@link MethodStructure} from {@link Method}
     *
     * @param method token of method that needs to be implemented
     */
    public MethodStructure(final Method method) {
        this(method.getName(), method.getReturnType(), method.getParameterTypes(), method.getExceptionTypes(),
                method.getModifiers());
    }

    /**
     * Creates {@link MethodStructure} by fields
     *
     * @param name           same as {@link MethodStructure#name}
     * @param returnType     same as {@link MethodStructure#returnType}
     * @param parameterTypes Array view of {@link MethodStructure#parameterTypes}
     * @param exceptions     Array view of {@link MethodStructure#exceptions}
     * @param modifiers      same as {@link MethodStructure#modifiers}
     */
    protected MethodStructure(final String name, final Class<?> returnType, final Class<?>[] parameterTypes,
            final Class<?>[] exceptions, final int modifiers) {
        this.name = name;
        this.returnType = returnType;
        // :NOTE: typeParameters и parameterTypes это разные вещи
        // :NOTE-ANSWER: fixed
        this.parameterTypes = Arrays.stream(parameterTypes).toList();
        this.exceptions = Arrays.stream(exceptions).toList();
        this.modifiers = modifiers;
    }

    /**
     * Generates string presentation of implemented method. Format method signature by one \t and method body by two
     * \t.
     *
     * @return correct formatted string for implemented method
     */
    @Override
    public String toString() {
        // :NOTE: какие переводы строк здесь генерируются?
        // :NOTE-ANSWER: fixed. Method from doc https://docs.oracle.com/en/java/javase/15/text-blocks/index.html
        return String.format("%s%s%s%s%s(%s)%s {%n%s%s%s%n}",
                TABULATION,
                overrideRepresentation(),
                modifiersRepresentation(),
                returnTypeRepresentation(),
                nameRepresentation(),
                parametersRepresentation(),
                exceptionsRepresentation(),
                TABULATION.repeat(2),
                bodyRepresentation(),
                TABULATION
        );
    }


    /**
     * Generates Override annotation with line separator and tabulation
     *
     * @return generated string
     */
    // :NOTE: лучше запихнуть переводы строк и табы в toString для единого вида
    // :NOTE-ANSWER: это, к сожалению, убьет форматирование. Или мы можем позволить себе лишние пробелы и строки?
    protected String overrideRepresentation() {
        // :NOTE: пропали аннотации
        return "@Override" + System.lineSeparator() + TABULATION;
    }


    /**
     * Generate string of modifies with whitespace on the end,
     *
     * @return generated string eor empty string if not modifiers
     */
    private String modifiersRepresentation() {
        // :NOTE: для этого есть стандартный метод
        // :NOTE-ANSWER: fixed
        final String result = Modifier.toString(modifiers & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
        return result.equals("") ? "" : result + " ";
    }


    /**
     * Gets canonical name of {@link MethodStructure#returnType} with whitespace
     *
     * @return gotten string
     */
    protected String returnTypeRepresentation() {
        return returnType.getCanonicalName() + " ";
    }

    /**
     * Gets name of method
     *
     * @return gotten string
     */
    protected String nameRepresentation() {
        return name;
    }

    /**
     * Generate string of method parameters. For generate variables names use {@link MethodStructure#getVarName}
     *
     * @return string of method parameters
     */
    protected String parametersRepresentation() {
        // :NOTE: varList тебе не нужен
        // :NOTE-ANSWER: fixed
        return IntStream.range(0, parameterTypes.size())
                .mapToObj(i -> parameterTypes.get(i).getCanonicalName() + " " + getVarName(i))
                .collect(Collectors.joining(", "));
    }

    /**
     * Generate unique with another index variable name
     *
     * @param index of parameter
     * @return unique variable name
     */
    protected String getVarName(final int index) {
        return VAR + index;
    }

    /**
     * Generates string representation of methods throwing exception.
     *
     * @return generated string or empty string, if no exception throws by method.
     */
    protected String exceptionsRepresentation() {
        if (exceptions.isEmpty()) {
            return "";
        }
        return exceptions.stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", ""));
    }

    /**
     * Generates body of method, returning default value. For get default value use
     * {@link MethodStructure#defaultReturnValueString}
     *
     * @return generated body of method
     */
    protected String bodyRepresentation() {
        return "return " + defaultReturnValueString() + ";";
    }

    /**
     * Generate string representation of default value for return type
     *
     * @return string representation of default value
     */
    private String defaultReturnValueString() {
        if (!returnType.isPrimitive()) {
            return "null";
        } else if (returnType.equals(Boolean.TYPE)) {
            return "false";
        } else if (returnType.equals(Void.TYPE)) {
            return "";
        } else {
            return "0";
        }
    }

    /**
     * Check with other {@link MethodStructure} by {@link MethodStructure#name} and
     * {@link MethodStructure#parameterTypes}
     *
     * @param other object
     * @return boolean of equality
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof MethodStructure that) {
            return Objects.equals(name, that.name)
                    && Objects.equals(parameterTypes, that.parameterTypes);
        }
        return false;
    }

    /**
     * Generates hash by {@link MethodStructure#name} and {@link MethodStructure#parameterTypes}
     *
     * @return hash of this {@code MethodStructure}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, parameterTypes);
    }
}
