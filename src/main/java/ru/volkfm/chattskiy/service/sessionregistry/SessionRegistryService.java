package ru.volkfm.chattskiy.service.sessionregistry;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionRegistryService {
    private final GlobalSessionRegistryService globalSessionRegistryService;
    private final LocalSessionRegistryService localSessionRegistryService;
}
