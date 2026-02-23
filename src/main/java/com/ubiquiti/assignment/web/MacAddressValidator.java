package com.ubiquiti.assignment.web;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MacAddressValidator {

    private static final Pattern MAC_ADDRESS_PATTERN =
            Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");

    public void validate(String macAddress) {
        if (macAddress == null || !MAC_ADDRESS_PATTERN.matcher(macAddress).matches()) {
            throw new IllegalArgumentException("Invalid MAC address: '%s'".formatted(macAddress));
        }
    }
}