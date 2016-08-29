package com.konkerlabs.platform.registry.business.services.api;

import java.util.Map;
import java.util.Optional;

public class ServiceResponseBuilder<T> {

    private NewServiceResponse<T> response;

    private ServiceResponseBuilder(NewServiceResponse<T> response) {
        this.response = response;
    }

    public static <T> ServiceResponseBuilder<T> ok() {
        NewServiceResponse<T> response = new NewServiceResponse<T>();
        response.setStatus(NewServiceResponse.Status.OK);

        return new ServiceResponseBuilder<T>(response);
    };

    public static <T> ServiceResponseBuilder<T> error() {
        NewServiceResponse<T> response = new NewServiceResponse<T>();
        response.setStatus(NewServiceResponse.Status.ERROR);

        return new ServiceResponseBuilder<T>(response);
    };

    public ServiceResponseBuilder<T> withMessage(String code, Object... parameters) {
        this.response.getResponseMessages().put(code,
            parameters == null || parameters.length == 0 ? null : parameters
        );
        return this;
    }

    public ServiceResponseBuilder<T> withMessages(Map<String, Object[]> messages) {
        this.response.getResponseMessages().putAll(messages);
        return this;
    }

    public ServiceResponseBuilder<T> withResult(T result) {
        this.response.setResult(result);
        return this;
    }

    public NewServiceResponse<T> build() {
        return this.response;
    }
}