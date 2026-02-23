package com.ubiquiti.assignment.web;

import com.ubiquiti.assignment.model.*;
import com.ubiquiti.assignment.service.*;
import io.vavr.collection.*;
import lombok.*;
import lombok.extern.jackson.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network-deployment")
@RequiredArgsConstructor
public class NetworkDeploymentController {
    private final NetworkDeploymentManager manager;
    private final MacAddressValidator macAddressValidator;

    @PostMapping("/devices")
    public void registerDevice(@RequestBody RegisterDevicePayload payload) {
        macAddressValidator.validate(payload.macAddress);
        if (payload.uplinkMacAddress != null) {
            macAddressValidator.validate(payload.uplinkMacAddress);
        }
        manager.registerDevice(payload.deviceType, payload.macAddress, payload.uplinkMacAddress);
    }

    @GetMapping("/devices")
    public List<NetworkingDeviceDescriptor> findAll() {
        return manager.findAll();
    }

    @GetMapping("/devices/{macAddress}")
    public NetworkingDeviceDescriptor get(@PathVariable String macAddress) {
        macAddressValidator.validate(macAddress);
        return manager.get(macAddress);
    }

    @GetMapping("/devices/tree")
    public NetworkingDeviceNode getTree() {
        return manager.getDeviceTree();
    }

    @GetMapping("/devices/tree/{macAddress}")
    public NetworkingDeviceNode getSubTree(@PathVariable String macAddress) {
        macAddressValidator.validate(macAddress);
        return manager.getDeviceSubtree(macAddress);
    }

    @Builder
    @Jacksonized
    @Value
    public static class RegisterDevicePayload {
        DeviceType deviceType;
        String macAddress;
        String uplinkMacAddress;

    }
}
