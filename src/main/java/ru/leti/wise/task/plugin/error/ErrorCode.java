package ru.leti.wise.task.plugin.error;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PROFILE_NOT_FOUND,
    INVALID_PASSWORD,
    PLUGIN_NOT_FOUND,
    TOO_LONG_PLUGIN_EXECUTION,
    PLUGIN_INTERNAL_EXCEPTION,
    INVALID_PLUGIN_IMPLEMENTATION_TYPE,
    ;
}
