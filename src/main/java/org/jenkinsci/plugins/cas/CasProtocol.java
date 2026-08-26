package org.jenkinsci.plugins.cas;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.client.validation.TicketValidator;
import org.springframework.security.cas.ServiceProperties;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * CAS protocol extension point. The protocol determines how to validate a
 * server response, and may define specific configuration options.
 * 
 * @author Fabien Crespel
 */
public abstract class CasProtocol extends AbstractDescribableImpl<CasProtocol> implements ExtensionPoint {

	public final String authoritiesAttribute;
	public final String authoritiesAttributeSeparator;
	public final String fullNameAttribute;
	public final String emailAttribute;
	public final String customValidationParams;

	protected transient String artifactParameter = null;
	protected transient List<String> authoritiesAttributes = null;
	protected transient Map<String, String> customValidationParamsMap = null;

	protected CasProtocol() {
		this(null, null, null);
	}

	protected CasProtocol(String authoritiesAttribute) {
		this(authoritiesAttribute, null, null);
	}

	protected CasProtocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, null);
	}

	protected CasProtocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute,
			String customValidationParams) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, customValidationParams, null);
	}

	protected CasProtocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute,
			String customValidationParams, String authoritiesAttributeSeparator) {
		this.authoritiesAttribute = Util.fixEmptyAndTrim(authoritiesAttribute);
		this.authoritiesAttributeSeparator = Util.fixEmptyAndTrim(authoritiesAttributeSeparator);
		this.fullNameAttribute = Util.fixEmptyAndTrim(fullNameAttribute);
		this.emailAttribute = Util.fixEmptyAndTrim(emailAttribute);
		this.customValidationParams = Util.fixEmptyAndTrim(customValidationParams);
	}

	/**
	 * @return the artifactParameter
	 */
	public String getArtifactParameter() {
		if (artifactParameter == null) {
			artifactParameter = createServiceProperties().getArtifactParameter();
		}
		return artifactParameter;
	}

	/**
	 * @return the authoritiesAttribute
	 */
	public String getAuthoritiesAttribute() {
		return authoritiesAttribute;
	}

	/**
	 * @return the separator used to split a single returned authority value, or null
	 */
	public String getAuthoritiesAttributeSeparator() {
		return authoritiesAttributeSeparator;
	}

	/**
	 * @return the authoritiesAttributes
	 */
	public List<String> getAuthoritiesAttributes() {
		if (authoritiesAttributes == null) {
			authoritiesAttributes = Arrays.asList(StringUtils.split(getAuthoritiesAttribute(), ","));
		}
		return authoritiesAttributes;
	}

	/**
	 * @return the fullNameAttribute
	 */
	public String getFullNameAttribute() {
		return fullNameAttribute;
	}

	/**
	 * @return the emailAttribute
	 */
	public String getEmailAttribute() {
		return emailAttribute;
	}

	/**
	 * @return the customValidationParams
	 */
	public String getCustomValidationParams() {
		return customValidationParams;
	}

	/**
	 * @return the customValidationParamsMap
	 */
	public Map<String, String> getCustomValidationParamsMap() {
		if (customValidationParamsMap == null) {
			Map<String, String> map = new HashMap<>();
			if (customValidationParams != null) {
				String[] parts = StringUtils.split(customValidationParams, "&");
				for (String part : parts) {
					String[] keyValue = StringUtils.split(part, "=");
					if (keyValue.length == 2) {
						try {
							map.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			customValidationParamsMap = map;
		}
		return customValidationParamsMap;
	}

	/**
	 * Create a ServiceProperties object describing the CAS service.
	 * @return ServiceProperties
	 */
	public ServiceProperties createServiceProperties() {
		return new ServiceProperties();
	}

	/**
	 * Create a TicketValidator used to validate a CAS response.
	 * @param casServerUrl CAS server URL prefix
	 * @return TicketValidator
	 */
	public abstract TicketValidator createTicketValidator(String casServerUrl);

	/**
	 * Returns all the registered {@link CasProtocol} descriptors.
	 * @return all {@link CasProtocol} descriptors
	 */
	public static DescriptorExtensionList<CasProtocol, Descriptor<CasProtocol>> all() {
		return Jenkins.get().<CasProtocol, Descriptor<CasProtocol>> getDescriptorList(CasProtocol.class);
	}

}
