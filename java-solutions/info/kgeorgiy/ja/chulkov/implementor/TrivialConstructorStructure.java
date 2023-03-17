package info.kgeorgiy.ja.chulkov.implementor;

public class TrivialConstructorStructure extends ConstructorStructure {

    public TrivialConstructorStructure(final String name, final int modifiers) {
        super(name, new Class[0], new Class[0], modifiers);
    }

    @Override
    protected String body() {
        return "";
    }
}
