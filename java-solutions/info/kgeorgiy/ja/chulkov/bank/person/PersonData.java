package info.kgeorgiy.ja.chulkov.bank.person;

import java.io.Serializable;

/**
 * Contains information about person
 *
 */
public record PersonData(String firstName, String secondName, String passport) implements Serializable {
}
