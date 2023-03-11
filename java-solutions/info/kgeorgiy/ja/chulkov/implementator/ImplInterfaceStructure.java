package info.kgeorgiy.ja.chulkov.implementator;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImplInterfaceStructure {
    protected final String typeName;
    protected final String superType;
    protected final List<MethodStructure> methods;

    public ImplInterfaceStructure(String typeName, String  superType,
            List<MethodStructure> methods
    ) {
        this.typeName = typeName;
        this.superType = superType;
        this.methods = methods;
    }

    public ImplInterfaceStructure(Class<?> superType, String name) {
        this.typeName = name;
        this.superType = superType.getCanonicalName();
        this.methods = Stream.concat(
                Stream.of(trivialConstructor(typeName)),
                Arrays.stream(superType.getMethods())
                        .filter(it -> Modifier.isAbstract(it.getModifiers()))
                        .map(MethodStructure::new)
        ).toList();
//        Arrays.stream(superType.getConstructors()).map(ConstructorStructure::new)

    }

    private ConstructorStructure trivialConstructor(String name) {
        return new ConstructorStructure(name, new Class[0], new Class[0], Modifier.PUBLIC);
    }

    @Override
    public String toString() {
        return String.format("public class %s %s { %s %s %s }",
                typeName(),
                superType(),
                System.lineSeparator(),
                methods(),
                System.lineSeparator()
        );
    }

    protected String typeName() {
        return typeName;
    }

    protected String superType() {
        return "implements " + superType;
    }

    protected String methods() {
        return methods.stream()
                .map(MethodStructure::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
