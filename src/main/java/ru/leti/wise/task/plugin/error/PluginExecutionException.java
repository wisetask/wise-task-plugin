package ru.leti.wise.task.plugin.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PluginExecutionException extends RuntimeException{
    private final String pluginLogs;
}
