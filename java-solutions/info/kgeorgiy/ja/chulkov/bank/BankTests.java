package info.kgeorgiy.ja.chulkov.bank;

import static info.kgeorgiy.ja.chulkov.bank.Server.BANK;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import info.kgeorgiy.ja.chulkov.bank.account.NegativeAccountAmountAfterOperation;
import info.kgeorgiy.ja.chulkov.bank.person.Person;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

@FixMethodOrder()
public class BankTests {

    private static final List<PersonData> PERSON_DATA = List.of(
            new PersonData("Георгий", "Корнеев", "105590"),
            new PersonData("Георгий", "Назаров", "242216"),
            new PersonData("Донат", "Соколов", "242555"),
            new PersonData("Анастасия", "Тушканова", "284669")
    );

    private static final List<String> ACCOUNTS = List.of(
            "Математический анализ",
            "Линейная алгебра",
            "Архитектура ЭВМ",
            "Введение в программирование"
    );
    private static final Random random = new Random(42);
    private static Registry registry;
    private Bank bank;

    public static void main(final String[] args) {
        final Result result = new JUnitCore().run(BankTests.class);
        if (!result.wasSuccessful()) {
            System.exit(1);
        }
    }

    @BeforeClass
    public static void setupRegistry() throws IOException {
        registry = LocateRegistry.createRegistry(getFreePort());
    }

    private static int getFreePort() throws IOException {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            final int ans = serverSocket.getLocalPort();
            if (ans == 0) {
                throw new RuntimeException("Must have a free port");
            }
            return ans;
        }
    }

    private static String getAccountId(final PersonData personData, final String accountId) {
        return personData.passport() + ":" + accountId;
    }

    @Before
    public void setupBank() throws IOException, NotBoundException {
        final var bankPort = getFreePort();
        bank = new RemoteBank(bankPort);
        try {
            UnicastRemoteObject.exportObject(bank, bankPort);
            registry.rebind(BANK, bank);
            bank = (Bank) registry.lookup(BANK);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Test
    public void bankRegistryTest() throws NotBoundException, RemoteException {
        Assert.assertEquals(registry.lookup(BANK), bank);
    }

    @Test
    public void accountAmountTest() throws NegativeAccountAmountAfterOperation, RemoteException {
        final String kgeorgiy = "kgeorgiy";
        final var account = new AccountImpl(kgeorgiy);
        Assert.assertEquals(account.getAmount(), 0);
        Assert.assertEquals(account.getId(), kgeorgiy);
        checkSetAmount(account, 10);
        checkSetAmount(account, 1000);
        checkSetAmount(account, 100000);
        checkSetAmount(account, 552552);
        checkSetAmount(account, Integer.MAX_VALUE);
        checkSetAmount(account, 0);
        Assert.assertEquals(account.getId(), kgeorgiy);
    }

    public void checkSetAmount(final Account account, final int amount)
            throws NegativeAccountAmountAfterOperation, RemoteException {
        account.setAmount(amount);
        Assert.assertEquals(amount, account.getAmount());
    }

    @Test(expected = NegativeAccountAmountAfterOperation.class)
    public void accountAmountNonNegativeTest() throws NegativeAccountAmountAfterOperation, RemoteException {
        final String kgeorgiy = "kgeorgiy";
        final var account = new AccountImpl(kgeorgiy);
        checkSetAmount(account, -1);
        Assert.fail();
    }

    @Test
    public void newPersonTest() throws RemoteException {
        for (final var personData : PERSON_DATA) {
            final Person person = bank.createPerson(personData);
            checkPerson(person, personData);
            Assert.assertEquals(person.getAccounts(), Map.of());
        }
    }

    @Test
    public void withoutPersonTest() throws RemoteException {
        for (final var personData : PERSON_DATA.subList(0, 2)) {
            final Person person = bank.createPerson(personData);
            checkPerson(person, personData);
            Assert.assertEquals(person.getAccounts(), Map.of());
        }
        for (final var personData : PERSON_DATA.subList(2, 4)) {
            Assert.assertNull(bank.getRemotePerson(personData));
        }
        for (final var personData : PERSON_DATA.subList(2, 4)) {
            Assert.assertNull(bank.getLocalPerson(personData));
        }
    }

    @Test
    public void localPersonGetTest() throws RemoteException {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            final Person person = bank.getLocalPerson(personData);
            checkPerson(person, personData);
            Assert.assertEquals(person.getAccounts(), Map.of());
        }
    }

    @Test
    public void createAccountTest() throws RemoteException {
        for (final var personData : PERSON_DATA) {
            for (final var accountId : ACCOUNTS) {
                final Person person = bank.createPerson(personData);
                final var account = person.createAccount(accountId);
                Assert.assertEquals(account.getId(), getAccountId(personData, accountId));
                Assert.assertEquals(account.getAmount(), 0);
            }
        }
        for (final var personData : PERSON_DATA) {
            final Person person = bank.getRemotePerson(personData);
            Assert.assertEquals(person.getAccounts().keySet(),
                    ACCOUNTS.stream().map(it -> getAccountId(personData, it)).collect(Collectors.toSet()));
        }
    }

    @Test
    public void randomAmountSetTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            for (final var accountId : ACCOUNTS) {
                final Person person = bank.createPerson(personData);
                final var account = person.createAccount(accountId);
                checkSetAmount(account, random.nextInt(0, Integer.MAX_VALUE));
                checkSetAmount(account, random.nextInt(0, Integer.MAX_VALUE));
            }
        }
    }

    @Test
    public void twoLocalPersonForOneTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account = bank.getLocalPerson(personData).createAccount(accountId);
                checkSetAmount(account, random.nextInt(1, Integer.MAX_VALUE));
                checkSetAmount(account, random.nextInt(1, Integer.MAX_VALUE));
                Assert.assertNull(bank.getLocalPerson(personData).getAccount(accountId));
            }
            for (final var accountId : ACCOUNTS) {
                Assert.assertNull(bank.getLocalPerson(personData).getAccount(accountId));
            }
        }
    }

    @Test
    public void twoRemotePersonForOneTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account1 = bank.getRemotePerson(personData).createAccount(accountId);
                final int set = random.nextInt(0, Integer.MAX_VALUE);
                checkSetAmount(account1, set);
                final var account2 = bank.getRemotePerson(personData).getAccount(accountId);
                Assert.assertNotNull(account2);
                Assert.assertEquals(account2.getAmount(), set);
            }
            for (final var accountId : ACCOUNTS) {
                Assert.assertNotNull(bank.getRemotePerson(personData).getAccount(accountId));
            }
        }
    }

    @Test
    public void RemoteAndLocalPersonsForOneTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account1 = bank.getRemotePerson(personData).createAccount(accountId);
                final int set = random.nextInt(0, Integer.MAX_VALUE);
                checkSetAmount(account1, set);
                final var account2 = bank.getLocalPerson(personData).getAccount(accountId);
                Assert.assertNotNull(account2);
                Assert.assertEquals(account2.getAmount(), set);
            }
            for (final var accountId : ACCOUNTS) {
                Assert.assertNotNull(bank.getRemotePerson(personData).getAccount(accountId));
            }
        }
    }

    @Test
    public void LocalAndRemotePersonsForOneTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account1 = bank.getLocalPerson(personData).createAccount(accountId);
                final int set = random.nextInt(0, Integer.MAX_VALUE);
                checkSetAmount(account1, set);
                final var account2 = bank.getRemotePerson(personData).getAccount(accountId);
                Assert.assertNull(account2);
            }
        }
    }


    private void checkPerson(final Person person, final PersonData personData) throws RemoteException {
        Assert.assertEquals(person.getFirstName(), personData.firstName());
        Assert.assertEquals(person.getSecondName(), personData.secondName());
        Assert.assertEquals(person.getPassport(), personData.passport());
    }


}
