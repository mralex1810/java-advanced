package info.kgeorgiy.ja.chulkov.bank;

import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.LocalPersonImpl;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePersonImpl;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {

    private final int port;
    private final ConcurrentMap<String, RemotePersonImpl> persons = new ConcurrentHashMap<>();

    /**
     * Creates {@link RemoteBank} on specified port
     *
     * @param port for export persons
     */
    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public LocalPerson getLocalPerson(final String passport) {
        final var remotePerson = getRemotePerson(passport);
        return remotePerson == null ? null : new LocalPersonImpl(remotePerson);
    }

    @Override
    public RemotePersonImpl getRemotePerson(final String passport) {
        return persons.get(passport);
    }

    @Override
    public RemotePersonImpl createPerson(final PersonData personData) throws RemoteException {
        System.out.printf("Creating person: %s %s %s%n",
                personData.firstName(), personData.secondName(), personData.passport());
        final RemotePersonImpl person = new RemotePersonImpl(personData, port);
        if (persons.putIfAbsent(personData.passport(), person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(personData.passport());
        }
    }
}
