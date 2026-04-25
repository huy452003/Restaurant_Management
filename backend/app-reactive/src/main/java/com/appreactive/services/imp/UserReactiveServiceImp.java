package com.appreactive.services.imp;

import java.util.Collections;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.appreactive.entities.UserReactiveEntity;
import com.appreactive.repositories.UserReactiveRepository;
import com.appreactive.services.UserReactiveService;
import com.common.models.user.UserModel;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserReactiveServiceImp implements UserReactiveService {
    @Autowired
    private UserReactiveRepository userReactiveRepository;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;

    private LogContext getLogContext(String methodName, List<Integer> userIds) {
        return LogContext.builder()
            .module("app-reactive")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(userIds)
            .build();
    }

    @Override
    public Flux<UserModel> getFluxTest() {
        LogContext logContext = getLogContext("getFluxTest", Collections.emptyList());
        log.logInfo("Testing Flux in reactive module", logContext);
        return userReactiveRepository.findAll()
            .map(this::toUserModel)
            .switchIfEmpty(Mono.error(() -> new NotFoundExceptionHandle(
                "Not Found Flux Test",
                Collections.emptyList(),
                "UserReactiveServiceImp"
            )))
            .doOnError(Exception.class, e -> log.logError("Error when Flux in reactive module", e, logContext));
    }

    private UserModel toUserModel(UserReactiveEntity userEntity) {
        return modelMapper.map(userEntity, UserModel.class);
    }
}
