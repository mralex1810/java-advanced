package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemotePersonImpl extends AbstractPerson implements RemotePerson {

    private final int port;

    /**
     * Creates {@link RemotePersonImpl} class by person data and port
     *
     */
    public RemotePersonImpl(final PersonData personData, final int port) {
        super(personData.firstName(), personData.secondName(), personData.passport());
        this.port = port;
    }


    @Override
    void export(final Account account) throws RemoteException {
        UnicastRemoteObject.exportObject(account, port);
    }
}
