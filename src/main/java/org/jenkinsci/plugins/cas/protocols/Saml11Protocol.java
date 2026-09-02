package org.jenkinsci.plugins.cas.protocols;

import org.apereo.cas.client.validation.Saml11TicketValidator;
import org.apereo.cas.client.validation.TicketValidator;
import org.jenkinsci.plugins.cas.CasProtocol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.springframework.security.cas.SamlServiceProperties;
import org.springframework.security.cas.ServiceProperties;

import hudson.Extension;
import hudson.model.Descriptor;

/**
 * SAML 1.1 protocol support.
 * 
 * @author Fabien Crespel
 */
public class Saml11Protocol extends CasProtocol {

	public final int tolerance;

	@Deprecated
	public Saml11Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, int tolerance) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, null, tolerance);
	}

	@Deprecated
	public Saml11Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, String customValidationParams, int tolerance) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, customValidationParams, tolerance, null);
	}

	@DataBoundConstructor
	public Saml11Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, String customValidationParams, int tolerance, String authoritiesValueSeparator) {
		super(authoritiesAttribute, fullNameAttribute, emailAttribute, customValidationParams, authoritiesValueSeparator);
		this.tolerance = tolerance;
	}

	@Override
	public ServiceProperties createServiceProperties() {
		return new SamlServiceProperties();
	}

	@Override
	public TicketValidator createTicketValidator(String casServerUrl) {
		Saml11TicketValidator ticketValidator = new Saml11TicketValidator(casServerUrl);
		ticketValidator.setCustomParameters(getCustomValidationParamsMap());
		ticketValidator.setTolerance(tolerance);
		return ticketValidator;
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<CasProtocol> {
		@Override
		public String getDisplayName() {
			return "SAML 1.1";
		}
	}

}
