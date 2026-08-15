# Chattskiy Load Testing

## Purpose

The load tests measure:

- WebSocket connection scalability;
- event throughput;
- reactive pipeline latency;
- inter-node propagation;
- session registry behavior;
- resource consumption;
- capacity and bottlenecks.

The load generator is k6 and runs on a separate machine in the same LAN.

## Workload model

Each VU represents one distinct user.

Each user:

- authenticates using Basic Authentication;
- opens at most one WebSocket session;
- belongs to exactly one private chat;
- communicates with exactly one other user.

Thus:

```text
3,000 VUs
≈ 3,000 users
≈ 3,000 WebSocket sessions
≈ 1,500 private chats
```

Traffic is distributed across many independent conversations rather than concentrated on a single hot chat.

## Hardware

Application host:

```text
CPU: Ryzen 5 3600
CPU: 6 cores / 12 threads
RAM: 16 GB DDR4
OS: Windows 11 Pro
Runtime: Docker Desktop / WSL
```

Approximately 12 GB RAM and 12 logical processors were allocated to Docker/WSL.

The load generator ran on a separate laptop.

## Infrastructure

```text
Traefik
Chattskiy node #1
Chattskiy node #2
Redis
Cassandra
Prometheus
Grafana
```

## Metrics

### k6

The test collects:

- VU count;
- WebSocket sessions;
- messages sent;
- messages received;
- connection latency;
- network throughput;
- check failures.

### Application metrics

Custom Micrometer histogram timers measure:

**Incoming:** from event delegation to completion of the delegated handler pipeline.

**Publishing:** from the start of `publish()` to completion of its publishing pipeline.

**Outside:** from external event-listener processing to emission into the local session sink.

Timers are tagged by event type.

## Two-node result

The test reached its configured maximum of 3,000 VUs.

```text
Concurrent users:          3,000
WebSocket sessions:        3,000
Private chats:             ~1,500
Client messages/sec:       ~2,000
WebSocket messages/sec:    ~4,000
```

k6 reported:

```text
checks_total:       1,797,091
checks_succeeded:   1,797,090
checks_failed:             1
```

WebSocket totals were approximately:

```text
ws_msgs_sent:        898,181
ws_msgs_received:  1,797,090
```

The approximately 2:1 receive/send ratio is expected because a client-originated message produces an acknowledgement plus delivery to the other participant.

### Application p95

| Pipeline | p95 |
|---|---:|
| Incoming | ~10.1 ms |
| Publishing | ~24.2 ms |
| Outside | ~1 ms |

These are separate measurement boundaries and should not be summed as an end-to-end latency.

## Single-node result

A single-node test reached approximately 2,600 VUs before a session sink reported overflow and the test was stopped.

This is an important capacity signal: it indicates that, under those conditions, events were being emitted into a sink faster than the downstream session pipeline could consume them.

Further investigation should correlate this with CPU, memory, Redis, Cassandra, WebSocket send latency, and GC.

## What has actually been demonstrated

The current two-node benchmark demonstrates that Chattskiy can maintain:

- approximately 3,000 concurrent WebSocket users;
- approximately 3,000 concurrent sessions;
- approximately 1,500 independent private chats;
- approximately 2,000 client-originated messages/sec;
- approximately 4,000 WebSocket messages/sec;

while retaining low measured application pipeline latency.

The 3,000-VU value is a test ceiling, not the measured absolute maximum.

## VUs vs throughput

These are different dimensions.

For example:

```text
3,000 VUs
+
one message every 30 seconds
≈ 100 messages/sec
```

would demonstrate connection scalability but not high event throughput.

A useful benchmark therefore varies both connection count and message rate.

## Recommended follow-up tests

### 1. Connection scalability

Increase VUs while keeping traffic relatively low:

```text
1,000 → 2,000 → 3,000 → 4,000 → 5,000 → ...
```

Track connection success, memory, CPU, and latency.

### 2. Throughput scalability

Keep VU count fixed and increase message rate:

```text
100 → 250 → 500 → 1,000 → 1,500 → 2,000 → ...
```

### 3. Horizontal scaling

Repeat the same workload with:

```text
1 node
2 nodes
3 nodes
...
```

Compare throughput, latency, CPU per node, Redis traffic, and Cassandra load.

## Finding the performance knee

The most useful capacity boundary is often not the crash point but the point where latency begins rising sharply.

Illustrative example:

```text
Load        Throughput     p95
1,000 VUs   700 msg/s       8 ms
2,000 VUs   1,400 msg/s    11 ms
3,000 VUs   2,000 msg/s    24 ms
4,000 VUs   2,600 msg/s    40 ms
5,000 VUs   3,100 msg/s   190 ms  ← knee
```

These values are illustrative, not measurements.

## Limitations

- application infrastructure runs on one physical machine;
- Windows/WSL/Docker adds overhead compared with dedicated Linux;
- Cassandra, Redis, monitoring, and application nodes compete for host resources;
- the scenario models two-person private chats;
- the current test stops at 3,000 VUs;
- tests are relatively short;
- no network fault injection is performed;
- no node/Redis/Cassandra failure injection is performed.

The results should therefore be treated as a repeatable local engineering benchmark, not a production capacity guarantee.

## Recommended headline

A defensible project-level statement based on the current result is:

> Load-tested a distributed reactive WebSocket messaging system with 3,000 concurrent users across two application nodes, processing approximately 2,000 client-originated messages/sec and approximately 4,000 WebSocket messages/sec with application pipeline p95 latencies below 25 ms on a Ryzen 5 3600 / 16 GB RAM development machine.
