package org.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import org.cloudstorage.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final UserRepository userRepository;


    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/health/db")
    public String dbHealth() {
        long count = userRepository.count();
        return "DB OK - Users in database:" + count;
    }
}
