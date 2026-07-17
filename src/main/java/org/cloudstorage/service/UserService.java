package org.cloudstorage.service;

import org.cloudstorage.model.User;

public interface UserService {
    User register(String username, String password);
}
