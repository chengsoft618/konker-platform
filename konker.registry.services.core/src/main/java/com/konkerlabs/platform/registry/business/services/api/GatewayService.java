package com.konkerlabs.platform.registry.business.services.api;

import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.Gateway;
import com.konkerlabs.platform.registry.business.model.Tenant;

import java.net.URI;
import java.util.List;

public interface GatewayService {

    enum Validations {
        NAME_IN_USE("service.gateway.name.in_use"),
        GUID_NULL("service.gateway.guid.not_null"),
        GATEWAY_NOT_FOUND("service.gateway.not_found");

        private String code;

        public String getCode() {
            return code;
        }

        Validations(String code) {
            this.code = code;
        }
    }

    ServiceResponse<Gateway> save(Tenant tenant, Application application, Gateway route);
    ServiceResponse<Gateway> update(Tenant tenant, Application application, String guid, Gateway eventRoute);
    ServiceResponse<List<Gateway>> getAll(Tenant tenant, Application application);
    ServiceResponse<Gateway> getByGUID(Tenant tenant, Application application, String guid);
    ServiceResponse<Gateway> remove(Tenant tenant, Application application, String guid);

}