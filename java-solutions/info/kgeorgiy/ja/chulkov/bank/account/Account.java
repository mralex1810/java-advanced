package info.kgeorgiy.ja.chulkov.bank.account;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {

    /**
     * Returns account identifier.
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money in the account.
     */
    long getAmount() throws RemoteException;

    /**
     * Sets amount of money in the account.
     */
    void setAmount(long amount) throws RemoteException, NegativeAccountAmountAfterOperation;
}
