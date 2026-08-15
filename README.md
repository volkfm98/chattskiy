# What is Chattskiy?

Chattskiy is a distributed reactive chat backend built with Java and Spring WebFlux.

The project explores the engineering problems behind a horizontally scalable, high-loaded chat backend:
handling big numbers of simultaneous connections, reactive event processing, distributed session
management, inter-node event propagation, persistence, observability, and load testing.

# Why is Chattskiy?

Initially it was just a pet project to get familiarize with modern technologies, approaches and challenges in high-loaded distributed systems.
Now it's treated as my portfolio project.

Don't expect it to be in anywhere near production-ready state. It's a deliberate choice to make only the most intricate and challenging parts of backend of the application.
It lacks any API and services dedicated to register and manage user, no chat management API and no any client implementation.
These components are considered simple to implement yet quite time-consuming.

# The goal

So, our task is to make some backend of a [Telegram](https://telegram.org/)-like chat application.

Such app should be:
* usable across multiple regions
* efficiently handle variable amounts of simultaneous user connections with quite big span
* and yet still serve users with acceptable latency.

In more engineering terms it should be:
* distributed
* horizontally scalable
* responsive

# Architecture

> [!Note]
> This chapter contains only a big picture of architecture description. For more detailed architecture description and reasoning behind it see [ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Diagrams

### One giant diagram

```mermaid
graph LR;
    subgraph Region1
        subgraph R1Clients [Clients]
            R1Client1 & R1Client2
        end
        
        R1Clients --> R1LoadBalancer[Load balancer];
        subgraph R1DataCenter [Regional data center]
            subgraph R1RegionalWorkers [Regional workers]
                R1ChatNode1[ChatNode1] & R1ChatNodeN[ChatNode2];     
            end
            R1LoadBalancer --> R1RegionalWorkers;
            
            subgraph R1FederalWorkers [Federal workers]
                R1GatewayNode1[GatewayNode1];
                R1GatewayNode2[GatewayNode2];
            end
            
            R1RegionalWorkers & R1FederalWorkers <--> R1DataBases;

            subgraph R1DataBases [Regional databases]
                direction TD
                R1Redis@{shape: db, label: Redis};
                R1Cassandra@{shape: db, label: Cassandra};
                R1Postgres@{shape: db, label: Postgres};
            end
        end 
    end

    subgraph Region2
        R2Client1 & R2ClientN --> R2LoadBalancer[Load balancer];
        subgraph R2DataCenter [Regional data center]
            subgraph R2RegionalWorkers [Regional workers]
                R2ChatNode1[ChatNode1] & R2ChatNodeN[ChatNode2];
            end
            R2LoadBalancer --> R2RegionalWorkers;

            subgraph R2FederalWorkers [Federal workers]
                R2GatewayNode1[GatewayNode1];
                R2GatewayNode2[GatewayNode2];
            end

            R2RegionalWorkers & R2FederalWorkers <--> R2DataBases;

            subgraph R2DataBases [Regional databases]
                direction TD
                R2Redis@{shape: db, label: Redis};
                R2Cassandra@{shape: db, label: Cassandra};
                R2Postgres@{shape: db, label: Postgres};
            end
        end
    end
    
    R1FederalWorkers <-----> R2FederalWorkers
    R1FederalWorkers & R2FederalWorkers <--> GlobalRoutingDB;
```

### Regional Layer

```mermaid
graph LR
    subgraph R1Clients [Clients]
        direction TD
        R1Client1 & R1Client2
        R1Client1 ~~~ R1Client2
    end
    
    R1Clients --> R1LoadBalancer[Load balancer];
    subgraph R1DataCenter [Regional data center]
        subgraph R1RegionalWorkers [Regional workers]
            R1ChatNode1[ChatNode1] & R1ChatNodeN[ChatNode2];     
        end
        R1LoadBalancer --> R1RegionalWorkers;
        
        subgraph R1FederalWorkers [Federal workers]
            R1GatewayNode1[GatewayNode1];
            R1GatewayNode2[GatewayNode2];
        end
        
        R1RegionalWorkers & R1FederalWorkers <--> R1DataBases;

        subgraph R1DataBases [Regional databases]
            direction TD
            R1Redis@{shape: db, label: Redis};
            R1Cassandra@{shape: db, label: Cassandra};
            R1Postgres@{shape: db, label: Postgres};
            
            R1Redis ~~~ R1Cassandra ~~~ R1Postgres;
        end
    end 
```

As you can see, on a scale of one region we have regional users connected to some load balancer (For now Traefik is used for simplicity).

Load balancer then distributes users across multiple ChatNodes. 

ChatNodes are responsible for handling users websocket sessions and propagation of incoming (user sent) events across other workers (regional and federal).

GatewayNodes are responsible for handling cross-regional event handling and propagation (e.g. propagate MessageEvent to users from other regions).

Redis pub/sub is used for worker communication inside a region.

### Federal layer
> [!Important]
> Currently not implemented, but described and reasoned quite thoroughly in [ARCHITECTURE.md](docs/ARCHITECTURE.md#federal-layer)

```mermaid
graph LR;
    R1FederalWorkers <-----> R2FederalWorkers
    R1FederalWorkers & R2FederalWorkers <--> GlobalRoutingDB;
    
    subgraph Region1
        subgraph R1DataCenter [Regional data center]
            subgraph R1FederalWorkers [Federal workers]
                R1GatewayNode1[GatewayNode1];
                R1GatewayNode2[GatewayNode2];
            end
            
            R1FederalWorkers <--> R1DataBases;

            subgraph R1DataBases [Regional databases]
                direction TD
                R1Redis@{shape: db, label: Redis};
                R1Cassandra@{shape: db, label: Cassandra};
                R1Postgres@{shape: db, label: Postgres};
                
                R1Redis ~~~ R1Cassandra ~~~ R1Postgres
            end
        end 
    end

    subgraph Region2
        subgraph R2DataCenter [Regional data center]
            subgraph R2FederalWorkers [Federal workers]
                R2GatewayNode1[GatewayNode1];
                R2GatewayNode2[GatewayNode2];
            end

            R2FederalWorkers <--> R2DataBases;

            subgraph R2DataBases [Regional databases]
                direction TD
                R2Redis@{shape: db, label: Redis};
                R2Cassandra@{shape: db, label: Cassandra};
                R2Postgres@{shape: db, label: Postgres};

                R2Redis ~~~ R2Cassandra ~~~ R2Postgres
            end
        end
    end
```

As mentioned earlier, each user has its home region assigned and GatewayNodes are responsible for cross-regional communications. 
To locate home region of foreign users we have to introduce some centralized storage with routing `user -> home_region` mappings.

GatewayNode then can ask home region of user for required data (like regions where this user has active sessions).

Then, knowing all the necessary routing data, GatewayNode can communicate only with interested regions, and to be more specific, to their GatewayNodes.

## Current benchmark

**3,000 concurrent WebSocket users across two application nodes, ~2,000 client-originated messages/sec and ~4,000 WebSocket messages/sec**, on a Ryzen 5 3600 / 16 GB RAM development machine.

## Architecture

```text
Client
  │ WebSocket
  ▼
Traefik
  ├───────────────┐
  ▼               ▼
Chattskiy #1   Chattskiy #2
  │               │
  └────── Redis ──┘
          │
       Cassandra
```

Each application node owns the actual WebSocket sessions connected to it. Redis stores the distributed session-routing information and transports events between nodes. Cassandra stores durable chat data.

## Event flow

```text
Client
  │
  ▼
ChatWsHandler
  │
  ▼
EventHandler
  │
  ▼
EventPublishingService
  │
  ├─ determine chat participants
  ├─ determine nodes with active sessions
  └─ Redis Pub/Sub
          │
          ▼
  EventListeningService
          │
          ▼
     EventListener
          │
          ▼
    local session sink
          │
          ▼
      WebSocket
```

## Session management

The local registry owns actual WebSocket session objects:

```text
userId → sessionId → LocalSession → WebSocket sink
```

A user may have multiple sessions.

The global Redis-backed registry answers which application nodes currently have sessions for a user. It uses TTLs so stale state eventually disappears if a node or session stops renewing it.

## Redis

Redis has two separate responsibilities:

- ephemeral global session registry / coordination;
- Pub/Sub event propagation between application nodes.

Redis Pub/Sub is intentionally transient. It is not a durable event log and does not provide replay.

## Reactive stack

Chattskiy uses Spring WebFlux and Project Reactor for WebSocket I/O, Redis access, and event-processing pipelines.

Reactor `Sinks` bridge the detached event-listening pipeline with individual WebSocket sending pipelines.

## Persistence

Cassandra stores durable chat-related data. Transient session-routing state and inter-node event propagation remain in Redis.

## Observability

Spring Boot Actuator and Micrometer expose metrics. Prometheus collects them and Grafana visualizes them.

Custom histogram timers measure:

- **incoming**: event delegation until handler completion;
- **publishing**: complete event publishing pipeline;
- **outside**: external event processing until emission to a local session sink.

The timers are tagged by event type.

Structured logging uses Spring's ECS logging support.

## Testing

The project contains:

- plain JUnit unit tests;
- Mockito-based unit tests;
- Redis integration tests;
- Testcontainers-based integration testing;
- concurrency-sensitive session TTL tests;
- k6 WebSocket load tests.

## Load testing

The main scenario models realistic private chats:

- one VU = one unique user;
- at most one WebSocket session per VU;
- each user belongs to exactly one private chat;
- each private chat contains two users.

Therefore 3,000 VUs represent approximately 3,000 users, 3,000 WebSocket sessions, and 1,500 independent private chats.

### Two-node result

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
| Failed checks | 1 |

The benchmark host was a Ryzen 5 3600 with 16 GB DDR4 running Windows 11 Pro. Docker/WSL was allocated approximately 12 GB RAM and 12 logical processors. The k6 load generator ran on a separate LAN machine.

### Single-node observation

A single-node run reached approximately 2,600 VUs before a session sink began reporting overflow. This is treated as a useful capacity signal rather than simply a test failure.

## Known limitations

- Redis Pub/Sub provides no durable delivery or replay.
- Session routing depends on TTL renewal.
- Basic Authentication is currently used for WebSocket authentication.
- The load test uses a single physical host for infrastructure.
- Cassandra and Redis are development-grade single-instance infrastructure in the local topology.
- Offline-message semantics and strong delivery guarantees are intentionally limited.

## Possible future work

- JWT/OAuth2 authentication;
- durable event delivery/replay;
- offline messages;
- stronger delivery acknowledgements;
- distributed tracing;
- failure-injection testing;
- Kubernetes deployment;
- production HA Redis/Cassandra;
- larger multi-node benchmarks.

## Project purpose

Chattskiy is intentionally small enough to understand end-to-end while still exposing real distributed-systems concerns.

Its main engineering areas are:

**Reactive WebSockets + distributed session state + inter-node messaging + persistence + observability + testing + performance engineering.**
