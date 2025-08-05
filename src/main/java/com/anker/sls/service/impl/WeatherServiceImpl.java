package com.anker.sls.service.impl;

import com.anker.sls.service.WeatherService;
import org.springframework.stereotype.Service;

@Service
public class WeatherServiceImpl implements WeatherService {
    @Override
    public String getWeather(String location) {
        return "晴，14度";
    }
} 