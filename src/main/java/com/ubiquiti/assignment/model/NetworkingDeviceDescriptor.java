package com.ubiquiti.assignment.model;

import lombok.*;

@Value
@Builder
public class NetworkingDeviceDescriptor {

    @NonNull
    String macAddress;

    @NonNull
    DeviceType deviceType;
}
