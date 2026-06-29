package ru.volkfm.chattskiy.model.data;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Set;
import java.util.UUID;

@Data
public class Chat {
    @Id
    private UUID id;
    private String name;
    private Set<UUID> users;
}
