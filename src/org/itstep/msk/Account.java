package org.itstep.msk;

public class Account {
    public static final double MAX_DEBT = -100000.0d;
    public static final double DEBT_TO_BLOCK = -150000.0d;
    public static final double MAX_WITHDRAWAL = 20000d;
    public static final double MAX_TRANSFER = 50000d;
    public static final double CAPITALIZATION_RATE = 0.01d;
    public static final double CREDIT_RATE = 0.1d;

    private final String _accountNumber;
    private double _balance = 0.0d;
    private double _lastBlocked = 0.0d;
    private boolean _isActive = true;

    public Account(String accountNumber, double startBalance) {
        _accountNumber = accountNumber;
        _balance = startBalance;
    }

    public boolean isActive() { return _isActive; }

    public double getBalance() { return _balance; }

    public double getLastBlocked() { return _lastBlocked; }

    public void withdraw(double amount) throws ExceedCreditException, UserDeletedException {
        if (amount <= 0.)
            throw new IllegalArgumentException("The amount must be positive!");

        if (!_isActive)
            throw new UserDeletedException("User is deleted!");

        if (_balance - amount < MAX_DEBT)
            throw new ExceedCreditException("Operation is impossible as it will create more than "
                    + MAX_DEBT + " debt!");

        if (amount > MAX_WITHDRAWAL && _lastBlocked != amount) {
                _lastBlocked = amount;
                return;
        }

        _balance -= amount;
        _lastBlocked = 0.0d;
    }

    public void deposit(double amount) throws UserDeletedException {
        if (amount <= 0.)
            throw new IllegalArgumentException("The amount must be positive!");

        if (!_isActive)
            throw new UserDeletedException("User is deleted!");

        _balance += amount;
    }

    public void capitalize() throws UserDeletedException, CapitalizationException {
        if (!_isActive)
            throw new UserDeletedException("User is deleted!");

        if (_balance < 0.0d)
            throw new CapitalizationException("Impossible to capitalize account with negative ballance!");

        _balance += Math.floor(_balance * CAPITALIZATION_RATE);
    }

    //tochnost do sotyh, okruglenije v polzu banka
    public void collectDebt() throws UserDeletedException, DebtException {
        if (!_isActive)
            throw new UserDeletedException("User is deleted!");

        if (_balance >= 0.0d)
            throw new DebtException("The account has positive ballance!");

        _balance += Math.floor(_balance * CREDIT_RATE * 100d) / 100d;

        if (_balance < DEBT_TO_BLOCK)
            _isActive = false;
    }

    public void transfer(double amount, Account recipient)
            throws UserDeletedException, TransferException, ExceedCreditException {
        if (amount <= 0.)
            throw new IllegalArgumentException("The amount must be positive!");

        if (recipient == null)
            throw new NullPointerException("The recipient must not be null!");

        if (!_isActive)
            throw new UserDeletedException("User is deleted!");

        if (amount > MAX_TRANSFER)
            throw new TransferException("Impossible to transfer more than " + MAX_TRANSFER);

        if (_balance - amount < MAX_DEBT)
            throw new ExceedCreditException("Operation is impossible as it will create more than "
                    + MAX_DEBT + " debt!");

        if (!recipient._isActive)
            throw new UserDeletedException("The recipient is deleted!");

        _balance -= amount;
        recipient.deposit(amount);
    }
}
