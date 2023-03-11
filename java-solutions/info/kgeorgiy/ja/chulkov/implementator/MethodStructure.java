package info.kgeorgiy.ja.chulkov.implementator;

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

    public MethodStructure(Method method) {
        this(method.getName(), method.getReturnType(), method.getParameterTypes(), method.getExceptionTypes(),
                method.getModifiers());
    }

    protected MethodStructure(String name, Class<?> returnType, Class<?>[] typeParameters,
            Class<?>[] exceptions, int modifiers) {
        this.name = name;
        this.returnType = returnType;
        this.typeParameters = Arrays.stream(typeParameters).toList();
        this.exceptions = Arrays.stream(exceptions).toList();
        this.modifiers = modifiers;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s(%s) %s { %s %s %s }",
                modifiers(),
                returnType(),
                name(),
                parameters(),
                exceptions(),
                System.lineSeparator(),
                body(),
                System.lineSeparator()
        );
    }


    private String modifiers() {
        List<String> list = new ArrayList<>();
        METHOD_MODIFIERS_STRING.forEach((predicate, str) -> {
            if (predicate.test(modifiers)) {
                list.add(str);
            }
        });
        return String.join(" ", list);
    }


    protected String returnType() {
        return returnType.getCanonicalName();
    }

    protected String name() {
        return name;
    }

    protected String parameters() {
        var list = new ArrayList<String>(typeParameters.size());
        var varList = getVarNames(typeParameters.size());
        for (int i = 0; i < typeParameters.size(); i++) {
            list.add(typeParameters.get(i).getCanonicalName() + " " + varList.get(i));
        }
        return String.join(", ", list);
    }

    protected List<String> getVarNames(int size) {
        var list = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            list.add(VAR + i);
        }
        return list;
    }

    protected String exceptions() {
        if (exceptions.isEmpty()) {
            return "";
        }
        return "throws " + exceptions.stream().map(Class::getCanonicalName).collect(Collectors.joining(", "));
    }

    protected String body() {
        return "return " + defaultReturnValueString() + ";";
    }

    private String defaultReturnValueString() {
        if (returnType.isPrimitive()) {
            if (returnType.getName().equals("boolean")) {
                return "false";
            } else if (returnType.getName().equals("void")) {
                return "";
            } else {
                return "0";
            }
        } else {
            return "null";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MethodStructure that) {
            return Objects.equals(name, that.name) && Objects.equals(
                    returnType, that.returnType) && Objects.equals(typeParameters, that.typeParameters)
                    && Objects.equals(exceptions, that.exceptions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, typeParameters, exceptions);
    }
}
