package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImplInterfaceStructure {

    protected final String typeName;
    protected final String superType;
    protected final List<MethodStructure> methods;

    public ImplInterfaceStructure(final String typeName, final String superType,
            final List<MethodStructure> methods
    ) {
        this.typeName = typeName;
        this.superType = superType;
        this.methods = methods;
    }

    public ImplInterfaceStructure(final Class<?> superType, final String name) {
        this.typeName = name;
        this.superType = superType.getCanonicalName();
        this.methods = Stream.concat(
                Stream.of(trivialConstructor(typeName)),
                Arrays.stream(superType.getMethods())
                        .filter(it -> Modifier.isAbstract(it.getModifiers()))
                        .map(MethodStructure::new)
        ).toList();
    }

    private ConstructorStructure trivialConstructor(final String name) {
        return new TrivialConstructorStructure(name, Modifier.PUBLIC);
    }

    @Override
    public String toString() {
        return String.format("""
                        public class %s %s {
                        
                        %s}
                        """,
                typeName(),
                implementingSuperType(),
                methods()
        );
    }

    protected String typeName() {
        return typeName;
    }

    protected String implementingSuperType() {
        return "implements " + superType;
    }

    protected String methods() {
        return methods.stream()
                .map(MethodStructure::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
