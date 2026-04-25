package com.appreactive.repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.appreactive.entities.UserReactiveEntity;

public interface UserReactiveRepository extends ReactiveCrudRepository<UserReactiveEntity, Integer> {
}
