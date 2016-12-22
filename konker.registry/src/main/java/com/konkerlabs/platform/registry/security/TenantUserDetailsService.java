package com.konkerlabs.platform.registry.security;

import com.konkerlabs.platform.registry.business.exceptions.BusinessException;
import com.konkerlabs.platform.registry.business.model.User;
import com.konkerlabs.platform.registry.business.repositories.UserRepository;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.config.SecurityConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("tenantUserDetails")
public class TenantUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUserDetailsService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        LOGGER.info("Loading details for username " + email);
        return Optional
            .ofNullable(userRepository.findOne(Optional.of(email).orElse("").trim().toLowerCase()))
            .orElseThrow(() -> new UsernameNotFoundException("authentication.credentials.invalid"));
    }
}
