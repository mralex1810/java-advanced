package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract class AbstractPerson implements Serializable {

    protected final String firstName;
    protected final String secondName;
    protected final String passport;
    protected final ConcurrentMap<String, AccountImpl> accounts = new ConcurrentHashMap<>();


    AbstractPerson(final String firstName, final String secondName, final String passport) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.passport = passport;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public String getPassport() {
        return passport;
    }

    public Account getAccount(final String id) {
        return accounts.get(passport + ":" + id);
    }

    Map<String, AccountImpl> getAccounts() {
        return accounts;
    }

    public Account createAccount(final String id) throws RemoteException {
        final var accountId = passport + ":" + id;
        System.out.println("Creating remote account " + accountId);
        final AccountImpl account = new AccountImpl(accountId);
        if (accounts.putIfAbsent(accountId, account) == null) {
            export(account);
            return account;
        } else {
            return getAccount(accountId);
        }
    }

    abstract void export(final Account account) throws RemoteException;
}
