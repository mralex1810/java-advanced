package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemotePerson extends AbstractPerson implements Person {

    private final int port;

    public RemotePerson(final String firstName, final String secondName, final String passport, final int port) {
        super(firstName, secondName, passport);
        this.port = port;
    }

    public RemotePerson(final PersonData personData, final int port) {
        this(personData.firstName(), personData.secondName(), personData.passport(), port);
    }


    @Override
    protected void export(final Account account) throws RemoteException {
        UnicastRemoteObject.exportObject(account, port);
    }
}
