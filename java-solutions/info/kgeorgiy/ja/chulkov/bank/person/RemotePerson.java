package info.kgeorgiy.ja.chulkov.bank.person;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RemotePerson extends Person, Remote {
    String getFirstName() throws RemoteException;

    String getSecondName() throws RemoteException;

    String getPassport() throws RemoteException;

    Account createAccount(String id) throws RemoteException;

    Account getAccount(String id) throws RemoteException;

    Map<String, ? extends Account> getAccounts() throws RemoteException;
}
