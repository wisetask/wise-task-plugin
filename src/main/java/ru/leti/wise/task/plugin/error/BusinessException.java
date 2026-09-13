package ru.leti.wise.task.plugin.error;

import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BusinessException extends RuntimeException {
    private final Status status;
    private final String message;
}
