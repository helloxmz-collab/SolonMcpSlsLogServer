package com.anker.sls.service.impl;

import com.anker.sls.service.ResourceService;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl implements ResourceService {
    @Override
    public String getAppVersion() {
        return "v3.2.0";
    }

    @Override
    public String getEmail(String userId) {
        return userId + "@example.com";
    }
} 