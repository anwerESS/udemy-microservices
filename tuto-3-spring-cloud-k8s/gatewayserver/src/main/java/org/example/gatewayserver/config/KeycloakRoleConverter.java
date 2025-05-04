package org.example.gatewayserver.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom converter that extracts Keycloak roles from a JWT token
 * and converts them into Spring Security GrantedAuthority objects.
 *
 * Implements Spring's Converter interface to transform JWT → Collection<GrantedAuthority>
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /**
     * Converts a JWT token into a collection of Spring Security authorities
     * by extracting roles from the Keycloak-specific "realm_access" claim.
     *
     * @param source The JWT token containing Keycloak role information
     * @return Collection of GrantedAuthority objects (will be empty if no roles found)
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        // 1. Extract the "realm_access" claim from the JWT
        // This is Keycloak-specific structure where roles are stored
        Map<String, Object> realmAccess = (Map<String, Object>) source.getClaims().get("realm_access");

        // 2. Return empty collection if no roles are found
        if (realmAccess == null || realmAccess.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. Process the roles and convert them to Spring Security authorities
        Collection<GrantedAuthority> returnValue = ((List<String>) realmAccess.get("roles"))
                .stream()
                // Add "ROLE_" prefix to match Spring Security's role convention
                .map(roleName -> "ROLE_" + roleName)
                // Convert each role to a SimpleGrantedAuthority
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return returnValue;
    }
}