package com.konkerlabs.platform.registry.business.model;

import com.konkerlabs.platform.registry.business.model.validation.CommonValidations;
import com.konkerlabs.platform.utilities.validations.api.Validatable;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import lombok.*;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@Document(collection = "devices")
public class Device implements DeviceURIDealer, Validatable, UserDetails {

	public enum Validations {
		ID_NULL_EMPTY("model.device.id.not_null"),
		ID_GREATER_THAN_EXPECTED("model.device.id.greater_than_expected"),
		NAME_NULL_EMPTY("model.device.name.not_null"),
		REGISTRATION_DATE_NULL("model.device.registration_date.not_null"),
		API_KEY_NULL("model.device.api_key.not_null");

		public String getCode() {
			return code;
		}

		private String code;

		Validations(String code) {
			this.code = code;
		}
	}

    private String id;
	@DBRef
	private Tenant tenant;
	private String deviceId;
    private String apiKey;
	private String securityHash;
	private String name;
	private String description;
	private Instant registrationDate;
//	private List<Event> events;
	private boolean active;

	public Optional<Map<String, Object[]>> applyValidations() {

		Map<String, Object[]> validations = new HashMap<>();

		if (getDeviceId() == null || getDeviceId().isEmpty())
			validations.put(Validations.ID_NULL_EMPTY.code,null);
		if (getDeviceId() != null && getDeviceId().length() > 16)
			validations.put(Validations.ID_GREATER_THAN_EXPECTED.code,new Object[]{16});
		if (getName() == null || getName().isEmpty())
			validations.put(Validations.NAME_NULL_EMPTY.code,null);
		if (getTenant() == null)
			validations.put(CommonValidations.TENANT_NULL.getCode(),null);
		if (getRegistrationDate() == null)
			validations.put(Validations.REGISTRATION_DATE_NULL.getCode(),null);
		if (getApiKey() == null || getApiKey().isEmpty())
			validations.put(Validations.API_KEY_NULL.getCode(),null);

		return Optional.of(validations).filter(map -> !map.isEmpty());
	}

	public void onRegistration() {
		setRegistrationDate(Instant.now());
		setApiKey(new BigInteger(60, new Random()).toString(32));
	}

//	public Event getLastEvent() {
//		return getMostRecentEvents().stream().findFirst().orElse(null);
//	}

	// FIXME Needs performance improvement. Sorting those items on the
	// application server and returning all of them is not efficient.
//	public List<Event> getMostRecentEvents() {
//		return Optional.ofNullable(getEvents()).orElse(Collections.emptyList()).stream()
//				.sorted((eventA, eventB) -> eventB.getTimestamp().compareTo(eventA.getTimestamp()))
//				.collect(Collectors.toList());
//	}

	public URI toURI() {
		return toDeviceRouteURI(getTenant().getDomainName(),getDeviceId());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(new SimpleGrantedAuthority("DEVICE"));
	}

	@Override
	public String getPassword() {
		return getSecurityHash();
	}

	@Override
	public String getUsername() {
		return getApiKey();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
