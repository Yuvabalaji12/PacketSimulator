# PacketSimulator

PacketSimulator is a Java client-server demonstration of reliable delivery
over an unreliable network. The client reads records from MySQL, converts
each record into a checksummed packet, and sends the packets to a server over
TCP. The network simulator can drop or corrupt packets, while the server
validates packet integrity and requests retransmission when packets are
missing or invalid.

## Features

- TCP communication with `Socket` and `ServerSocket`
- Java object serialization for packets and retransmission messages
- SHA-256 payload checksums
- Configurable packet loss and corruption simulation
- Sequence numbers and duplicate filtering
- Retransmission of missing or corrupted packets
- Hibernate/JPA persistence for client and server data

## Requirements

- JDK 11 or later
- Apache Maven 3.8 or later
- MySQL 8.x
- A MySQL database named `network`

The Eclipse metadata targets Java 8, but the project uses Hibernate ORM 6.5.
Use a Java runtime supported by the Hibernate version in `pom.xml`.

## Database setup

Create the database and source table, then insert at least one record:

```sql
CREATE DATABASE network;

USE network;

CREATE TABLE client_data (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    age INT NOT NULL
);

INSERT INTO client_data (name, age) VALUES
    ('Alice', 30),
    ('Bob', 25);
```

The application currently reads the total number of rows from `client_data`
and sends records by ID, starting at `1`. Ensure the IDs are continuous from
`1` through the number of records to send.

Update [`hibernate.cfg.xml`](src/main/resources/hibernate.cfg.xml) for the
local MySQL username, password, and JDBC URL. The checked-in configuration
uses:

```text
JDBC URL: jdbc:mysql://localhost:3306/network
Username: admin
```

Do not use real or shared credentials in source control. The configuration
uses `hbm2ddl.auto=update`, so Hibernate creates or updates the `server_data`
table when the server persists received records.

## Build

From the repository root:

```bash
mvn clean compile
```

The compiled classes are generated under `target/classes`.

## Run the application

Start the server before the client. The server listens on TCP port `8080` and
accepts one client connection.

### Eclipse

1. Import the project as an existing Maven project.
2. Run `org.network.server.Server` as a Java application.
3. Run `org.network.client.Client` as a second Java application.

### Command line

Generate the dependency classpath:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
```

In the first terminal, start the server:

```bash
java -cp "target/classes:$(cat cp.txt)" org.network.server.Server
```

In a second terminal, start the client:

```bash
java -cp "target/classes:$(cat cp.txt)" org.network.client.Client
```

On Windows, use `;` instead of `:` as the classpath separator and use the
appropriate command to read `cp.txt`.

## How it works

1. The client opens a Hibernate session and counts rows in the `Data` entity.
2. Each record is encoded as `id,name,age`.
3. `CheckSumUtil` calculates a SHA-256 checksum for the encoded payload.
4. The client wraps the payload in a `DataPacket` and stores a copy locally
   for possible retransmission.
5. `NetworkSimulator` transmits the packet, potentially dropping it or
   corrupting its first payload byte.
6. The server reads the packet count from the first packet and validates every
   received packet using its checksum.
7. After a 1.2-second socket timeout, the server identifies missing sequence
   numbers and sends a `RetransmissionRequest`.
8. The client resends the requested packets until the server sends an empty
   retransmission request, indicating completion.
9. The server reconstructs each record and persists it to `server_data`.

## Project structure

```text
src/main/java/org/network/
├── Entity/
│   ├── Data.java                 # Client-side JPA entity
│   └── ServerData.java           # Server-side JPA entity
├── Simulator/
│   └── NetworkSimulator.java     # Packet loss and corruption simulation
├── client/
│   └── Client.java               # Client entry point and retransmission loop
├── packet/
│   ├── DataPacket.java           # Serialized data packet
│   ├── RetransmissionRequest.java
│   ├── ServerPacket.java         # Reserved packet type
│   └── SessionMetadata.java      # Reserved metadata type
├── server/
│   └── Server.java               # Server entry point and persistence
└── util/
    ├── CheckSumUtil.java         # SHA-256 helper
    └── Initial_Connection.java   # Hibernate session creation
```

## Simulation settings

The default network behavior is configured in
[`NetworkSimulator.java`](src/main/java/org/network/Simulator/NetworkSimulator.java):

- Packet loss rate: `0.25` (25%)
- Packet corruption rate: `0.20` (20%)

Adjust these constants to make the simulation more or less disruptive.

## Known limitations

- The server accepts one client and then shuts down.
- The server uses port `8080`; the port is currently hard-coded.
- The first packet is used to determine the total packet count.
- Records are encoded as comma-separated text, so commas in names are not
  supported by the current format.
- The client expects database IDs to be sequential starting at `1`.
- `Initial_Connection` creates a new Hibernate `SessionFactory` for each
  requested session; a shared lifecycle-managed factory would be preferable
  for production.
- Java object deserialization should not be exposed to untrusted clients.
- There are currently no automated tests in the repository.

## Troubleshooting

### Connection refused

Start the server first and confirm that port `8080` is available.

### Database connection failure

Verify that MySQL is running, the `network` database exists, and the
connection values in [`hibernate.cfg.xml`](src/main/resources/hibernate.cfg.xml)
match the local MySQL account.

### The client cannot find a record

The client loads records with IDs from `1` through the row count. Check that
the `client_data` table contains continuous IDs and that the database user has
permission to read it.

### The client waits for a response

Packet loss is expected. The server waits for its timeout, reports missing
sequence numbers, and the client retransmits them. Check both consoles for
checksum, timeout, socket, or database errors.

## License

No license has been specified for this project.
