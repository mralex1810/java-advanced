package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;

public interface LocalPerson extends RemotePerson {

    @Override
    String getFirstName();

    @Override
    String getSecondName();

    @Override
    String getPassport();

    @Override
    Account createAccount(String id);

    @Override
    Account getAccount(String id);
}
