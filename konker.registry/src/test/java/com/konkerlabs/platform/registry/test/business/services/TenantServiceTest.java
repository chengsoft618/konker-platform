package com.konkerlabs.platform.registry.test.business.services;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.enumerations.LogLevel;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.TenantService;
import com.konkerlabs.platform.registry.business.services.api.TenantService.Validations;
import com.konkerlabs.platform.registry.test.base.BusinessLayerTestSupport;
import com.konkerlabs.platform.registry.test.base.BusinessTestConfiguration;
import com.konkerlabs.platform.registry.test.base.MongoTestConfiguration;
import com.konkerlabs.platform.registry.test.base.RedisTestConfiguration;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MongoTestConfiguration.class, BusinessTestConfiguration.class,
		RedisTestConfiguration.class })
@UsingDataSet(locations = { "/fixtures/tenants.json", "/fixtures/users.json", "/fixtures/passwordBlacklist.json" })
public class TenantServiceTest extends BusinessLayerTestSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private TenantService tenantService;

	@Autowired
	private TenantRepository tenantRepository;

	private Tenant tenant;

	@Before
	public void setUp() throws Exception {
		tenant = tenantRepository.findByDomainName("konker");
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void shouldUpdateLogLevel() {

		// set disabled
		tenantService.updateLogLevel(tenant, LogLevel.DISABLED);

		Tenant stored = tenantRepository.findOne(tenant.getId());

		Assert.assertNotNull(stored);
		Assert.assertEquals(stored.getLogLevel(), LogLevel.DISABLED);

		tenantService.updateLogLevel(tenant, LogLevel.DISABLED);

		// change to all
		ServiceResponse<Tenant> response = tenantService.updateLogLevel(tenant, LogLevel.ALL);

		stored = tenantRepository.findOne(tenant.getId());

		Assert.assertNotNull(stored);
		Assert.assertEquals(stored.getLogLevel(), LogLevel.ALL);
		Assert.assertEquals(response.getStatus(), ServiceResponse.Status.OK);

	}

	@Test
	public void shouldWarningLogLevelBeDefault() {

		ServiceResponse<Tenant> response = tenantService.updateLogLevel(tenant, null);

		Tenant stored = tenantRepository.findOne(tenant.getId());

		Assert.assertNotNull(stored);
		Assert.assertEquals(stored.getLogLevel(), LogLevel.WARNING);
		Assert.assertEquals(response.getStatus(), ServiceResponse.Status.OK);

	}

	@Test
	public void shouldValidateNullTenant() {

		ServiceResponse<Tenant> response = tenantService.updateLogLevel(null, LogLevel.WARNING);

		Assert.assertNotNull(response);
		Assert.assertEquals(response.getStatus(), ServiceResponse.Status.ERROR);
		Assert.assertTrue(response.getResponseMessages().containsKey(Validations.TENANT_NULL.getCode()));

	}

	@Test
	public void shouldValidateNonExistingTenant() {

		Tenant strangeTenant = Tenant.builder().id("qqqq").build();
		ServiceResponse<Tenant> response = tenantService.updateLogLevel(strangeTenant, LogLevel.WARNING);

		Assert.assertNotNull(response);
		Assert.assertEquals(response.getStatus(), ServiceResponse.Status.ERROR);
		Assert.assertTrue(response.getResponseMessages().containsKey(Validations.NO_EXIST_TENANT.getCode()));

	}

}