package info.kgeorgiy.ja.chulkov.bank;


import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePerson;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    LocalPerson getLocalPerson(PersonData personData) throws RemoteException;

    RemotePerson getRemotePerson(PersonData personData) throws RemoteException;
    RemotePerson createPerson(PersonData personData) throws RemoteException;

}
