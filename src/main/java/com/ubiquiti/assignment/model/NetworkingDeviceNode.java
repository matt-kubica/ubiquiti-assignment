package com.ubiquiti.assignment.model;

import io.vavr.*;
import io.vavr.collection.*;
import lombok.*;
import lombok.Value;

@Value
@Builder
public class NetworkingDeviceNode {

    @NonNull
    String macAddress;

    @NonNull
    DeviceType deviceType;

    @NonNull
    Set<NetworkingDeviceNode> downlinkDevices;

    public static NetworkingDeviceNode empty(String macAddress, DeviceType deviceType) {
        return new NetworkingDeviceNode(macAddress, deviceType, API.Set());
    }
}
