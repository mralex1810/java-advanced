package info.kgeorgiy.ja.chulkov.student;


import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static info.kgeorgiy.ja.chulkov.student.StreamUtils.*;

public class StudentDB implements AdvancedQuery {

    public static final String EMPTY_STRING = "";
    public static final Comparator<Student> ID_COMPARATOR = Comparator.comparingInt(Student::getId);
    private static final Comparator<Student> STUDENT_COMPARATOR = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .reversed()
            .thenComparing(Student::getId);
    public static final BinaryOperator<String> STRING_MIN_OPERATOR =
            (first, second) -> first.compareTo(second) < 0 ? first : second;
    public static final Collector<Student, ?, TreeMap<GroupName, List<Student>>> GROUP_COLLECTOR =
            Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList());

    private static String studentFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapCollectiontoList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapCollectiontoList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapCollectiontoList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapCollectiontoList(students, StudentDB::studentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapCollection(students, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }


    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return maxAndMapOptional(students, ID_COMPARATOR, Student::getFirstName, EMPTY_STRING);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortCollectionByComparatorToList(students, ID_COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortCollectionByComparatorToList(students, STUDENT_COMPARATOR);
    }


    private <T, A, CR> CR findStudentBySmth(Collection<Student> students, T obj, Function<Student, T> mapper,
                                            Collector<Student, A, CR> collector) {
        return processCollectionByStream(students,
                (stream) -> stream.filter(student -> mapper.apply(student).equals(obj)).sorted(STUDENT_COMPARATOR),
                collector);
    }

    private <T> List<Student> findStudentBySmthToList(Collection<Student> students, T obj, Function<Student, T> mapper) {
        return findStudentBySmth(students, obj, mapper, Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentBySmthToList(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentBySmthToList(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentBySmthToList(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentBySmth(students, group, Student::getGroup,
                Collectors.toMap(Student::getLastName, Student::getFirstName, STRING_MIN_OPERATOR));
    }

    private List<Group> toGroupList(Map<GroupName, List<Student>> map) {
        return processCollectionByStreamToList(map.entrySet(),
                stream -> stream.map(it -> new Group(it.getKey(), it.getValue())));
    }

    private List<Group> getGroups(Collection<Student> students) {
        return toGroupList(processCollectionByStream(students, Function.identity(), GROUP_COLLECTOR));
    }

    private List<Group> getGroupsBySmth(Collection<Student> students, Comparator<Student> comparator) {
        return toGroupList(sortCollectionByComparator(students, comparator, GROUP_COLLECTOR));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsBySmth(students, STUDENT_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsBySmth(students, ID_COMPARATOR);
    }

    private GroupName getLargestGroup(Collection<Student> students, Comparator<Group> comparator) {
        return maxAndMapOptional(getGroups(students), comparator, Group::getName, null);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroup(students, Comparator.comparing((Group group) -> group.getStudents().size())
                .thenComparing(group -> group.getName().name()));
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroup(students,
                Comparator.comparingInt((Group group) -> getDistinctFirstNames(group.getStudents()).size())
                        .reversed()
                        .thenComparing(group -> group.getName().name())
                        .reversed());
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return maxAndMapOptional(
                students.stream()
                        .collect(Collectors.groupingBy(Student::getFirstName, Collectors.toList()))
                        .entrySet(),
                Comparator.comparingLong((Map.Entry<String, List<Student>> entry) ->
                                entry.getValue().stream().map(Student::getGroup).distinct().count())
                        .thenComparing(Map.Entry::getKey),
                Map.Entry::getKey,
                EMPTY_STRING
        );
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return mapByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return mapByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return mapByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return mapByIndices(students, indices, StudentDB::studentFullName);
    }
}
