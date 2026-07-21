package ru.volkfm.chattskiy.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import ru.volkfm.chattskiy.model.data.postgres.User;
import ru.volkfm.chattskiy.repository.postgres.UserRepository;

import java.util.Collections;

@RequiredArgsConstructor
public class R2dbUserDetailsService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;

    @Getter
    public static class DbUser extends org.springframework.security.core.userdetails.User {
        private final User dbData;

        public DbUser(User user) {
            super(user.getUsername(), user.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())));

            this.dbData = user;
        }
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(DbUser::new);
    }

}
