package ru.leti.wise.task.plugin.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.grpc.server.service.GrpcService;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.PluginGrpc.*;
import ru.leti.wise.task.plugin.PluginServiceGrpc.PluginServiceImplBase;
import ru.leti.wise.task.plugin.error.BusinessException;
import ru.leti.wise.task.plugin.error.PluginExecutionException;
import ru.leti.wise.task.plugin.helper.LogInterceptor;
import ru.leti.wise.task.plugin.logic.*;

import java.util.UUID;

@Slf4j
@Observed
@GrpcService(interceptors = {LogInterceptor.class})
@RequiredArgsConstructor
public class PluginGrpcService extends PluginServiceImplBase {

    private final GetPluginOperation getPluginOperation;
    private final GetPluginsOperation getPluginsOperation;
    private final DeletePluginOperation deletePluginOperation;
    private final UpdatePluginOperation updatePluginOperation;
    private final CheckPluginSolutionOperation checkPluginSolutionOperation;
    private final CreateExternalPluginOperation createExternalPluginOperation;
    private final CheckPluginImplementationOperation checkPluginImplementationOperation;
    private final ValidatePluginOperation validatePluginOperation;


    @Override
    public void getAllPlugins(GetAllPluginRequest request, StreamObserver<GetAllPluginsResponse> responseStreamObserver){
        responseStreamObserver.onNext(getPluginsOperation.activate(request));
        responseStreamObserver.onCompleted();
    }
    @Override
    public void getPlugin(GetPluginRequest request, StreamObserver<GetPluginResponse> responseObserver) {
        responseObserver.onNext(getPluginOperation.activate(UUID.fromString(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void createPlugin(CreatePluginRequest request, StreamObserver<CreatePluginResponse> responseObserver) {
        responseObserver.onNext(createExternalPluginOperation.activate(request));
        responseObserver.onCompleted();
    }

    @Override
    public void deletePlugin(DeletePluginRequest request, StreamObserver<DeletePluginResponse> responseObserver) {
        deletePluginOperation.activate(UUID.fromString(request.getId()));
        responseObserver.onNext(DeletePluginResponse.newBuilder().setId(request.getId()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePlugin(UpdatePluginRequest request, StreamObserver<UpdatePluginResponse> responseObserver) {
        responseObserver.onNext(updatePluginOperation.activate(request));
        responseObserver.onCompleted();
    }

    @Override
    public void checkPluginSolution(CheckPluginSolutionRequest request,
                                    StreamObserver<CheckPluginSolutionResponse> responseObserver) {
        responseObserver.onNext(checkPluginSolutionOperation.activate(request));
        responseObserver.onCompleted();
    }

    @Override
    public void checkPluginImplementation(CheckPluginImplementationRequest request,
                                          StreamObserver<PluginGrpc.CheckPluginImplementationResponse> responseObserver) {
        responseObserver.onNext(checkPluginImplementationOperation.activate(request));
        responseObserver.onCompleted();
    }

    @Override
    public void validatePlugin(ValidatePluginRequest request, StreamObserver<ValidatePluginResponse> responseObserver) {
        responseObserver.onNext(validatePluginOperation.activate(UUID.fromString(request.getId())));
        responseObserver.onCompleted();
    }

    @GrpcAdvice
    @RequiredArgsConstructor
    public static class ErrorHandler {
        @GrpcExceptionHandler
        public StatusRuntimeException handleBusinessException(BusinessException e) {
            return e.getStatus().withDescription(e.getMessage()).asRuntimeException();
        }

        @GrpcExceptionHandler
        public StatusRuntimeException handeRuntimeException(PluginExecutionException e) {
            return Status.INTERNAL.withDescription("Произошла ошибка при выполнении плагина: " + e.getPluginLogs()).asRuntimeException();
        }
    }
}
