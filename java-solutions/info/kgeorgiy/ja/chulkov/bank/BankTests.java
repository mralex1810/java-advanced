package info.kgeorgiy.ja.chulkov.bank;

import static info.kgeorgiy.ja.chulkov.bank.Server.BANK;

import info.kgeorgiy.ja.chulkov.bank.account.Account;
import info.kgeorgiy.ja.chulkov.bank.account.AccountImpl;
import info.kgeorgiy.ja.chulkov.bank.account.NegativeAccountAmountAfterOperation;
import info.kgeorgiy.ja.chulkov.bank.person.LocalPerson;
import info.kgeorgiy.ja.chulkov.bank.person.PersonData;
import info.kgeorgiy.ja.chulkov.bank.person.RemotePerson;
import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class BankTests {

    private static final List<PersonData> PERSON_DATA = List.of(
            new PersonData("Георгий", "Корнеев", "105590"),
            new PersonData("Георгий", "Назаров", "242216"),
            new PersonData("Донат", "Соколов", "242555"),
            new PersonData("Анастасия", "Тушканова", "284669"),
            new PersonData("جورج", "كورنييف", "১০৫৫৮০"),
            new PersonData("👉👌🤘👈", "👶🧒👦🧑🧑👩‍🦲", "1⃣0⃣5⃣5⃣9⃣0⃣")
    );

    private static final List<String> ACCOUNTS = List.of(
            "Математический анализ",
            "Дискретная математика",
            "Архитектура ЭВМ",
            "Введение в программирование",
            "جافا المتقدمة",
            "😆🤣🤣🤣🔞🔞"
    );
    private static final Random random = new Random(42);
    private Bank bank;

    public static void main(final String[] args) {
        final Result result = new JUnitCore().run(BankTests.class);
        if (!result.wasSuccessful()) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    @BeforeClass
    public static void setupRegistry() throws IOException {
        final var registry = LocateRegistry.createRegistry(1099);
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
            Naming.rebind(BANK, bank);
            bank = (Bank) Naming.lookup(BANK);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
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

    private void checkSetAmount(final Account account, final int amount)
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
            final RemotePerson person = bank.createPerson(personData);
            checkPerson(person, personData);
        }
    }

    @Test
    public void withoutPersonTest() throws RemoteException {
        for (final var personData : PERSON_DATA.subList(0, PERSON_DATA.size() / 2)) {
            final RemotePerson person = bank.createPerson(personData);
            checkPerson(person, personData);
        }
        for (final var personData : PERSON_DATA.subList(PERSON_DATA.size() / 2, PERSON_DATA.size())) {
            Assert.assertNull(bank.getRemotePerson(personData.passport()));
            Assert.assertNull(bank.getLocalPerson(personData.passport()));
        }
    }

    @Test
    public void localPersonGetTest() throws RemoteException {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            final LocalPerson person = bank.getLocalPerson(personData.passport());
            checkPerson(person, personData);
        }
    }

    @Test
    public void createAccountTest() throws RemoteException {
        for (final var personData : PERSON_DATA) {
            for (final var accountId : ACCOUNTS) {
                final RemotePerson person = bank.createPerson(personData);
                final var account = person.createAccount(accountId);
                Assert.assertEquals(account.getId(), getAccountId(personData, accountId));
                Assert.assertEquals(account.getAmount(), 0);
            }
        }
        for (final var personData : PERSON_DATA) {
            final RemotePerson person = bank.getRemotePerson(personData.passport());
            for (final var accountId : ACCOUNTS) {
                Assert.assertNotNull(person.getAccount(accountId));
            }
        }
    }

    @Test
    public void randomAmountSetTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            for (final var accountId : ACCOUNTS) {
                final RemotePerson person = bank.createPerson(personData);
                final var account = person.createAccount(accountId);
                checkSetAmount(account, random.nextInt(0, Integer.MAX_VALUE));
                checkSetAmount(account, random.nextInt(0, Integer.MAX_VALUE));
            }
        }
    }

    private void twoSequentialPersonForOneTest(
            final RemoteFunction<String, RemotePerson> gen1,
            final RemoteFunction<String, RemotePerson> gen2,
            final Function<Integer, Integer> expected) throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account1 = gen1.apply(personData.passport()).createAccount(accountId);
                final int set = random.nextInt(1, Integer.MAX_VALUE);
                checkSetAmount(account1, set);
                final var account2 = gen2.apply(personData.passport()).getAccount(accountId);
                final var ans = expected.apply(set);
                if (ans != null) {
                    Assert.assertNotNull(account2);
                    Assert.assertEquals(account2.getAmount(), ans.intValue());
                } else {
                    Assert.assertNull(account2);
                }
            }
        }
    }

    private void twoParallelPersonForOneTest(
            final RemoteFunction<String, RemotePerson> gen1,
            final RemoteFunction<String, RemotePerson> gen2,
            final Function<Integer, Integer> expected) throws RemoteException, NegativeAccountAmountAfterOperation {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account1 = gen1.apply(personData.passport()).createAccount(accountId);
                final var account2 = gen2.apply(personData.passport()).getAccount(accountId);
                final int set = random.nextInt(1, Integer.MAX_VALUE);
                checkSetAmount(account1, set);
                final var ans = expected.apply(set);
                if (ans != null) {
                    Assert.assertNotNull(account2);
                    Assert.assertEquals(account2.getAmount(), ans.intValue());
                } else {
                    Assert.assertNull(account2);

                }
            }
        }
    }

    @Test
    public void twoLocalSeqTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoSequentialPersonForOneTest(bank::getLocalPerson, bank::getLocalPerson, (set) -> null);
    }

    @Test
    public void twoLocalParTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoParallelPersonForOneTest(bank::getLocalPerson, bank::getLocalPerson, (set) -> null);
    }

    @Test
    public void twoRemoteSeqTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoSequentialPersonForOneTest(bank::getRemotePerson, bank::getRemotePerson, (set) -> set);
    }

    @Test
    public void twoRemoteParTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoParallelPersonForOneTest(bank::getRemotePerson, bank::getRemotePerson, (set) -> set);
    }

    @Test
    public void RemoteAndLocalSeqTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoSequentialPersonForOneTest(bank::getRemotePerson, bank::getLocalPerson, (set) -> set);
    }

    @Test
    public void RemoteAndLocalParTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoParallelPersonForOneTest(bank::getRemotePerson, bank::getLocalPerson, (set) -> 0);
    }


    @Test
    public void LocalAndRemoteSeqTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoSequentialPersonForOneTest(bank::getLocalPerson, bank::getRemotePerson, (set) -> null);
    }

    @Test
    public void LocalAndRemoteParTest() throws RemoteException, NegativeAccountAmountAfterOperation {
        twoParallelPersonForOneTest(bank::getLocalPerson, bank::getRemotePerson, (set) -> null);
    }

    private void checkPerson(final RemotePerson person, final PersonData personData) throws RemoteException {
        Assert.assertEquals(person.getFirstName(), personData.firstName());
        Assert.assertEquals(person.getSecondName(), personData.secondName());
        Assert.assertEquals(person.getPassport(), personData.passport());
    }

    @Test
    public void twoSeqBanksTest() throws IOException, NotBoundException {
        for (final var personData : PERSON_DATA) {
            bank.createPerson(personData);
        }
        for (final var personData : PERSON_DATA) {
            Assert.assertNotNull(bank.getRemotePerson(personData.passport()));
        }
        setupBank();
        for (final var personData : PERSON_DATA) {
            Assert.assertNull(bank.getRemotePerson(personData.passport()));
            Assert.assertNull(bank.getLocalPerson(personData.passport()));
        }
    }

    @Test
    public void twoParallelBanksTest() throws IOException, NotBoundException {
        final Bank bank1 = bank;
        setupBank();
        final Bank bank2 = bank;
        for (final var personData : PERSON_DATA.subList(0, PERSON_DATA.size() / 2)) {
            checkPerson(bank1.createPerson(personData), personData);
        }
        for (final var personData : PERSON_DATA.subList(PERSON_DATA.size() / 2, PERSON_DATA.size())) {
            checkPerson(bank2.createPerson(personData), personData);
        }
        for (final var personData : PERSON_DATA.subList(0, PERSON_DATA.size() / 2)) {
            Assert.assertNotNull(bank1.getRemotePerson(personData.passport()));
            Assert.assertNull(bank2.getRemotePerson(personData.passport()));
        }
        for (final var personData : PERSON_DATA.subList(PERSON_DATA.size() / 2, PERSON_DATA.size())) {
            Assert.assertNull(bank1.getRemotePerson(personData.passport()));
            Assert.assertNotNull(bank2.getRemotePerson(personData.passport()));
        }
    }

    @Test
    public void twoParallelBanksOneAccountTest()
            throws IOException, NotBoundException, NegativeAccountAmountAfterOperation {
        final Bank bank1 = bank;
        setupBank();
        final Bank bank2 = bank;
        for (final var personData : PERSON_DATA) {
            bank1.createPerson(personData);
            bank2.createPerson(personData);
            for (final var accountId : ACCOUNTS) {
                final var account1 = bank1.getRemotePerson(personData.passport()).createAccount(accountId);
                final var account2 = bank2.getRemotePerson(personData.passport()).createAccount(accountId);
                checkSetAmount(account1, 10);
                Assert.assertEquals(account2.getAmount(), 0);
                checkSetAmount(account2, 20);
                Assert.assertEquals(account1.getAmount(), 10);
            }
        }
    }

    private void checkClientFail(final PersonData personData, final String accountId) throws RemoteException {
        Assert.assertNull(bank.getRemotePerson(personData.passport()));
    }

    private void checkClientOk(final PersonData personData, final String subAccountId, final int amount)
            throws RemoteException {
        final RemotePerson person = bank.getRemotePerson(personData.passport());
        Assert.assertNotNull(person);
        final Account account = person.getAccount(subAccountId);
        Assert.assertNotNull(account);
        Assert.assertEquals(account.getAmount(), amount);
    }

    /**
     * Однажды КТ-шники решили открыть свой международный банк "Aksёnov Financial Transatlantic Co Ltd", жили-жили себе
     * спокойно, до тех пор пока в какой-то день в офис для открытия счёта не пришёл некто كورنييف جورج الكسندروفيتش
     * <p>
     * Сначала на его счету было 0 рублей.
     * <p>
     * Перевёл со своего счёта 100 рублей самому себе
     */
    @Test
    public void test1() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        Client.main(person.firstName(), person.secondName(), person.passport(), ACCOUNTS.get(4), Integer.toString(100));
        checkClientOk(person, ACCOUNTS.get(4), 100);
    }

    /**
     * Перевёл со своего счёта 999223372036854775807 рублей самому себе
     */
    @Test
    public void test2() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        Client.main(person.firstName(), person.secondName(), person.passport(), ACCOUNTS.get(4),
                "999223372036854775807");
        checkClientFail(person, ACCOUNTS.get(4));
    }

    /**
     * Перевёл 0 рублей самому себе
     */
    @Test
    public void test3() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        Client.main(person.firstName(), person.secondName(), person.passport(), ACCOUNTS.get(4), "0");
        checkClientOk(person, ACCOUNTS.get(4), 0);
    }

    /**
     * С 1000 компьютеров переводил 0 рублей самому себе
     */
    @Test
    public void test4() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        try (final ExecutorService executorService = Executors.newCachedThreadPool()) {
            IntStream.range(0, 1000)
                    .<Runnable>mapToObj((ignore) -> () ->
                            Client.main(person.firstName(), person.secondName(), person.passport(),
                                    ACCOUNTS.get(4), "0"))
                    .forEach(executorService::submit);
        }
        checkClientOk(person, ACCOUNTS.get(4), 0);
    }

    /**
     * Перевёл NaN рублей أندريه ستانكيفيتش
     */
    @Test
    public void test5() throws RemoteException {
        final var stankevich = new PersonData("ستانكيفيتش", "أندريه", "116501");
        Client.main(stankevich.firstName(), stankevich.secondName(), stankevich.passport(),
                ACCOUNTS.get(2), Float.toString(Float.NaN));
        checkClientFail(stankevich, ACCOUNTS.get(2));
    }

    /**
     * Перевёл 100 рублей через дорогу на финский язык
     */
    @Test
    public void test6() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String account = "через дорогу";
        Client.main(person.firstName(), person.secondName(), person.passport(), account,
                NumberFormat.getNumberInstance(Locale.forLanguageTag("fi")).format(100));
        checkClientOk(person, account, 100);
    }

    /**
     * Обменял undefined рублей на десять ClassCastException
     */
    @Test
    public void testNull() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String account =  new ClassCastException().toString().repeat(10);
        Client.main(person.firstName(), person.secondName(), person.passport(),
               account,
                "undefined");
        checkClientFail(person, account);

    }

    /**
     * Отправил на сервер ёжика в стакане
     */
    @Test
    public void testMinus1() {
        Server.main("Ежик в стакане");
    }

    /**
     * null null null
     */
    @Test(expected = NullPointerException.class)
    public void testMinus2() {
        Client.main(null, null, null);
    }

    /**
     * Кинул Number("100") рублей себе на телефон
     */
    @Test
    public void test10() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String account = "телефон";
        Client.main(person.firstName(), person.secondName(), person.passport(), account,
                ((Number) 100).toString());
        checkClientOk(person, account, 100);
    }

    /**
     * Попробовал заплатить "1000" рублей за ЖКХ
     */
    @Test
    public void test11() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String account = "ЖКХ";
        Client.main(person.firstName(), person.secondName(), person.passport(), account, "\"1000\"");
        checkClientFail(person, account);
    }

    /**
     * Привязал к своему аккаунту номер телефона ""'`;,.;DROP TABLE USERS"
     */
    @Test
    public void test12() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String account = "\"'`;,.;DROP TABLE USERS";
        Client.main(person.firstName(), person.secondName(), person.passport(), account, "0");
        checkClientOk(person, account, 0);
    }

    /**
     * В поле "адрес для доставки корреспонденции" указал kgeorgiy.info
     */
    @Test
    public void test13() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String account = "kgeorgiy.info";
        Client.main(person.firstName(), person.secondName(), person.passport(), account, "0");
        checkClientOk(person, account, 0);
    }


    /**
     * В поле "Отображаемое имя" указал "() -> {console.log("Georgiy Korneev");}"
     */
    @Test
    public void test14() throws RemoteException {
        final var person = PERSON_DATA.get(4);
        final String firstName = "() -> {console.log(\"Georgiy Korneev\");}";
        Client.main(firstName, person.secondName(), person.passport(),
                ACCOUNTS.get(4), "0");
        checkClientOk(person, ACCOUNTS.get(4), 0);
    }


    /**
     * В качестве суммы автоплатежа указал SLEEPING_COMPARATOR.for(10000000000000LL, "ms")
     */
    @Test
    public void test15() throws RemoteException {
        final Comparator<Integer> SLEEP_COMPARATOR = (o1, o2) -> {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            return Integer.compare(o1, o2);
        };
        final var person = PERSON_DATA.get(4);
        Client.main(person.firstName(), person.secondName(), person.passport(),
                ACCOUNTS.get(4), SLEEP_COMPARATOR.toString());
        checkClientFail(person, ACCOUNTS.get(4));
    }

    /**
     * При помощи reflections и API Excel вызвал редактор встроеных функций и подменил РАВНОКЧЕК так, чтобы она считала
     * не эквивалентный облигации доход по казначейскому векселю, а накопленный процент по ценным бумагам, процент по
     * которым выплачивается в срок погашения, то есть НАКОПДОХОДПОГАШ.
     * <p>
     * После этого рыночная стоимость акций банка стала сначала комплексной, а потом мнимой, и во избежании
     * RuntimeException было принято решение банк принудительно закрыть.
     * <p>
     * ɿoɿɿƎʏɿomɘMᎸOƚuO.ǫᴎɒ|.ɒvɒꞁ
     * <p>
     * j̸́à̸v̴͝  a̷̎.̵̆l̵͠a̸͐n̶̑ǧ̷.̴̄O̸ ủ̶t̸̔Ǒ̵f̶͂M̶̉ě̸m̶̈o̴̍ṙ̵ y̸͋É̵r̴͌r̴̂õ̷r̶̕
     * <p>
     * 🅹
     */
    @Test(expected = RuntimeException.class)
    public void testR12() {
        throw new RuntimeException(bank.toString());
    }


    @FunctionalInterface
    private interface RemoteFunction<T, V> {

        V apply(T it) throws RemoteException;
    }


}
