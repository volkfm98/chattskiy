package ru.volkfm.chattskiy.repository.postgres;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.model.data.postgres.User;

import java.util.UUID;

public interface UserRepository extends R2dbcRepository<User, UUID> {
    Mono<User> findByUsername(String username);
}
