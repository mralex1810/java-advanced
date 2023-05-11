package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {

    /**
     * @return first name of this person
     * @throws RemoteException if something went wrong with RMI
     */
    String getFirstName() throws RemoteException;
    /**
     * @return second name of this person
     * @throws RemoteException if something went wrong with RMI
     */
    String getSecondName() throws RemoteException;
    /**
     * @return passport of this person
     * @throws RemoteException if something went wrong with RMI
     */
    String getPassport() throws RemoteException;

    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

}
