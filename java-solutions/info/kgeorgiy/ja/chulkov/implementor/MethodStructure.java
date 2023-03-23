package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public class MethodStructure {

    public static final String VAR = "var";
    private static final Map<IntPredicate, String> METHOD_MODIFIERS_STRING = Map.of(
            Modifier::isPublic, "public",
            Modifier::isProtected, "protected",
            Modifier::isPrivate, "private"
    );

    protected final String name;
    protected final Class<?> returnType;
    protected final List<Class<?>> typeParameters;
    protected final List<Class<?>> exceptions;
    protected final int modifiers;

    public MethodStructure(final Method method) {
        this(method.getName(), method.getReturnType(), method.getParameterTypes(), method.getExceptionTypes(),
                method.getModifiers());
    }

    protected MethodStructure(final String name, final Class<?> returnType, final Class<?>[] typeParameters,
            final Class<?>[] exceptions, final int modifiers) {
        this.name = name;
        this.returnType = returnType;
        // :NOTE: typeParameters и parameterTypes это разные вещи
        this.typeParameters = Arrays.stream(typeParameters).toList();
        this.exceptions = Arrays.stream(exceptions).toList();
        this.modifiers = modifiers;
    }

    @Override
    public String toString() {
        // :NOTE: какие переводы строк здесь генерируются?
        return String.format("""                
                        \t%s%s%s%s(%s)%s {
                        \t\t%s
                        \t}
                        """,
                override(),
                modifiers(),
                returnType(),
                name(),
                parameters(),
                exceptions(),
                body()
        );
    }

    // :NOTE: лучше запихнуть переводы строк и табы в toString для единого вида
    protected String override() {
        return "@Override" + System.lineSeparator() + "\t";
    }


    private String modifiers() {
        // :NOTE: для этого есть стандартный метод
        final List<String> list = new ArrayList<>();
        METHOD_MODIFIERS_STRING.forEach((predicate, str) -> {
            if (predicate.test(modifiers)) {
                list.add(str);
            }
        });
        return String.join(" ", list) + (!list.isEmpty() ? " " : "");
    }


    protected String returnType() {
        return returnType.getCanonicalName() + " ";
    }

    protected String name() {
        return name;
    }

    protected String parameters() {
        final List<String> list = new ArrayList<>(typeParameters.size());
        // :NOTE: varList тебе не нужен
        final List<String> varList = getVarNames(typeParameters.size());
        for (int i = 0; i < typeParameters.size(); i++) {
            list.add(typeParameters.get(i).getCanonicalName() + " " + varList.get(i));
        }
        return String.join(", ", list);
    }

    protected List<String> getVarNames(final int size) {
        final List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(VAR + i);
        }
        return list;
    }

    protected String exceptions() {
        if (exceptions.isEmpty()) {
            return "";
        }
        return exceptions.stream().map(Class::getCanonicalName).collect(Collectors.joining(", ", " throws ", ""));
    }

    protected String body() {
        return "return " + defaultReturnValueString() + ";";
    }

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MethodStructure that) {
            return Objects.equals(name, that.name)
                    && Objects.equals(typeParameters, that.typeParameters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, typeParameters);
    }
}
