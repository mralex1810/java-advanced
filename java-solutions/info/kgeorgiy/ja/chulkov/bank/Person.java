package info.kgeorgiy.ja.chulkov.bank;

import java.rmi.RemoteException;

public interface Person {
    String getFirstName() throws RemoteException;
    String getSecondName() throws RemoteException;
    String getPassport() throws RemoteException;
}
