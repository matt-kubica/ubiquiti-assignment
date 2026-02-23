package com.ubiquiti.assignment.service

import com.ubiquiti.assignment.model.DeviceType
import com.ubiquiti.assignment.model.NetworkingDeviceDescriptor
import io.vavr.API
import spock.lang.Specification

import static com.ubiquiti.assignment.model.DeviceType.*

class NetworkDeploymentManagerSpec extends Specification {

    def sut = new NetworkDeploymentManager()

    def "findAll returns empty list if no device has been added"() {
        expect:
        sut.findAll().isEmpty()
    }

    def "registerDevice called for the first time initializes deployment"() {
        given:
        def macAddress = "abcd"

        when:
        sut.registerDevice(GATEWAY, macAddress, null)

        then:
        sut.findAll() == API.List(descriptor(GATEWAY, macAddress))
    }

    def "device cannot be registered more than once in the deployment"() {
        given:
        def macAddress = "abcd"
        sut.registerDevice(GATEWAY, macAddress, null)

        when:
        sut.registerDevice(GATEWAY, macAddress, null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Device with '$macAddress' MAC address already exists"
    }

    def "first call to registerDevice cannot accept uplink device"() {
        given:
        def uplinkMacAddress = "efgh"

        when:
        sut.registerDevice(GATEWAY, "abcd", uplinkMacAddress)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Uplink device with '$uplinkMacAddress' MAC address does not exist"
    }

    def "device cannot reference itself as uplink device"() {
        given:
        def macAddress = "abcd"

        when:
        sut.registerDevice(GATEWAY, macAddress, macAddress)


        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Uplink device with '$macAddress' MAC address does not exist"
    }

    def "multiple devices can reference the same device as uplink device"() {
        given:
        def rootMacAddress = "abcd"
        def firstChild = "efgh"
        def secondChild = "ijkl"
        sut.registerDevice(SWITCH, rootMacAddress, null)

        when:
        sut.registerDevice(ACCESS_POINT, firstChild, rootMacAddress)
        sut.registerDevice(ACCESS_POINT, secondChild, rootMacAddress)

        then:
        def list = sut.findAll()
        verifyAll(list) {
            it.size() == 3
            it.toSet() == API.Set(
                    descriptor(SWITCH, rootMacAddress),
                    descriptor(ACCESS_POINT, firstChild),
                    descriptor(ACCESS_POINT, secondChild)
            )
        }
    }

    def "getDeviceTree represents network deployment as a tree"() {
        given:
        sut.registerDevice(GATEWAY, "1", null)

        when:
        sut.registerDevice(SWITCH, "1.1", "1")
        sut.registerDevice(SWITCH, "1.2", "1")
        sut.registerDevice(SWITCH, "1.1.1", "1.1")
        sut.registerDevice(ACCESS_POINT, "1.1.1.1", "1.1.1")
        sut.registerDevice(ACCESS_POINT, "1.1.1.2", "1.1.1")
        sut.registerDevice(ACCESS_POINT, "1.2.1", "1.2")

        then:
        var _1 = sut.getDeviceTree()
        verifyAll(_1) {
            it.deviceType == GATEWAY
            it.macAddress == "1"
            verifyAll(it.downlinkDevices) {
                it.size() == 2
                def _1_1 = it.filter {
                    it.macAddress == "1.1" && it.deviceType == SWITCH
                }.single()
                def _1_2 = it.filter {
                    it.macAddress == "1.2" && it.deviceType == SWITCH
                }.single()

                verifyAll(_1_1.downlinkDevices) {
                    it.size() == 1
                    def _1_1_1 = it.filter {
                        it.macAddress == "1.1.1" && it.deviceType == SWITCH
                    }.single()

                    verifyAll(_1_1_1.downlinkDevices) {
                        it.size() == 2
                        it.filter {
                            it.macAddress == "1.1.1.1" && it.deviceType == ACCESS_POINT
                        }.single()
                        it.filter {
                            it.macAddress == "1.1.1.2" && it.deviceType == ACCESS_POINT
                        }.single()
                    }
                }

                verifyAll(_1_2.downlinkDevices) {
                    it.size() == 1
                    it.filter {
                        it.macAddress == "1.2.1" && it.deviceType == ACCESS_POINT
                    }.single()
                }
            }
        }
    }

    def "getDeviceSubtree represents part of network deployment as a tree"() {
        given:
        sut.registerDevice(GATEWAY, "1", null)

        when:
        sut.registerDevice(SWITCH, "1.1", "1")
        sut.registerDevice(SWITCH, "1.2", "1")
        sut.registerDevice(SWITCH, "1.1.1", "1.1")
        sut.registerDevice(ACCESS_POINT, "1.1.1.1", "1.1.1")
        sut.registerDevice(ACCESS_POINT, "1.1.1.2", "1.1.1")
        sut.registerDevice(ACCESS_POINT, "1.2.1", "1.2")

        then:
        var subtree = sut.getDeviceSubtree("1.1.1")
        verifyAll(subtree) {
            it.macAddress == "1.1.1"
            it.deviceType == SWITCH

            verifyAll(it.downlinkDevices) {
                it.size() == 2
                it.filter {
                    it.macAddress == "1.1.1.1" && it.deviceType == ACCESS_POINT
                }.single()
                it.filter {
                    it.macAddress == "1.1.1.2" && it.deviceType == ACCESS_POINT
                }.single()
            }
        }
    }

    def "can represent single device deployment as a tree"() {
        given:
        sut.registerDevice(GATEWAY, "1", null)

        when:
        def tree = sut.getDeviceTree()

        then:
        verifyAll {
            tree.macAddress == "1"
            tree.deviceType == GATEWAY
            tree.downlinkDevices.isEmpty()
        }

        when: "called for a subtree"
        def subtree = sut.getDeviceSubtree("1")

        then:
        verifyAll {
            subtree.macAddress == "1"
            subtree.deviceType == GATEWAY
            subtree.downlinkDevices.isEmpty()
        }
    }

    def "cannot find subtree for non existing mac address"() {
        given:
        def notExistingMac = "not-existing"
        sut.registerDevice(GATEWAY, "1", null)

        when:
        sut.getDeviceSubtree(notExistingMac)

        then:
        def ex = thrown(NoSuchElementException)
        ex.message == "Device with '$notExistingMac' MAC address does not exist"
    }

    def "getDeviceTree fails when there are no devices in network deployment"() {
        when:
        sut.getDeviceTree()

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Network deployment does not contain any devices"
    }

    def "get retrieves particular device"() {
        given:
        sut.registerDevice(GATEWAY, "1", null)
        sut.registerDevice(SWITCH, "1.1", "1")
        sut.registerDevice(SWITCH, "1.2", "1")

        expect:
        verifyAll(sut.get("1.1")) {
            it.macAddress == "1.1"
            it.deviceType == SWITCH
        }
    }

    def "get fails if device does not exist in the deployment"() {
        given:
        def notExistingMac = "not-existing"
        sut.registerDevice(GATEWAY, "1", null)
        sut.registerDevice(SWITCH, "1.1", "1")
        sut.registerDevice(SWITCH, "1.2", "1")

        when:
        sut.get(notExistingMac)

        then:
        def ex = thrown(NoSuchElementException)
        ex.message == "Device with '$notExistingMac' MAC address cannot be found"
    }

    def "find all returns a list in predetermined order"() {
        given:
        sut.registerDevice(GATEWAY, "a", null)
        sut.registerDevice(SWITCH, "b", "a")
        sut.registerDevice(SWITCH, "d", "a")
        sut.registerDevice(ACCESS_POINT, "v", "a")
        sut.registerDevice(GATEWAY, "f", "a")
        sut.registerDevice(GATEWAY, "x", "a")
        sut.registerDevice(ACCESS_POINT, "g", "a")
        sut.registerDevice(GATEWAY, "i", "a")
        sut.registerDevice(ACCESS_POINT, "h", "a")
        sut.registerDevice(ACCESS_POINT, "y", "a")
        sut.registerDevice(ACCESS_POINT, "z", "a")
        sut.registerDevice(SWITCH, "e", "a")
        sut.registerDevice(SWITCH, "c", "a")

        expect:
        sut.findAll() == API.List(
                descriptor(GATEWAY, "a"),
                descriptor(GATEWAY, "f"),
                descriptor(GATEWAY, "i"),
                descriptor(GATEWAY, "x"),
                descriptor(SWITCH, "b"),
                descriptor(SWITCH, "c"),
                descriptor(SWITCH, "d"),
                descriptor(SWITCH, "e"),
                descriptor(ACCESS_POINT, "g"),
                descriptor(ACCESS_POINT, "h"),
                descriptor(ACCESS_POINT, "v"),
                descriptor(ACCESS_POINT, "y"),
                descriptor(ACCESS_POINT, "z"),
        )
    }

    def "cannot pass null macAddress or deviceType to registerDevice method"() {
        when:
        sut.registerDevice(SWITCH, null, null)

        then:
        def ex1 = thrown(NullPointerException)
        ex1.message == "MAC address cannot be null"

        when:
        sut.registerDevice(null, "any", null)

        then:
        def ex2 = thrown(NullPointerException)
        ex2.message == "Device type cannot be null"
    }

    def "cannot pass null macAddress to get method"() {
        when:
        sut.get(null)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "MAC address cannot be null"
    }

    def "cannot pass null macAddress to getDeviceSubtree method"() {
        when:
        sut.getDeviceSubtree(null)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "MAC address cannot be null"
    }

    private static NetworkingDeviceDescriptor descriptor(DeviceType deviceType, String macAddress) {
        return NetworkingDeviceDescriptor.builder().deviceType(deviceType).macAddress(macAddress).build()
    }


}
