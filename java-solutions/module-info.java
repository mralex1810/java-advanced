/**
 * Chulkov Alexey homeworks for <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author Chulkov Alexey (chulkovalex18@gmail.com)
 */
module info.kgeorgiy.ja.chulkov {
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires java.compiler;

    exports info.kgeorgiy.ja.chulkov.implementor;
    exports info.kgeorgiy.ja.chulkov.arrayset;
    exports info.kgeorgiy.ja.chulkov.student;
    exports info.kgeorgiy.ja.chulkov.walk;

    opens info.kgeorgiy.ja.chulkov.implementor to info.kgeorgiy.java.advanced.implementor,
            info.kgeorgiy.java.advanced.base;
    opens info.kgeorgiy.ja.chulkov.student to info.kgeorgiy.java.advanced.student, info.kgeorgiy.java.advanced.base;
    opens info.kgeorgiy.ja.chulkov.arrayset to info.kgeorgiy.java.advanced.arrayset, info.kgeorgiy.java.advanced.base;
    opens info.kgeorgiy.ja.chulkov.walk to info.kgeorgiy.java.advanced.walk, info.kgeorgiy.java.advanced.base;
}