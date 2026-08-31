package org.jenkinsci.plugins.cas.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apereo.cas.client.authentication.AttributePrincipalImpl;
import org.apereo.cas.client.validation.Assertion;
import org.apereo.cas.client.validation.AssertionImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class CasUserDetailsServiceTest {

	@Test
	void keepsSingleAuthorityValueWhenNoSeparatorIsConfigured() {
		UserDetails user = loadUser("groups", "jenkins-core-developer,jenkins-fleet-developer", null);

		assertEquals(Collections.singletonList("jenkins-core-developer,jenkins-fleet-developer"), authorities(user));
	}

	@Test
	void keepsSingleAuthorityValueWhenEmptySeparatorIsConfigured() {
		UserDetails user = loadUser("groups", "jenkins-core-developer,jenkins-fleet-developer", "");

		assertEquals(Collections.singletonList("jenkins-core-developer,jenkins-fleet-developer"), authorities(user));
	}

	@Test
	void splitsSingleAuthorityValueWhenLiteralSeparatorIsConfigured() {
		UserDetails user = loadUser("groups", "jenkins-core-developer,jenkins-fleet-developer", ",");

		assertEquals(Arrays.asList("jenkins-core-developer", "jenkins-fleet-developer"), authorities(user));
	}

	@Test
	void splitsSingleAuthorityValueWhenSpaceSeparatorIsConfigured() {
		UserDetails user = loadUser("groups", "jenkins-core-developer jenkins-fleet-developer", " ");

		assertEquals(Arrays.asList("jenkins-core-developer", "jenkins-fleet-developer"), authorities(user));
	}

	@Test
	void splitsMultipleAuthorityValuesWhenLiteralSeparatorIsConfigured() {
		UserDetails user = loadUser("groups", Arrays.asList("core|developer", "fleet|developer"), "|");

		assertEquals(Arrays.asList("core", "developer", "fleet"), authorities(user));
	}

	private static UserDetails loadUser(String attributeName, Object value, String separator) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put(attributeName, value);
		AttributePrincipalImpl principal = new AttributePrincipalImpl("user", attributes);
		Assertion assertion = new AssertionImpl(principal);

		CasUserDetailsService service = new CasUserDetailsService();
		service.setAttributes(Collections.singletonList(attributeName));
		service.setConvertToUpperCase(false);
		service.setAuthoritySeparator(separator);
		return service.loadUserDetails(assertion);
	}

	private static List<String> authorities(UserDetails user) {
		return user.getAuthorities().stream().map(authority -> authority.getAuthority())
				.filter(authority -> !"authenticated".equals(authority)).collect(Collectors.toList());
	}
}
