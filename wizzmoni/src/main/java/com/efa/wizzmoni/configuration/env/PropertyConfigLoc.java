package com.efa.wizzmoni.configuration.env;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Profile("loc")
@Configuration
    @PropertySource("file:C:/Users/thomas.paulson/Documents/Wizzmoni EFA/Properties/efa-loc.properties")
public class PropertyConfigLoc {
}