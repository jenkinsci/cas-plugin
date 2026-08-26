package org.jenkinsci.plugins.cas.protocols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.client.validation.Cas30ProxyTicketValidator;
import org.apereo.cas.client.validation.Cas30ServiceTicketValidator;
import org.apereo.cas.client.validation.ProxyList;
import org.apereo.cas.client.validation.TicketValidator;
import org.apereo.cas.client.validation.json.Cas30JsonProxyTicketValidator;
import org.apereo.cas.client.validation.json.Cas30JsonServiceTicketValidator;
import org.jenkinsci.plugins.cas.CasProtocol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;

/**
 * CAS 3.0 protocol support.
 * 
 * @author Fabien Crespel
 */
public class Cas30Protocol extends CasProtocol {

	public final Boolean proxyEnabled;
	public final Boolean proxyAllowAny;
	public final String proxyAllowList;
	public final Boolean jsonEnabled;

	@Deprecated
	public Cas30Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, Boolean proxyEnabled, Boolean proxyAllowAny, String proxyAllowList) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, proxyEnabled, proxyAllowAny, proxyAllowList, false);
	}

	@Deprecated
	public Cas30Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, Boolean proxyEnabled, Boolean proxyAllowAny, String proxyAllowList, Boolean jsonEnabled) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, null, proxyEnabled, proxyAllowAny, proxyAllowList, jsonEnabled, null);
	}

	public Cas30Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, String customValidationParams, Boolean proxyEnabled, Boolean proxyAllowAny, String proxyAllowList, Boolean jsonEnabled) {
		this(authoritiesAttribute, fullNameAttribute, emailAttribute, customValidationParams, proxyEnabled, proxyAllowAny, proxyAllowList, jsonEnabled, null);
	}

	@DataBoundConstructor
	public Cas30Protocol(String authoritiesAttribute, String fullNameAttribute, String emailAttribute, String customValidationParams, Boolean proxyEnabled, Boolean proxyAllowAny, String proxyAllowList, Boolean jsonEnabled, String authoritiesAttributeSeparator) {
		super(authoritiesAttribute, fullNameAttribute, emailAttribute, customValidationParams, authoritiesAttributeSeparator);
		this.proxyEnabled = proxyEnabled;
		this.proxyAllowAny = proxyAllowAny;
		this.proxyAllowList = proxyAllowList;
		this.jsonEnabled = jsonEnabled;
	}

	@Override
	public TicketValidator createTicketValidator(String casServerUrl) {
		Map<String, String> customParams = new HashMap<>();
		customParams.putAll(getCustomValidationParamsMap());
		if (this.proxyEnabled != null && this.proxyEnabled) {
			Cas30ProxyTicketValidator ptv;
			if (Boolean.TRUE.equals(this.jsonEnabled)) {
				ptv = new Cas30JsonProxyTicketValidator(casServerUrl);
				customParams.put("format", "JSON");
			} else {
				ptv = new Cas30ProxyTicketValidator(casServerUrl);
			}
			ptv.setCustomParameters(customParams);
			ptv.setAcceptAnyProxy(this.proxyAllowAny);
			String[] proxyChain = StringUtils.split(this.proxyAllowList, '\n');
			if (proxyChain != null && proxyChain.length > 0) {
				List<String[]> proxyList = new ArrayList<String[]>(1);
				proxyList.add(proxyChain);
				ptv.setAllowedProxyChains(new ProxyList(proxyList));
			}
			return ptv;
		} else {
			Cas30ServiceTicketValidator stv;
			if (Boolean.TRUE.equals(this.jsonEnabled)) {
				stv = new Cas30JsonServiceTicketValidator(casServerUrl);
				customParams.put("format", "JSON");
			} else {
				stv = new Cas30ServiceTicketValidator(casServerUrl);
			}
			stv.setCustomParameters(customParams);
			return stv;
		}
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<CasProtocol> {
		@Override
		public String getDisplayName() {
			return "CAS 3.0";
		}
	}

}
