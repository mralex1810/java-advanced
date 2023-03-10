package info.kgeorgiy.ja.chulkov.student;


import static info.kgeorgiy.ja.chulkov.student.StreamUtils.mapCollection;
import static info.kgeorgiy.ja.chulkov.student.StreamUtils.mapCollectiontoList;
import static info.kgeorgiy.ja.chulkov.student.StreamUtils.maxAndMapOptional;
import static info.kgeorgiy.ja.chulkov.student.StreamUtils.processCollectionByStream;
import static info.kgeorgiy.ja.chulkov.student.StreamUtils.recollectCollection;
import static info.kgeorgiy.ja.chulkov.student.StreamUtils.sortCollectionByComparator;
import static info.kgeorgiy.ja.chulkov.student.StreamUtils.sortCollectionByComparatorToList;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {

    public static final String EMPTY_STRING = "";
    public static final Comparator<Student> ID_COMPARATOR = Comparator.comparingInt(Student::getId);
    public static final Collector<Student, ?, SortedMap<GroupName, List<Student>>> GROUP_COLLECTOR =
            Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList());
    private static final Comparator<Student> STUDENT_COMPARATOR =
            Comparator.comparing(Student::getLastName, Comparator.reverseOrder())
                    .thenComparing(Student::getFirstName, Comparator.reverseOrder())
                    .thenComparing(Student::getId);

    private static String studentFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    public static <R> List<R> mapByIds(Map<Integer, Student> collection, int[] indices, Function<Student, R> mapper) {
        return Arrays.stream(indices).mapToObj(collection::get).map(mapper).toList();
    }

    public static <R> List<R> mapByIds(Collection<Student> collection, int[] indices, Function<Student, R> mapper) {
        return mapByIds(collection.stream().collect(Collectors.toMap(Student::getId, Function.identity())),
                indices, mapper);
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

    private <R, A, CR> CR findStudentBySmth(Collection<Student> students, R obj, Function<Student, R> mapper,
            Collector<Student, A, CR> collector) {
        return processCollectionByStream(students,
                stream -> stream.filter(student -> mapper.apply(student).equals(obj)).sorted(STUDENT_COMPARATOR),
                collector);
    }

    private <T> List<Student> findStudentBySmthToList(Collection<Student> students, T obj,
            Function<Student, T> mapper) {
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
                Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private List<Group> toGroupList(Map<GroupName, List<Student>> map) {
        return mapCollectiontoList(map.entrySet(), it -> new Group(it.getKey(), it.getValue()));
    }

    private List<Group> getGroups(Collection<Student> students) {
        return toGroupList(recollectCollection(students, GROUP_COLLECTOR));
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

    private GroupName getLargestGroupNameByComparatorWithOrder(Collection<Student> students,
            Comparator<Group> comparator, Comparator<String> order) {
        return getLargestGroup(students, comparator.thenComparing(group -> group.getName().name(), order));
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroupNameByComparatorWithOrder(students,
                Comparator.comparingInt(group -> group.getStudents().size()),
                Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupNameByComparatorWithOrder(students,
                Comparator.comparingInt(group -> getDistinctFirstNames(group.getStudents()).size()),
                Comparator.reverseOrder());
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return maxAndMapOptional(
                recollectCollection(students, Collectors.groupingBy(Student::getFirstName, Collectors.toList()))
                        .entrySet(),
                Comparator.comparingLong((Map.Entry<String, List<Student>> entry) ->
                                entry.getValue().stream().map(Student::getGroup).distinct().count())
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()),
                Map.Entry::getKey,
                EMPTY_STRING
        );
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] ids) {
        return mapByIds(students, ids, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] ids) {
        return mapByIds(students, ids, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] ids) {
        return mapByIds(students, ids, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] ids) {
        return mapByIds(students, ids, StudentDB::studentFullName);
    }
}
