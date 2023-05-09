package info.kgeorgiy.ja.chulkov.bank;


import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.Person;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    LocalPerson getLocalPerson(String passport) throws RemoteException;

    Person getRemotePerson(String passport) throws RemoteException;
    Person createPerson(PersonData personData) throws RemoteException;

}
