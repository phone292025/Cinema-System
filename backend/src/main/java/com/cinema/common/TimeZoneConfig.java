package com.cinema.common;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {
    @Bean
    ZoneId businessZone(@Value("${app.time-zone:UTC}") String zone) {
        return ZoneId.of(zone);
    }
}
