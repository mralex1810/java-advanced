package info.kgeorgiy.ja.chulkov.implementator;

import java.lang.reflect.Constructor;

public class ConstructorStructure extends MethodStructure {


    public ConstructorStructure(Constructor<?> constructor, String name) {
        super(name, null, constructor.getParameterTypes(), constructor.getExceptionTypes(),
                constructor.getModifiers());
    }

    public ConstructorStructure(String name, Class<?>[] typeParameters, Class<?>[] exceptions,
            int modifiers) {
        super(name, null, typeParameters, exceptions, modifiers);
    }

    @Override
    protected String returnType() {
        return "";
    }

    @Override
    protected String body() {
        return "super(" + String.join(", ", getVarNames(typeParameters.size())) + ");";
    }
}
