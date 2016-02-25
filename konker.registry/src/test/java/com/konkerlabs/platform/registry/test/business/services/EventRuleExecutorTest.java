package com.konkerlabs.platform.registry.test.business.services;

import com.konkerlabs.platform.registry.business.model.Event;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import com.konkerlabs.platform.registry.business.services.rules.api.EventRuleExecutor;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.spy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoTestConfiguration.class, BusinessTestConfiguration.class})
public class EventRuleExecutorTest extends BusinessLayerTestSupport {

    private static final String REGISTERED_TENANT_DOMAIN = "konker";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private EventRuleExecutor subject;
    private Event event;
    private URI uri;

    private String inactiveRuleDeviceId = "0000000000000001";
    private String malformedRuleDeviceId = "0000000000000002";
    private String matchingRuleDeviceId = "0000000000000004";
    private String nonMatchingFilterDeviceId = "0000000000000007";
    private String nonMatchingRuleDeviceId = "0000000000000009";

    private String payload = "{\"metric\":\"temperature\",\"deviceId\":\"0000000000000004\",\"value\":30,\"ts\":1454900000,\"data\":{\"sn\":1234,\"test\":1,\"foo\":2}}";

    @Before
    public void setUp() throws Exception {
        event = spy(Event.builder().channel("data").timestamp(Instant.now()).payload(payload).build());
        uri = new DeviceURIDealer() {}.toDeviceRuleURI(REGISTERED_TENANT_DOMAIN,matchingRuleDeviceId);
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/devices.json","/fixtures/event-rules.json"})
    public void shouldSendEventsForAMatchingRule() throws ExecutionException, InterruptedException {
        Future<List<Event>> eventFuture = subject.execute(event, uri);
        assertThat(eventFuture.get(), notNullValue());
        assertThat(eventFuture.get(), hasSize(3));
        assertThat(eventFuture.get().get(0).getPayload(), equalTo(payload));
        assertThat(eventFuture.get().get(1).getPayload(), equalTo(payload));
        assertThat(eventFuture.get().get(2).getPayload(), equalTo(payload));
    }

    @Test
    @UsingDataSet(locations = "/fixtures/event-rules.json")
    public void shouldntSendAnyEventsForANonmatchingRule() throws ExecutionException, InterruptedException, URISyntaxException {
        URI nonMatchingFilterURI = new URI("device",nonMatchingFilterDeviceId,null,null,null);
        Future<List<Event>> eventFuture = subject.execute(event, nonMatchingFilterURI);
        assertThat(eventFuture.get(), notNullValue());
        assertThat(eventFuture.get(), hasSize(0));
    }

    @Test
    @UsingDataSet(locations = "/fixtures/event-rules.json")
    public void shouldntSendAnyEventsForANonMatchingIncomingDevice() throws ExecutionException, InterruptedException, URISyntaxException {
        URI nonMatchingDeviceURI = new URI("device",nonMatchingRuleDeviceId,null,null,null);
        Future<List<Event>> eventFuture = subject.execute(event, nonMatchingDeviceURI);
        assertThat(eventFuture.get(), notNullValue());
        assertThat(eventFuture.get(), hasSize(0));
    }

    @Test
    @UsingDataSet(locations = "/fixtures/event-rules.json")
    public void shouldntSendAnyEventsForANonMatchingIncomingChannel() throws ExecutionException, InterruptedException, URISyntaxException {
        URI nonMatchingDeviceURI = new URI("device",matchingRuleDeviceId,null,null,null);
        event.setChannel("non_matching_channel");
        Future<List<Event>> eventFuture = subject.execute(event, nonMatchingDeviceURI);
        assertThat(eventFuture.get(), notNullValue());
        assertThat(eventFuture.get(), hasSize(0));
    }

    @Test
    @UsingDataSet(locations = "/fixtures/event-rules.json")
    public void shouldntSendAnyEventsForANonActiveRule() throws ExecutionException, InterruptedException, URISyntaxException {
        URI inactiveRuleDeviceURI = new URI("device",inactiveRuleDeviceId,null,null,null);
        Future<List<Event>> eventFuture = subject.execute(event, inactiveRuleDeviceURI);
        assertThat(eventFuture, notNullValue());
        assertThat(eventFuture.get(), hasSize(0));
    }

    @Test
    @UsingDataSet(locations = "/fixtures/event-rules.json")
    public void shouldntSendAnyEventsForMalformedExpressionFilter() throws ExecutionException, InterruptedException, URISyntaxException {
        URI nonBooleanRuleDeviceURI = new URI("device", malformedRuleDeviceId,null,null,null);
        Future<List<Event>> eventFuture = subject.execute(event, nonBooleanRuleDeviceURI);
        assertThat(eventFuture, notNullValue());
        assertThat(eventFuture.get(), hasSize(0));
    }

}
