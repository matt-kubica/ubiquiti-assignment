# Network Deployment Manager

## Run the service

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080/api/v1/network-deployment`.

## API endpoints

| Method | Endpoint                     | Description                         |
|--------|------------------------------|-------------------------------------|
| `POST` | `/devices`                   | Register a device                   |
| `GET`  | `/devices`                   | List all devices (sorted by type)   |
| `GET`  | `/devices/{macAddress}`      | Get device by MAC address           |
| `GET`  | `/devices/tree`              | Get full network topology           |
| `GET`  | `/devices/tree/{macAddress}` | Get topology from a specific device |

All endpoints are prefixed with `/api/v1/network-deployment`.

### Register device payload

```json
{
  "deviceType": "GATEWAY|SWITCH|ACCESS_POINT",
  "macAddress": "AA:BB:CC:DD:EE:FF",
  "uplinkMacAddress": "AA:BB:CC:DD:EE:00"
}
```

`uplinkMacAddress` is optional (null for the root device).

## Run tests

```bash
./gradlew test
```
