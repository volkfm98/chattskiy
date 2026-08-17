package ru.volkfm.chattskiy.repository.postgres;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.volkfm.chattskiy.model.data.postgres.Chat;

import java.util.UUID;

@Repository
public interface ChatRepository extends R2dbcRepository<Chat, UUID> {
    @Query("SELECT user_id FROM chat_user WHERE chat_id = $1")
    Flux<UUID> getUsers(UUID id);
}
