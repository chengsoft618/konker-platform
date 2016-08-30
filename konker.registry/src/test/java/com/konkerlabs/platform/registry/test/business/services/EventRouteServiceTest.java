package com.konkerlabs.platform.registry.test.business.services;

import com.konkerlabs.platform.registry.business.model.EventRoute;
import com.konkerlabs.platform.registry.business.model.EventRoute.RouteActor;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.Transformation;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import com.konkerlabs.platform.registry.business.model.validation.CommonValidations;
import com.konkerlabs.platform.registry.business.repositories.EventRouteRepository;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.repositories.TransformationRepository;
import com.konkerlabs.platform.registry.business.services.api.NewServiceResponse;
import com.konkerlabs.platform.registry.business.services.routes.api.EventRouteService;
import com.konkerlabs.platform.registry.test.base.BusinessLayerTestSupport;
import com.konkerlabs.platform.registry.test.base.BusinessTestConfiguration;
import com.konkerlabs.platform.registry.test.base.MongoTestConfiguration;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.konkerlabs.platform.registry.business.model.EventRoute.builder;
import static com.konkerlabs.platform.registry.business.services.publishers.EventPublisherMqtt.DEVICE_MQTT_CHANNEL;
import static com.konkerlabs.platform.registry.test.base.matchers.NewServiceResponseMatchers.*;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoTestConfiguration.class, BusinessTestConfiguration.class})
public class EventRouteServiceTest extends BusinessLayerTestSupport {

    private static final String TRANSFORMATION_ID_IN_USE = "2747ec73-6910-43a1-8ddc-5a4a134ebab3";

    @Rule
    public ExpectedException thrown = none();

    @Autowired
    private EventRouteService subject;

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private EventRouteRepository eventRouteRepository;
    @Autowired
    private TransformationRepository transformationRepository;

    private EventRoute route;

    private String routeId = "71fb0d48-674b-4f64-a3e5-0256ff3a63af";
    private String existingGuid = "bd923670-d888-472a-b6d9-b20af31253da";
    private Tenant tenant;
    private Tenant emptyTenant;
    private Transformation transformation;
    private RouteActor nonExistingRouteActor;

    @Before
    public void setUp() throws Exception {
        tenant = tenantRepository.findByDomainName("konker");
        emptyTenant = tenantRepository.findByDomainName("empty");

        transformation = Transformation.builder().id(TRANSFORMATION_ID_IN_USE).build();

        route = spy(builder()
                .name("Route name")
                .description("Description")
                .incoming(RouteActor.builder().uri(
                        new DeviceURIDealer() {
                        }.toDeviceRouteURI(tenant.getDomainName(), "0000000000000004")
                ).data(new HashMap<String,String>(){{
                    put(DEVICE_MQTT_CHANNEL,"data");
                }}).build())
                .outgoing(RouteActor.builder().uri(
                        new DeviceURIDealer() {
                        }.toDeviceRouteURI(tenant.getDomainName(), "0000000000000006")
                ).data(new HashMap<String,String>(){{
                    put(DEVICE_MQTT_CHANNEL,"in");
                }}).build())
                .filteringExpression("#command.type == 'ButtonPressed'")
                .transformation(transformation)
                .active(true)
                .build());

        nonExistingRouteActor = RouteActor.builder()
                .uri(new DeviceURIDealer(){}.toDeviceRouteURI(tenant.getDomainName(), "999"))
                .data(new HashMap<>())
                .build();
    }

    /* ----------------------------- save ------------------------------ */

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json"})
    public void shouldReturnValidationMessageTenantIsNull() throws Exception {
        NewServiceResponse<EventRoute> response = subject.save(null, route);

        assertThat(response,hasErrorMessage(CommonValidations.TENANT_NULL.getCode(),null));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json"})
    public void shouldReturnValidationMessageIfRecordIsNull() throws Exception {
        NewServiceResponse<EventRoute> response = subject.save(tenant, null);

        assertThat(response,hasErrorMessage(CommonValidations.RECORD_NULL.getCode(),null));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json"})
    public void shouldReturnValidationMessageIfRecordIsInvalid() throws Exception {
        Map<String,Object[]> errorMessages = new HashMap() {{
            put("some_error",new Object[] {"some_value"});
        }};
        when(route.applyValidations()).thenReturn(Optional.of(errorMessages));

        NewServiceResponse<EventRoute> response = subject.save(tenant, route);

        assertThat(response,hasErrorMessage("some_error",new Object[] {"some_value"}));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json"})
    public void shouldReturnValidationMessageIfTenantDoesNotExist() throws Exception {
        NewServiceResponse<EventRoute> response = subject.save(Tenant.builder().id("unknown_id").name("name").build(), route);

        assertThat(response,hasErrorMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode(),null));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json","/fixtures/event-routes.json"})
    public void shouldReturnAValidationMessageIfRouteNameAlreadyExistsWithinTenant() throws Exception {
        String existingRouteName = "Device event forwarding route";

        route.setName(existingRouteName);

        NewServiceResponse<EventRoute> response = subject.save(tenant, route);
        assertThat(response,hasErrorMessage(EventRouteService.Validations.NAME_IN_USE.getCode(),null));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json","/fixtures/event-routes.json"})
    public void shouldPersistIfRouteIsValid() throws Exception {

        NewServiceResponse<EventRoute> response = subject.save(tenant, route);

        assertThat(response, isResponseOk());
        assertThat(eventRouteRepository.findByIncomingUri(route.getIncoming().getUri()), notNullValue());
        assertThat(response.getResult().getIncoming().getUri(), equalTo(route.getIncoming().getUri()));
        assertThat(response.getResult().getTransformation(),equalTo(route.getTransformation()));
        assertThat(response.getResult().getGuid(),notNullValue());
    }

    //TODO Verify this constraint for effectiveness
//    @Test
//    @UsingDataSet(locations = "/fixtures/tenants.json")
//    public void shouldReturnAValidationMessageIfIncomingAndOutgoingChannelsAreTheSame() throws Exception {
//        String channel = "channel";
//
//        route.getIncoming().getData().put("channel",channel);
//        route.getOutgoing().getData().put("channel",channel);
//
//        List<String> errorMessages = Arrays.asList(new String[] { "Incoming and outgoing device channels cannot be the same" });
//        NewServiceResponse response = subject.save(tenant,route);
//
//        assertThat(response, notNullValue());
//        assertThat(response.getStatus(), equalTo(NewServiceResponse.Status.ERROR));
//        assertThat(response.getResponseMessages(), equalTo(errorMessages));
//    }

    /* ---------------------- update ------------------------- */

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageTenantIsNullWhenUpdating() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(null,existingGuid,route);

        assertThat(response,hasErrorMessage(CommonValidations.TENANT_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageTenantDoesNotExistWhenUpdating() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(
                Tenant.builder().id("unknown_id").name("name").domainName("unknown_domain").build(),
                existingGuid,
                route);

        assertThat(response,hasErrorMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageGuidIsNullWhenUpdating() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(tenant,null,route);

        assertThat(response,hasErrorMessage(EventRouteService.Validations.GUID_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageGuidIsEmptyWhenUpdating() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(tenant,"",route);

        assertThat(response,hasErrorMessage(EventRouteService.Validations.GUID_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json", "/fixtures/event-routes.json"})
    public void shouldReturnValidationMessageGuidDoesNotExistWhenUpdating() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(tenant,"unknown_guid",route);

        assertThat(response,hasErrorMessage(EventRouteService.Validations.EVENT_ROUTE_NOT_FOUND.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageIfRecordIsNullWhenUpdating() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(tenant,existingGuid,null);

        assertThat(response,hasErrorMessage(CommonValidations.RECORD_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json", "/fixtures/event-routes.json"})
    public void shouldReturnRecordValidationMessagesIfRecordIsInvalidWhenUpdating() throws Exception {
        //Invalid state
        route.setName(null);

        NewServiceResponse<EventRoute> response = subject.update(tenant,existingGuid,route);

        assertThat(response,hasErrorMessage(EventRoute.Validations.NAME_NULL.getCode(),null));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json","/fixtures/event-routes.json"})
    public void shouldReturnAValidationMessageIfRouteNameAlreadyExistsWithinTenantWhenUpdating() throws Exception {
        String existingRouteName = "Device event forwarding route 2";

        route.setName(existingRouteName);

        NewServiceResponse<EventRoute> response = subject.update(tenant,existingGuid,route);
        assertThat(response,hasErrorMessage(EventRouteService.Validations.NAME_IN_USE.getCode(),null));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json", "/fixtures/event-routes.json"})
    public void shouldUpdateIfRouteIsValid() throws Exception {
        NewServiceResponse<EventRoute> response = subject.update(tenant,existingGuid,route);

        assertThat(response,isResponseOk());
        assertThat(response.getResult(),notNullValue());
        assertThat(response.getResult().getId(),equalTo(routeId));
    }

    /* ---------------------- getAll ------------------------- */
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json", "/fixtures/event-routes.json"})
    public void shouldReturnAllRegisteredRoutesWithinATenant() throws Exception {
        List<EventRoute> allRoutes = subject.getAll(tenant).getResult();

        assertThat(allRoutes, notNullValue());
        assertThat(allRoutes, hasSize(8));
        assertThat(allRoutes.get(0).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63af"));
        assertThat(allRoutes.get(1).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ab"));
        assertThat(allRoutes.get(2).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ac"));
        assertThat(allRoutes.get(3).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ad"));
        assertThat(allRoutes.get(4).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ae"));
        assertThat(allRoutes.get(5).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ba"));
        assertThat(allRoutes.get(6).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63bb"));
        assertThat(allRoutes.get(7).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63bc"));

        allRoutes = subject.getAll(emptyTenant).getResult();
        assertThat(allRoutes, notNullValue());
        assertThat(allRoutes, empty());
    }

    /* ---------------------- getByGUID ------------------------- */

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json", "/fixtures/event-routes.json"})
    public void shouldReturnARegisteredRouteByItsID() throws Exception {
        EventRoute route = subject.getByGUID(tenant, existingGuid).getResult();

        assertThat(route, notNullValue());
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json", "/fixtures/event-routes.json"})
    public void shouldSaveEditedRouteState() throws Exception {
        EventRoute route = subject.getByGUID(tenant, existingGuid).getResult();

        String editedName = "Edited name";
        route.setName(editedName);
        route.setActive(false);

        NewServiceResponse<EventRoute> response = subject.save(tenant, route);

        assertThat(response, isResponseOk());
        assertThat(EventRoute.class.cast(response.getResult()).getName(), equalTo(editedName));
        assertThat(EventRoute.class.cast(response.getResult()).isActive(), equalTo(false));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json", "/fixtures/event-routes.json"})
    public void shouldReturnErrorMessageIfRouteDoesNotBelongToTenantWhenFindByGUID() throws Exception {
        NewServiceResponse<EventRoute> response = subject.getByGUID(emptyTenant, existingGuid);

        assertThat(response,hasErrorMessage(EventRouteService.Validations.EVENT_ROUTE_NOT_FOUND.getCode(),null));
    }

    /* ---------------------- findByIncomingUri ------------------------- */

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/transformations.json","/fixtures/event-routes.json"})
    public void shouldReturnARegisteredRouteByItsIncomingUri() throws Exception {
        NewServiceResponse<List<EventRoute>> NewServiceResponse = subject.findByIncomingUri(route.getIncoming().getUri());
        List<EventRoute> routes = NewServiceResponse.getResult();

        assertThat(routes, notNullValue());
        assertThat(routes, hasSize(5));
        assertThat(routes.get(0).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63af"));
        assertThat(routes.get(1).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ab"));
        assertThat(routes.get(2).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ac"));
        assertThat(routes.get(3).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ae"));
        assertThat(routes.get(4).getId(), equalTo("71fb0d48-674b-4f64-a3e5-0256ff3a63ba"));
    }

    /* ---------------------- remove ------------------------- */

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageTenantIsNullWhenRemoving() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(null,existingGuid);

        assertThat(response,hasErrorMessage(CommonValidations.TENANT_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageTenantDoesNotExistWhenRemoving() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(
                Tenant.builder().id("unknown_id").name("name").domainName("unknown_domain").build(),
                existingGuid);

        assertThat(response,hasErrorMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageGuidIsNullWhenRemoving() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(tenant,null);

        assertThat(response,hasErrorMessage(EventRouteService.Validations.GUID_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageGuidIsEmptyWhenRemoving() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(tenant,"");

        assertThat(response,hasErrorMessage(EventRouteService.Validations.GUID_NULL.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json", "/fixtures/event-routes.json"})
    public void shouldReturnValidationMessageGuidDoesNotExistWhenRemoving() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(tenant,"unknown_guid");

        assertThat(response,hasErrorMessage(EventRouteService.Validations.EVENT_ROUTE_NOT_FOUND.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json"})
    public void shouldReturnValidationMessageIfRecordIsNullWhenRemoving() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(tenant,existingGuid);

        assertThat(response,hasErrorMessage(EventRouteService.Validations.EVENT_ROUTE_NOT_FOUND.getCode(),null));
    }
    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json", "/fixtures/event-routes.json"})
    public void shouldRemoveSuccessfully() throws Exception {
        NewServiceResponse<EventRoute> response = subject.remove(tenant,existingGuid);

        EventRoute removedRoute = subject.getByGUID(tenant, existingGuid).getResult();

        assertThat(response,isResponseOk());
        assertThat(response.getResult(),notNullValue());
        assertThat(response.getResult().getId(),equalTo(routeId));

        assertThat(removedRoute, nullValue());
    }

}
