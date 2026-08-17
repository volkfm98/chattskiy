package ru.volkfm.chattskiy.model.data.postgres;

import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Data
public class User {
    @Id
    private UUID id;
    private String username;
    private String password;
    private String role;
}
