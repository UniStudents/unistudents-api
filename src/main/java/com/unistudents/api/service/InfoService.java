package com.unistudents.api.service;

import com.unistudents.api.model.Info;

public interface InfoService {

    public Info getInfo(String username, String password);
}
