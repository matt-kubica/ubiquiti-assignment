package com.ubiquiti.assignment.web

import spock.lang.Specification

class MacAddressValidatorSpec extends Specification {

    def sut = new MacAddressValidator()

    def "should accept valid MAC address '#macAddress'"() {
        when:
        sut.validate(macAddress)

        then:
        noExceptionThrown()

        where:
        macAddress << [
                "AA:BB:CC:DD:EE:FF",
                "aa:bb:cc:dd:ee:ff",
                "aA:bB:cC:dD:eE:fF",
                "00:11:22:33:44:55",
                "AA-BB-CC-DD-EE-FF",
                "aa-bb-cc-dd-ee-ff",
                "00-11-22-33-44-55",
        ]
    }

    def "should reject invalid MAC address '#macAddress'"() {
        when:
        sut.validate(macAddress)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("Invalid MAC address")

        where:
        macAddress << [
                null,
                "",
                "not-a-mac",
                "AA:BB:CC:DD:EE",
                "AA:BB:CC:DD:EE:FF:00",
                "GG:HH:II:JJ:KK:LL",
                "AABBCCDDEEFF",
                "AA BB CC DD EE FF",
                "AA:BB:CC:DD:EE:F",
        ]
    }
}