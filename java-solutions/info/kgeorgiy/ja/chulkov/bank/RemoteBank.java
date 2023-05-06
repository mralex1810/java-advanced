package info.kgeorgiy.ja.chulkov.bank;

import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.Person.PersonData;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePerson;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePersonImpl;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {

    private final int port;
    private final ConcurrentMap<PersonData, RemotePerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public LocalPerson getLocalPerson(final PersonData personData) throws RemoteException {
        final var remotePerson = getRemotePerson(personData);
        return remotePerson == null ? null : new LocalPerson(remotePerson);
    }

    @Override
    public RemotePerson getRemotePerson(final PersonData personData) throws RemoteException {
        return persons.get(personData);
    }

    @Override
    public RemotePerson createPerson(final PersonData personData) throws RemoteException {
        System.out.println("Creating person " + personData);
        final RemotePersonImpl person = new RemotePersonImpl(personData, port);
        if (persons.putIfAbsent(personData, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(personData);
        }
    }
}