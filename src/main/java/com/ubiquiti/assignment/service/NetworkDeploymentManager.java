package com.ubiquiti.assignment.service;

import com.ubiquiti.assignment.model.*;
import io.vavr.*;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import jakarta.annotation.*;
import lombok.*;
import lombok.Value;

import org.springframework.stereotype.*;

import java.util.*;

import static java.util.Comparator.*;

/**
 * Component that manages devices in a single network deployment.
 * Network deployment can consist of zero or more devices of different types.
 * The devices are identified using MAC addresses and are typically connected to other devices.
 *
 * This implementation assumes that devices in a deployment are forming a tree structure, thus,
 * if there's more than one device, there need to be connection between them.
 * In other words, this service cannot manage more than one network deployment tree.
 *
 * This implementation also assumes that there are no particular constraints of how the services of
 * different types are connected - for example, ACCESS_POINT can act as an uplink device for GATEWAY.
 * Essentially any type of device can act as uplink device for any other device.
 * This is intentional, as no explicit instructions have been provided on such constraints.
 *
 * This implementation uses in-memory storage, however it is possible to extract an interface
 * and provide similar implementation that would use other kind of persistence, such as relational or NoSQL database.
 */
@Component
public class NetworkDeploymentManager {

    private LinkedHashMap<String, NetworkingDeviceEntry> storage = LinkedHashMap.empty();

    /**
     * Register device to a network deployment.
     * @param deviceType - type of the networking device
     * @param macAddress - MAC address of the networking device
     * @param uplinkMacAddress - MAC address of the uplink networking device. If null, the networking device
     *                         is not connected to uplink device.
     * @throws IllegalArgumentException - if device with given MAC address already exists
     */
    public void registerDevice(DeviceType deviceType, String macAddress, @Nullable String uplinkMacAddress) {
        Objects.requireNonNull(deviceType, "Device type cannot be null");
        Objects.requireNonNull(macAddress, "MAC address cannot be null");

        if (storage.get(macAddress).isDefined()) {
            throw new IllegalArgumentException("Device with '%s' MAC address already exists".formatted(macAddress));
        }

        if (uplinkMacAddress == null && !storage.isEmpty()) {
            throw new IllegalArgumentException("Root device already exists, uplink MAC address is required");
        }

        if (uplinkMacAddress != null) {
            var uplinkEntryOpt = storage.get(uplinkMacAddress);
            if (uplinkEntryOpt.isEmpty()) {
                throw new IllegalArgumentException("Uplink device with '%s' MAC address does not exist".formatted(uplinkMacAddress));
            }

            var updatedUplinkEntry = uplinkEntryOpt.get().withNewChild(macAddress);
            storage = storage.put(uplinkMacAddress, updatedUplinkEntry);
        }

        storage = storage.put(macAddress, NetworkingDeviceEntry.empty(deviceType));
    }

    /**
     * Get list of all the devices from a network deployment.
     * @return Sorted list - precedence: Gateway -> Switch -> Access Point, then alphabetical order of MAC addresses.
     */
    public List<NetworkingDeviceDescriptor> findAll() {
        return storage
                .map(tup -> NetworkingDeviceDescriptor.builder()
                        .macAddress(tup._1)
                        .deviceType(tup._2.getDeviceType())
                        .build())
                .sorted(comparing(NetworkingDeviceDescriptor::getDeviceType)
                        .thenComparing(NetworkingDeviceDescriptor::getMacAddress))
                .toList();
    }

    /**
     * Retrieve particular networking device.
     * @param macAddress - MAC address of the networking device
     * @return NetworkingDeviceDescriptor
     * @throws NoSuchElementException if device with particular MAC address cannot be found.
     */
    public NetworkingDeviceDescriptor get(String macAddress) {
        Objects.requireNonNull(macAddress, "MAC address cannot be null");

        return storage.get(macAddress)
                .map(entry -> NetworkingDeviceDescriptor.builder()
                        .macAddress(macAddress)
                        .deviceType(entry.getDeviceType())
                        .build())
                .getOrElseThrow(() -> new NoSuchElementException("Device with '%s' MAC address cannot be found".formatted(macAddress)));
    }

    /**
     * Retrieve complete tree of network deployment.
     * @return NetworkingDeviceNode object representing root of a tree, having reference to subnodes.
     * @throws NoSuchElementException if network deployment does not contain at least one device.
     */
    public NetworkingDeviceNode getDeviceTree() {
        if (storage.isEmpty()) {
            throw new NoSuchElementException("Network deployment does not contain any devices");
        }

        return getDeviceSubtree(storage.head()._1);
    }

    /**
     * Retrieve subtree starting from device indicated by input MAC address.
     * @param macAddress - MAC address of the subtree's root node.
     * @return NetworkingDeviceNode object representing subtree, having reference to subnodes.
     * @throws NoSuchElementException if device described by input MAC address cannot be found
     */
    public NetworkingDeviceNode getDeviceSubtree(String macAddress) {
        Objects.requireNonNull(macAddress, "MAC address cannot be null");

        return storage.get(macAddress)
                .map(entry -> NetworkingDeviceNode.builder()
                        .macAddress(macAddress)
                        .deviceType(entry.getDeviceType())
                        .downlinkDevices(entry.getChildren().map(this::getDeviceSubtree))
                        .build())
                .getOrElseThrow(() -> new NoSuchElementException("Device with '%s' MAC address does not exist".formatted(macAddress)));
    }

    public void reset() {
        storage = LinkedHashMap.empty();
    }

    @Value
    @Builder
    private static class NetworkingDeviceEntry {

        @NonNull
        DeviceType deviceType;

        @NonNull
        Set<String> children;

        static NetworkingDeviceEntry empty(DeviceType deviceType) {
            return new NetworkingDeviceEntry(deviceType, API.Set());
        }

        NetworkingDeviceEntry withNewChild(String macAddress) {
            var newChildren = children.add(macAddress);
            return new NetworkingDeviceEntry(deviceType, newChildren);
        }
    }
}
