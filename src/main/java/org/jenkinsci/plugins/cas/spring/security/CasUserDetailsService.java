package org.jenkinsci.plugins.cas.spring.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apereo.cas.client.validation.Assertion;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

/**
 * Populates the {@link org.springframework.security.core.GrantedAuthority}s for a user by reading a list of attributes that were returned as
 * part of the CAS response. Each attribute is read and each value of the attribute is turned into a GrantedAuthority. If the attribute has no
 * value then its not added.
 *
 * @author Scott Battaglia
 * @author Fabien Crespel
 */
public final class CasUserDetailsService extends AbstractCasAssertionUserDetailsService {

	public static final String NON_EXISTENT_PASSWORD_VALUE = "NO_PASSWORD";

	private final List<String> attributes = new ArrayList<>();
	private final List<String> defaultAuthorities = new ArrayList<>();

	private boolean convertToUpperCase = true;

	private String authoritySeparator;

	@Override
	@SuppressWarnings("rawtypes")
	protected UserDetails loadUserDetails(final Assertion assertion) {
		final List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

		for (final String attribute : this.attributes) {
			final Object value = assertion.getPrincipal().getAttributes().get(attribute);

			if (value == null) {
				continue;
			}

			if (value instanceof List) {
				final List list = (List) value;

				for (final Object o : list) {
					addAuthorities(grantedAuthorities, o.toString());
				}

			} else {
				addAuthorities(grantedAuthorities, value.toString());
			}
		}

		for (final String authority : this.defaultAuthorities) {
			grantedAuthorities.add(new SimpleGrantedAuthority(authority));
		}

		return new User(assertion.getPrincipal().getName(), NON_EXISTENT_PASSWORD_VALUE, true, true, true, true, grantedAuthorities);
	}

	private void addAuthorities(final List<GrantedAuthority> grantedAuthorities, final String value) {
		String[] authorities = this.authoritySeparator == null ? new String[] { value }
				: value.split(Pattern.quote(this.authoritySeparator));
		for (String authority : authorities) {
			if (StringUtils.hasText(authority)) {
				grantedAuthorities.add(new SimpleGrantedAuthority(this.convertToUpperCase ? authority.toUpperCase() : authority));
			}
		}
	}

	/**
	 * Get the attribute names used to extract granted authorities.
	 * @return the attributes
	 */
	public List<String> getAttributes() {
		return Collections.unmodifiableList(attributes);
	}

	/**
	 * Set the attribute names used to extract granted authorities.
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<String> attributes) {
		this.attributes.clear();
		this.attributes.addAll(attributes);
	}

	/**
	 * Sets the literal separator used to split each returned authority value.
	 * A null or empty separator preserves the CAS value unchanged.
	 * @param authoritySeparator separator, or null to disable splitting
	 */
	public void setAuthoritySeparator(final String authoritySeparator) {
		this.authoritySeparator = StringUtils.hasText(authoritySeparator) ? authoritySeparator : null;
	}

	/**
	 * Converts the returned attribute values to uppercase values.
	 * @return true if it should convert, false otherwise.
	 */
	public boolean isConvertToUpperCase() {
		return convertToUpperCase;
	}

	/**
	 * Converts the returned attribute values to uppercase values.
	 * @param convertToUpperCase true if it should convert, false otherwise.
	 */
	public void setConvertToUpperCase(final boolean convertToUpperCase) {
		this.convertToUpperCase = convertToUpperCase;
	}

	/**
	 * Get default authorities to add to the user in any case.
	 * @return default authorities
	 */
	public List<String> getDefaultAuthorities() {
		return Collections.unmodifiableList(defaultAuthorities);
	}

	/**
	 * Set default authorities to add to the user in any case.
	 * @param defaultAuthorities default authorities
	 */
	public void setDefaultAuthorities(List<String> defaultAuthorities) {
		this.defaultAuthorities.clear();
		this.defaultAuthorities.addAll(defaultAuthorities);
	}
}
