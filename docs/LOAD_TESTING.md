# Chattskiy Load Testing

## Test

The main scenario models realistic private chats:

- one VU = one unique user;
- at most one WebSocket session per VU;
- each user belongs to exactly one private chat;
- each private chat contains two users.

Therefore 3,000 VUs represent approximately 3,000 users, 3,000 WebSocket sessions, and 1,500 independent private chats.

## Hardware

Application host:

```text
CPU: Ryzen 5 3600, 6 cores / 12 threads
RAM: 16 GB DDR4
OS: Windows 11 Pro
Runtime: Docker Desktop / WSL
```

Approximately 12 GB RAM and 12 logical processors were allocated to Docker/WSL.

The load generator ran on a separate laptop.

## Infrastructure

```text
Traefik
ChatNode #1
ChatNode #2
Redis
Cassandra
Prometheus
Grafana
```

## Result

| Metric | Result |
|---|---:|
| Application nodes | 2 |
| Concurrent users | 3,000 |
| WebSocket sessions | 3,000 |
| Private chats | ~1,500 |
| Client messages/sec | ~2,000 |
| WebSocket messages/sec | ~4,000 |
| Incoming p95 | ~10 ms |
| Publishing p95 | ~24 ms |
| Outside p95 | ~1 ms |
| k6 checks | 1,797,091 |

## Purpose

The load tests measure:

- WebSocket connection scalability;
- event throughput;
- reactive pipeline latency;
- inter-node propagation;
- session registry behavior;
- resource consumption;
- capacity and bottlenecks.

### Application metrics

Custom Micrometer histogram timers measure:

**Incoming:** from event delegation to completion of the delegated handler pipeline.

**Publishing:** from the start of `publish()` (a.k.a. event propagation) to completion of its publishing pipeline.

**Outside:** from external event-listener processing to emission into the local session sink.

Timers are tagged by event type.

The approximately 2:1 receive/send ratio is expected because a client-originated message produces an acknowledgement plus delivery to the other participant.

## Limitations

- Application infrastructure runs on one physical machine;
- Windows and Docker add overhead compared with dedicated Linux;
- Cassandra (especially Cassandra), Redis, monitoring, and application nodes compete for host resources;
