package com.efa.wizzmoni.common.constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommonVariables {

    @Value("${efa.common.path-host}")
    private String pathHost;
    @Value("${efa.upload-path}")
    private String uploadPath;

    public String getPathHost() {
        return pathHost;
    }

    public String getUploadPath() {
        return uploadPath;
    }
}