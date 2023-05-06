package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson<T extends Account> implements Person, Serializable {

    protected final String firstName;
    protected final String secondName;
    protected final String passport;
    protected final ConcurrentMap<String, T> accounts = new ConcurrentHashMap<>();


    protected AbstractPerson(final String firstName, final String secondName, final String passport) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.passport = passport;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getSecondName() {
        return secondName;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    @Override
    public T getAccount(final String id) {
        return accounts.get(passport + ":" + id);
    }

    @Override
    public Map<String, T> getAccounts() {
        return accounts;
    }
}
