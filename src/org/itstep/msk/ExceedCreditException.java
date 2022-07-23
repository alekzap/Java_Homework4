package org.itstep.msk;

public class ExceedCreditException extends AccountException {

    public ExceedCreditException(String message) {
        super(message);
    }
}
