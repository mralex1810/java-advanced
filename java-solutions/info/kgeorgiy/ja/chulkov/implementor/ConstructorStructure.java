package info.kgeorgiy.ja.chulkov.implementor;

import java.lang.reflect.Constructor;

public class ConstructorStructure extends MethodStructure {


    public ConstructorStructure(final Constructor<?> constructor, final String name) {
        super(name, null, constructor.getParameterTypes(), constructor.getExceptionTypes(),
                constructor.getModifiers());
    }

    public ConstructorStructure(final String name, final Class<?>[] typeParameters, final Class<?>[] exceptions,
            final int modifiers) {
        super(name, null, typeParameters, exceptions, modifiers);
    }


    @Override
    protected String override() {
        return "";
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
