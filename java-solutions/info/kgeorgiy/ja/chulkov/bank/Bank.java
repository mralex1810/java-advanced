package info.kgeorgiy.ja.chulkov.bank;


import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.Person;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePerson;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    LocalPerson getLocalPerson(Person.PersonData personData) throws RemoteException;

    RemotePerson getRemotePerson(Person.PersonData personData) throws RemoteException;
    RemotePerson createPerson(Person.PersonData personData) throws RemoteException;

}
