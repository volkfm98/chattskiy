package ru.volkfm.chattskiy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;
import ru.volkfm.chattskiy.model.event.MessageEvent;
import ru.volkfm.chattskiy.model.data.cassandra.Message;

@Mapper(componentModel = "spring")
public interface MessageEventToEntityMapper extends Converter<MessageEvent,Message> {
    @Override
    @Mapping(target = "key.chatId", source = "chatId")
    Message convert(MessageEvent source);
}
