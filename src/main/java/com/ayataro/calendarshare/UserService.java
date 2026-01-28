package com.ayataro.calendarshare;

import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User currentUser(Principal p) {
        return repo.findByEmail(p.getName()).orElseThrow();
    }

    public User findByEmail(String email) {
        return repo.findByEmail(email).orElseThrow();
    }
}