package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person, Serializable {

    protected final String firstName;
    protected final String secondName;
    protected final String passport;
    protected final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();


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
    public Account getAccount(final String id) {
        return accounts.get(passport + ":" + id);
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    public Account createAccount(final String id) throws RemoteException {
        final var accountId = passport + ":" + id;
        System.out.println("Creating remote account " + accountId);
        final Account account = new AccountImpl(accountId);
        if (accounts.putIfAbsent(accountId, account) == null) {
            export(account);
            return account;
        } else {
            return getAccount(accountId);
        }
    }

    protected abstract void export(final Account account) throws RemoteException;
}
