package com.enterprise.grpc;

import com.enterprise.entity.User;
import com.enterprise.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;

@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    public UserGrpcService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void createUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        User user = userService.createUser(request.getName(), request.getEmail());
        UserResponse response = UserResponse.newBuilder()
                .setId(user.getId())
                .setName(user.getName())
                .setEmail(user.getEmail())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(UserIdRequest request, StreamObserver<UserResponse> responseObserver) {
        Optional<User> userOpt = userService.getUser(request.getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserResponse response = UserResponse.newBuilder()
                    .setId(user.getId())
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("User not found with id: " + request.getId()).asRuntimeException()
            );
        }
    }

    @Override
    public void deleteUser(UserIdRequest request, StreamObserver<DeleteResponse> responseObserver) {
        boolean deleted = userService.deleteUser(request.getId());
        if (deleted) {
            DeleteResponse response = DeleteResponse.newBuilder()
                    .setMessage("User deleted successfully")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("User not found with id: " + request.getId()).asRuntimeException()
            );
        }
    }
}
