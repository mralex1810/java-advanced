package info.kgeorgiy.ja.chulkov.bank;


import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.LocalPersonImpl;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePerson;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    /**
     * Get a {@link LocalPersonImpl} copy to some Person by passport. Next operations with this person will not affect
     * any data on server.
     *
     * @param passport to get person
     * @return LocalPerson copy with specified data or {@code null} if such person does not exist.
     * @throws RemoteException if something went wrong with RMI
     */
    LocalPerson getLocalPerson(String passport) throws RemoteException;

    /**
     * Get a remote {@link RemotePerson} reference to some Person by passport. Next operations with this person will get and
     * change data on server.
     *
     * @param passport to get person
     * @return Person reference with specified data or {@code null} if such person does not exist.
     * @throws RemoteException if something went wrong with RMI
     */
    RemotePerson getRemotePerson(String passport) throws RemoteException;

    /**
     * Creates a remote {@link RemotePerson} in this bank by user data if it does not already exist.
     *
     * @param personData firstName, secondName and passport to create person profile.
     * @return created or existing Person reference
     * @throws RemoteException if something went wrong with RMI
     */
    RemotePerson createPerson(PersonData personData) throws RemoteException;

}
