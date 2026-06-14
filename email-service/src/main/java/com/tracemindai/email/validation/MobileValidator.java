package com.tracemindai.email.validation;

public class MobileValidator {
    public static boolean isValidMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return false;
        }
        return mobileNumber.matches("^\\d+$");
    }
}
