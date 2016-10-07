package com.konkerlabs.platform.registry.test.integration.endpoints;

import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.services.JedisTaskService;
import com.konkerlabs.platform.registry.business.services.api.DeviceEventService;
import com.konkerlabs.platform.registry.business.services.api.DeviceRegisterService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;
import com.konkerlabs.platform.registry.config.WebMvcConfig;
import com.konkerlabs.platform.registry.integration.endpoints.DeviceEventRestEndpoint;
import com.konkerlabs.platform.registry.integration.processors.DeviceEventProcessor;
import com.konkerlabs.platform.registry.test.base.SecurityTestConfiguration;
import com.konkerlabs.platform.registry.test.base.WebLayerTestContext;
import com.konkerlabs.platform.registry.test.base.WebTestConfiguration;
import com.konkerlabs.platform.utilities.parsers.json.JsonParsingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebMvcConfig.class, WebTestConfiguration.class, SecurityTestConfiguration.class,
        DeviceEventRestEndpointTest.DeviceEventRestEndpointTestContextConfig.class})
public class DeviceEventRestEndpointTest extends WebLayerTestContext {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public DeviceEventRestEndpoint deviceEventRestEndpoint;

    @Autowired
    private DeviceEventProcessor deviceEventProcessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JsonParsingService jsonParsingService;

    @Autowired
    private DeviceEventService deviceEventService;

    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Autowired
    private Executor executor;

    @Autowired
    private JedisTaskService jedisTaskService;

    private String DEVICE_ID = "95c14b36ba2b43f1";
    private String DEVICE_USER = "tug6g6essh4m";
    private String VALID_CHANNEL = "data";
    private String INVALID_CHANNEL_SIZE = "abcabcabcabcabcabcabcabcabcabcabc";
    private String INVALID_CHANNEL_CHAR = "dataç";
    private Long OFFSET = 1475765814662l;
    private Long waitTime = 30000l;

    @Before
    public void setUp() throws Exception {
        deviceEventProcessor = mock(DeviceEventProcessor.class);
        deviceEventRestEndpoint = new DeviceEventRestEndpoint(
                applicationContext,
                deviceEventProcessor,
                jsonParsingService,
                deviceEventService,
                deviceRegisterService,
                executor,
                jedisTaskService);
    }

    @Test
    public void shouldReturnErrorOnSubscriptionWithInvalidChannel() throws Exception {
        Device device = Device.builder().deviceId("tug6g6essh4m")
                .active(true)
                .apiKey("e4399b2ed998")
                .guid("7d51c242-81db-11e6-a8c2-0746f010e945")
                .description("test")
                .deviceId("device_id")
                .guid("67014de6-81db-11e6-a5bc-3f99b38315c6").build();

        getMockMvc().perform(
                get("/sub/"+ DEVICE_USER +"/"+ INVALID_CHANNEL_CHAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("offset", String.valueOf(OFFSET))
                        .param("waitTime", String.valueOf(waitTime))
                        .flashAttr("principal", device))
                .andExpect(status().isBadRequest());


        getMockMvc().perform(
                get("/sub/"+ DEVICE_USER +"/"+ INVALID_CHANNEL_SIZE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("offset", String.valueOf(OFFSET))
                        .param("waitTime", String.valueOf(waitTime))
                        .flashAttr("principal", device))
                .andExpect(status().isBadRequest());
    }


    @Configuration
    static class DeviceEventRestEndpointTestContextConfig {
        @Bean
        public DeviceRegisterService deviceRegisterService() {
            return Mockito.mock(DeviceRegisterService.class);
        }

        @Bean
        public DeviceEventService deviceEventService() {
            return Mockito.mock(DeviceEventService.class);
        }

        @Bean
        public DeviceEventProcessor deviceEventProcessor() {
            return Mockito.mock(DeviceEventProcessor.class);
        }

        @Bean
        public JsonParsingService jsonParsingService() {
            return Mockito.mock(JsonParsingService.class);
        }

        @Bean
        public JedisTaskService jedisTaskService() {
            return Mockito.mock(JedisTaskService.class);
        }

        @Bean
        public RedisTemplate<String, String> redisTemplate() {
            return Mockito.mock(RedisTemplate.class);
        }

        @Bean
        public Executor executor() {
            return Mockito.mock(Executor.class);
        }
    }
}