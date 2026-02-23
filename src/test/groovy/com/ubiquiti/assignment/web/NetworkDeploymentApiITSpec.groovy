package com.ubiquiti.assignment.web

import com.ubiquiti.assignment.Application
import com.ubiquiti.assignment.service.NetworkDeploymentManager
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import spock.lang.Specification

@SpringBootTest(
        classes = Application,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class NetworkDeploymentApiITSpec extends Specification {

    static final String BASE_PATH = "/api/v1/network-deployment"

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    NetworkDeploymentManager manager

    def cleanup() {
        manager.reset()
    }

    def "GET /devices returns empty list when no devices registered"() {
        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices", List)

        then:
        verifyAll(response) {
            statusCode == HttpStatus.OK
            body == []
        }
    }

    def "POST /devices registers a device and GET /devices returns it"() {
        when:
        def postResponse = postDevice("GATEWAY", "AA:BB:CC:DD:EE:01", null)

        then:
        postResponse.statusCode == HttpStatus.OK

        when:
        def getResponse = restTemplate.getForEntity("$BASE_PATH/devices", List)

        then:
        verifyAll(getResponse) {
            statusCode == HttpStatus.OK
            body.size() == 1
            body[0].macAddress == "AA:BB:CC:DD:EE:01"
            body[0].deviceType == "GATEWAY"
        }
    }

    def "GET /devices/{macAddress} returns a single device"() {
        given:
        postDevice("SWITCH", "AA:BB:CC:DD:EE:02", null)

        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/AA:BB:CC:DD:EE:02", Map)

        then:
        verifyAll(response) {
            statusCode == HttpStatus.OK
            body.macAddress == "AA:BB:CC:DD:EE:02"
            body.deviceType == "SWITCH"
        }
    }

    def "GET /devices/{macAddress} returns 404 for unknown device"() {
        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/FF:FF:FF:FF:FF:FF", Map)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "POST /devices returns 400 for invalid MAC address"() {
        when:
        def response = postDevice("GATEWAY", "invalid", null)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "POST /devices returns 400 for invalid uplink MAC address"() {
        when:
        def response = postDevice("GATEWAY", "AA:BB:CC:DD:EE:03", "invalid")

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "POST /devices returns 400 when registering duplicate device"() {
        given:
        postDevice("GATEWAY", "AA:BB:CC:DD:EE:04", null)

        when:
        def response = postDevice("GATEWAY", "AA:BB:CC:DD:EE:04", null)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "POST /devices with uplink registers a connected device"() {
        given:
        postDevice("GATEWAY", "AA:BB:CC:DD:EE:05", null)

        when:
        def response = postDevice("SWITCH", "AA:BB:CC:DD:EE:06", "AA:BB:CC:DD:EE:05")

        then:
        response.statusCode == HttpStatus.OK
    }

    def "GET /devices/tree/{macAddress} returns a subtree rooted at given device"() {
        given:
        postDevice("GATEWAY", "AA:BB:CC:DD:EE:09", null)
        postDevice("SWITCH", "AA:BB:CC:DD:EE:0A", "AA:BB:CC:DD:EE:09")
        postDevice("SWITCH", "AA:BB:CC:DD:EE:0B", "AA:BB:CC:DD:EE:09")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:0C", "AA:BB:CC:DD:EE:0A")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:0D", "AA:BB:CC:DD:EE:0A")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:0E", "AA:BB:CC:DD:EE:0B")

        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/tree/AA:BB:CC:DD:EE:09", String)

        then:
        response.statusCode == HttpStatus.OK
        parseJson(response.body) == [
                macAddress     : "AA:BB:CC:DD:EE:09",
                deviceType     : "GATEWAY",
                downlinkDevices: [
                        [
                                macAddress     : "AA:BB:CC:DD:EE:0A",
                                deviceType     : "SWITCH",
                                downlinkDevices: [
                                        [
                                                macAddress     : "AA:BB:CC:DD:EE:0C",
                                                deviceType     : "ACCESS_POINT",
                                                downlinkDevices: []
                                        ],
                                        [
                                                macAddress     : "AA:BB:CC:DD:EE:0D",
                                                deviceType     : "ACCESS_POINT",
                                                downlinkDevices: []
                                        ]
                                ]
                        ],
                        [
                                macAddress     : "AA:BB:CC:DD:EE:0B",
                                deviceType     : "SWITCH",
                                downlinkDevices: [
                                        [
                                                macAddress     : "AA:BB:CC:DD:EE:0E",
                                                deviceType     : "ACCESS_POINT",
                                                downlinkDevices: []
                                        ]
                                ]
                        ]
                ]
        ]
    }

    def "GET /devices/tree/{macAddress} returns a mid-level subtree"() {
        given:
        postDevice("GATEWAY", "AA:BB:CC:DD:EE:10", null)
        postDevice("SWITCH", "AA:BB:CC:DD:EE:11", "AA:BB:CC:DD:EE:10")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:12", "AA:BB:CC:DD:EE:11")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:13", "AA:BB:CC:DD:EE:11")

        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/tree/AA:BB:CC:DD:EE:11", String)

        then:
        response.statusCode == HttpStatus.OK
        parseJson(response.body) == [
                macAddress     : "AA:BB:CC:DD:EE:11",
                deviceType     : "SWITCH",
                downlinkDevices: [
                        [
                                macAddress     : "AA:BB:CC:DD:EE:12",
                                deviceType     : "ACCESS_POINT",
                                downlinkDevices: []
                        ],
                        [
                                macAddress     : "AA:BB:CC:DD:EE:13",
                                deviceType     : "ACCESS_POINT",
                                downlinkDevices: []
                        ]
                ]
        ]
    }

    def "GET /devices/tree/{macAddress} returns a leaf node with empty downlink devices"() {
        given:
        postDevice("GATEWAY", "AA:BB:CC:DD:EE:14", null)
        postDevice("SWITCH", "AA:BB:CC:DD:EE:15", "AA:BB:CC:DD:EE:14")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:16", "AA:BB:CC:DD:EE:15")

        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/tree/AA:BB:CC:DD:EE:16", String)

        then:
        response.statusCode == HttpStatus.OK
        parseJson(response.body) == [
                macAddress     : "AA:BB:CC:DD:EE:16",
                deviceType     : "ACCESS_POINT",
                downlinkDevices: []
        ]
    }

    def "GET /devices/tree returns the full device tree"() {
        given:
        postDevice("GATEWAY", "AA:BB:CC:DD:EE:07", null)
        postDevice("SWITCH", "AA:BB:CC:DD:EE:08", "AA:BB:CC:DD:EE:07")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:17", "AA:BB:CC:DD:EE:08")
        postDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:18", "AA:BB:CC:DD:EE:08")

        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/tree/AA:BB:CC:DD:EE:07", String)

        then:
        response.statusCode == HttpStatus.OK
        parseJson(response.body) == [
                macAddress     : "AA:BB:CC:DD:EE:07",
                deviceType     : "GATEWAY",
                downlinkDevices: [
                        [
                                macAddress     : "AA:BB:CC:DD:EE:08",
                                deviceType     : "SWITCH",
                                downlinkDevices: [
                                        [
                                                macAddress     : "AA:BB:CC:DD:EE:17",
                                                deviceType     : "ACCESS_POINT",
                                                downlinkDevices: []
                                        ],
                                        [
                                                macAddress     : "AA:BB:CC:DD:EE:18",
                                                deviceType     : "ACCESS_POINT",
                                                downlinkDevices: []
                                        ]
                                ]
                        ]
                ]
        ]
    }

    def "GET /devices/{macAddress} returns 400 for invalid MAC address"() {
        when:
        def response = restTemplate.getForEntity("$BASE_PATH/devices/not-valid", Map)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    private postDevice(String deviceType, String macAddress, String uplinkMacAddress) {
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def body = [
                deviceType      : deviceType,
                macAddress      : macAddress,
                uplinkMacAddress: uplinkMacAddress
        ]

        restTemplate.postForEntity("$BASE_PATH/devices", new HttpEntity<>(body, headers), String)
    }

    private static parseJson(String json) {
        return sortTree(new JsonSlurper().parseText(json))
    }

    private static sortTree(node) {
        if (node instanceof Map && node.downlinkDevices != null) {
            node.downlinkDevices = node.downlinkDevices
                    .collect { sortTree(it) }
                    .sort { it.macAddress }
        }
        return node
    }
}
