package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.RemoteAccount;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemotePersonImpl extends AbstractPerson<RemoteAccount> implements RemotePerson {

    private final int port;

    public RemotePersonImpl(final String firstName, final String secondName, final String passport, final int port) {
        super(firstName, secondName, passport);
        this.port = port;
    }

    public RemotePersonImpl(final PersonData personData, final int port) {
        this(personData.firstName(), personData.secondName(), personData.passport(), port);
    }


    @Override
    public RemoteAccount createAccount(final String id) throws RemoteException {
        final var accountId = passport + ":" + id;
        System.out.println("Creating remote account " + accountId);
        final RemoteAccount account = new RemoteAccount(accountId);
        if (accounts.putIfAbsent(accountId, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(accountId);
        }
    }
}
