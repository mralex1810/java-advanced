package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Person extends Remote {

    String getFirstName() throws RemoteException;

    String getSecondName() throws RemoteException;

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

    Map<String, ? extends Account> getAccounts() throws RemoteException;


    record PersonData(String firstName, String secondName, String passport) implements Serializable {

    }
}
