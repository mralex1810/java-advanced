package info.kgeorgiy.ja.chulkov.bank.person;

import java.io.Serializable;

public record PersonData(String firstName, String secondName, String passport) implements Serializable {

}
