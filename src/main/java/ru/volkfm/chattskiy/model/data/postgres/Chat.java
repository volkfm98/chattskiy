package ru.volkfm.chattskiy.model.data.postgres;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
public class Chat {
    @Id
    private UUID id;
    private String name;
}
