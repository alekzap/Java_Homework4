package org.itstep.msk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AccountTest {

    private Account _account = null;
    private static double _startBalance;
    private static double _delta;

    @org.junit.jupiter.api.BeforeAll
    static void init() {
        _startBalance = 10000d;
        _delta = 1e-9;
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        _account = new Account("testAccount", _startBalance);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        _account = null;
    }

    @ParameterizedTest
    @ValueSource(doubles = {10d, 20d, 21.7d, 100d, 1000d, 10000d, 20000d})
    void test_successful_withdrawal(double amount) throws UserDeletedException, ExceedCreditException {
        _account.withdraw(amount);
        assertEquals(_startBalance - amount, _account.getBalance(), _delta);
        assertTrue(_account.isActive());
    }

    @ParameterizedTest
    @ValueSource(doubles = {110000.1d, 200000d, 300000d})
    void test_over_credit_withdrawal(double amount) {
        assertThrows(ExceedCreditException.class, () -> _account.withdraw(amount));
        assertEquals(_startBalance, _account.getBalance());
        assertTrue(_account.isActive());
    }

    @ParameterizedTest
    @ValueSource(doubles = {20000.1d, 30000d, 40000d, 110000d})
    void test_big_amount_withdrawal_confirmation(double amount) throws UserDeletedException, ExceedCreditException {
        _account.withdraw(amount);
        assertEquals(_startBalance, _account.getBalance());
        assertEquals(amount, _account.getLastBlocked());
        _account.withdraw(amount);
        assertEquals(_startBalance - amount, _account.getBalance());
        assertEquals(0d, _account.getLastBlocked());

        assertTrue(_account.isActive());
    }

    static Stream<Arguments> cancellationData() {
        return Stream.of(
                Arguments.of(20000.1d, 50d),
                Arguments.of(30000d, 100d),
                Arguments.of(100000d, 14d)
        );
    }

    @ParameterizedTest
    @MethodSource("cancellationData")
    void test_big_amount_withdrawal_cancellation(double firstAmount, double secondAmount)
            throws UserDeletedException, ExceedCreditException {
        _account.withdraw(firstAmount);
        assertEquals(_startBalance, _account.getBalance());
        assertEquals(firstAmount, _account.getLastBlocked());

        _account.withdraw(secondAmount);
        assertEquals(_startBalance - secondAmount, _account.getBalance());
        assertEquals(0d, _account.getLastBlocked());

        _account.withdraw(firstAmount);
        assertEquals(_startBalance - secondAmount, _account.getBalance());
        assertEquals(firstAmount, _account.getLastBlocked());

        assertTrue(_account.isActive());
    }

    static Stream<Arguments> cancellationBigAmountData() {
        return Stream.of(
                Arguments.of(20000.1d, 25000d),
                Arguments.of(30000d, 90000d),
                Arguments.of(100000d, 110000d)
        );
    }

    @ParameterizedTest
    @MethodSource("cancellationBigAmountData")
    void test_big_withdrawal_cancellation_by_another_big_amount(double firstAmount, double secondAmount)
            throws UserDeletedException, ExceedCreditException {
        _account.withdraw(firstAmount);
        assertEquals(_startBalance, _account.getBalance());
        assertEquals(firstAmount, _account.getLastBlocked());

        _account.withdraw(secondAmount);
        assertEquals(_startBalance, _account.getBalance());
        assertEquals(secondAmount, _account.getLastBlocked());

        _account.withdraw(secondAmount);
        assertEquals(_startBalance - secondAmount, _account.getBalance());
        assertEquals(0d, _account.getLastBlocked());

        assertTrue(_account.isActive());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1, 10, 100, 150, 1342.12, 54368976.54})
    void test_deposit(double amount) throws UserDeletedException {
        _account.deposit(amount);
        assertEquals(_startBalance + amount, _account.getBalance());

        assertTrue(_account.isActive());
    }

    static Stream<Arguments> capitalizationData() {
        return Stream.of(
                Arguments.of(100, 101),
                Arguments.of(200, 202),
                Arguments.of(10, 10),
                Arguments.of(99, 99),
                Arguments.of(150, 151),
                Arguments.of(199, 200),
                Arguments.of(0d, 0d)
        );
    }

    @ParameterizedTest
    @MethodSource("capitalizationData")
    void test_successful_capitalization(double deposit, double expected)
            throws UserDeletedException, CapitalizationException, ExceedCreditException {
        _account.withdraw(_startBalance);

        if (deposit > 0)
            _account.deposit(deposit);
        _account.capitalize();
        assertEquals(expected, _account.getBalance());
        assertTrue(_account.isActive());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1d, 0.1d, 0.01d, 100d, 1000d})
    void test_negative_balance_capitalization_failure(double withdrawal)
            throws UserDeletedException, ExceedCreditException {
        _account.withdraw(withdrawal + _startBalance);
        assertThrows(CapitalizationException.class, () -> _account.capitalize());
        assertTrue(_account.isActive());
        assertEquals(-withdrawal, _account.getBalance(), _delta);
    }

    static Stream<Arguments> debtCollectionData() {
        return Stream.of(
                Arguments.of(100, -110),
                Arguments.of(200, -220),
                Arguments.of(10, -11),
                Arguments.of(1, -1.1),
                Arguments.of(155.11, -170.63),
                Arguments.of(99, -108.9),
                Arguments.of(150, -165)
        );
    }

    @ParameterizedTest
    @MethodSource("debtCollectionData")
    void test_successful_debt_collection(double withdrawal, double expected)
            throws UserDeletedException, ExceedCreditException, DebtException {
        _account.withdraw(withdrawal + _startBalance);
        _account.collectDebt();
        assertEquals(expected, _account.getBalance(), _delta);
        assertTrue(_account.isActive());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0d, 100d, 120d, 1000000d})
    void test_positive_balance_debt_collection_failure(double deposit)
            throws UserDeletedException, ExceedCreditException {
        _account.withdraw(_startBalance);

        if (deposit > 0d)
            _account.deposit(deposit);
        assertThrows(DebtException.class, () -> _account.collectDebt());
        assertEquals(deposit, _account.getBalance(), _delta);
    }

    void depleteCredit(Account account) throws UserDeletedException, ExceedCreditException {
        double amount = account.getBalance() + Math.abs(Account.MAX_DEBT);

        account.withdraw(amount);
        if (amount > Account.MAX_WITHDRAWAL)
            account.withdraw(amount); //confirm
    }

    void blockAccount(Account account) throws UserDeletedException, ExceedCreditException, DebtException {
        depleteCredit(account);

        for (int i = 0; i < 4; i++) {
            account.collectDebt();
            assertTrue(account.isActive());
        }

        account.collectDebt();
    }

    @Test
    void test_account_deletion()
            throws UserDeletedException, ExceedCreditException, DebtException {
        blockAccount(_account);
        assertFalse(_account.isActive());

        assertThrows(UserDeletedException.class, () -> _account.deposit(10d));
        assertThrows(UserDeletedException.class, () -> _account.withdraw(10d));
        assertThrows(UserDeletedException.class, () -> _account.capitalize());
        assertThrows(UserDeletedException.class, () -> _account.collectDebt());
        assertThrows(UserDeletedException.class, () ->
                _account.transfer(10d, new Account("recipient", _startBalance)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {10d, 20d, 21.7d, 100d, 1000d, 10000d, 20000d, 50000d})
    void test_successful_transfer(double amount)
            throws TransferException, UserDeletedException, ExceedCreditException {
        Account recipient = new Account("recipient", _startBalance);
        _account.transfer(amount, recipient);

        assertEquals(_startBalance - amount, _account.getBalance());
        assertEquals(_startBalance + amount, recipient.getBalance());
        assertTrue(_account.isActive());
    }

    @Test
    void test_null_recipient_transfer() {
        assertThrows(NullPointerException.class, () -> _account.transfer(10d, null));
        assertEquals(_startBalance, _account.getBalance());
        assertTrue(_account.isActive());
    }

    @Test
    void test_deleted_recipient_transfer() throws DebtException, UserDeletedException, ExceedCreditException {
        Account recipient = new Account("recipient", _startBalance);
        blockAccount(recipient);

        assertFalse(recipient.isActive());
        assertThrows(UserDeletedException.class, () -> _account.transfer(10d, recipient));
        assertTrue(_account.isActive());
        assertEquals(_startBalance, _account.getBalance());
    }

    @ParameterizedTest
    @ValueSource(doubles = {50000.01, 100000d, 70000d, 150000d})
    void test_too_big_transfer(double amount) {
        Account recipient = new Account("recipient", _startBalance);
        assertThrows(TransferException.class, () -> _account.transfer(amount, recipient));
        assertEquals(_startBalance, recipient.getBalance());
        assertEquals(_startBalance, _account.getBalance());
        assertTrue(_account.isActive());
        assertTrue(recipient.isActive());
    }

    static Stream<Arguments> overCreditTransferData() {
        return Stream.of(
          Arguments.of(-70000d, 50000d),
          Arguments.of(-50000.1, 50000d),
          Arguments.of(-100000d, 0.1)
        );
    }

    @ParameterizedTest
    @MethodSource("overCreditTransferData")
    void test_over_credit_transfer(double initialBalance, double amount)
            throws UserDeletedException, ExceedCreditException {
        double amountToWithdraw = _startBalance + Math.abs(initialBalance);
        _account.withdraw(amountToWithdraw);
        if (amountToWithdraw > Account.MAX_WITHDRAWAL)
            _account.withdraw(amountToWithdraw); //confirm

        Account recipient = new Account("recipient", _startBalance);
        assertThrows(ExceedCreditException.class, () -> _account.transfer(amount, recipient));
        assertEquals(initialBalance, _account.getBalance());
        assertEquals(_startBalance, recipient.getBalance());
        assertTrue(_account.isActive());
        assertTrue(recipient.isActive());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0d, -0.01, -0.1, -20d})
    void test_non_positive_arguments(double amount) {
        assertThrows(IllegalArgumentException.class, () -> _account.withdraw(amount));
        assertThrows(IllegalArgumentException.class, () -> _account.deposit(amount));
        assertThrows(IllegalArgumentException.class, () ->
                _account.transfer(amount, new Account("recipient", _startBalance)));

        assertEquals(_startBalance, _account.getBalance());
        assertTrue(_account.isActive());
    }
}