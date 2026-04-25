package com.appreactive.services;

import com.common.models.user.UserModel;
import reactor.core.publisher.Flux;

public interface UserReactiveService {
    Flux<UserModel> getFluxTest();
}
