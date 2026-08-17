package ru.volkfm.chattskiy.model.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public class MessageEvent extends PublishableEvent {
    private String content;

    @Override
    public Event copy() {
        return this.toBuilder().build();
    }
}
