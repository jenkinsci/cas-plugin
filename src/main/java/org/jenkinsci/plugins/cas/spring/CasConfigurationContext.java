package org.jenkinsci.plugins.cas.spring;

import java.util.Collections;

import org.apereo.cas.client.session.HashMapBackedSessionMappingStorage;
import org.apereo.cas.client.session.SessionMappingStorage;
import org.apereo.cas.client.session.SingleSignOutHandler;
import org.apereo.cas.client.validation.AbstractUrlBasedTicketValidator;
import org.apereo.cas.client.validation.TicketValidator;
import org.jenkinsci.plugins.cas.CasProtocol;
import org.jenkinsci.plugins.cas.CasSecurityRealm;
import org.jenkinsci.plugins.cas.spring.security.CasRestAuthenticator;
import org.jenkinsci.plugins.cas.spring.security.CasSessionFixationProtectionStrategy;
import org.jenkinsci.plugins.cas.spring.security.CasSingleSignOutFilter;
import org.jenkinsci.plugins.cas.spring.security.CasUserDetailsService;
import org.jenkinsci.plugins.cas.spring.security.DynamicServiceAuthenticationDetailsSource;
import org.jenkinsci.plugins.cas.spring.security.SessionUrlAuthenticationSuccessHandler;
import org.jenkinsci.plugins.cas.spring.security.SessionUrlCasAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import hudson.security.ChainedServletFilter2;
import hudson.security.HttpSessionContextIntegrationFilter2;
import hudson.security.SecurityRealm;

/**
 * Spring configuration class for the CAS ApplicationContext.
 * CasSecurityRealm and CasProtocol beans must be registered before adding this
 * class to the ApplicationContext. Note that CGLIB proxying is deliberately
 * disabled to avoid adding the dependency.
 * 
 * @author Fabien Crespel
 */
@Configuration(proxyBeanMethods = false)
public class CasConfigurationContext {

	@Bean
	public AuthenticationEventPublisher authenticationEventPublisher() {
		return new DefaultAuthenticationEventPublisher();
	}

	@Bean
	public CasEventListener casEventListener(CasProtocol casProtocol) {
		CasEventListener casEventListener = new CasEventListener();
		casEventListener.setFullNameAttribute(casProtocol.getFullNameAttribute());
		casEventListener.setEmailAttribute(casProtocol.getEmailAttribute());
		return casEventListener;
	}

	@Bean
	public ServiceProperties casServiceProperties(CasSecurityRealm securityRealm, CasProtocol casProtocol) {
		ServiceProperties casServiceProperties = casProtocol.createServiceProperties();
		casServiceProperties.setSendRenew(securityRealm.forceRenewal);
		casServiceProperties.setService(CasSecurityRealm.getFinishLoginUrl());
		return casServiceProperties;
	}

	@Bean
	public TicketValidator casTicketValidator(CasSecurityRealm securityRealm, CasProtocol casProtocol) {
		TicketValidator casTicketValidator = casProtocol.createTicketValidator(securityRealm.casServerUrl);
		if (casTicketValidator instanceof AbstractUrlBasedTicketValidator) {
			((AbstractUrlBasedTicketValidator) casTicketValidator).setRenew(securityRealm.forceRenewal);
		}
		return casTicketValidator;
	}

	@Bean
	public CasUserDetailsService casUserDetailsService(CasProtocol casProtocol) {
		CasUserDetailsService casUserDetailsService = new CasUserDetailsService();
		casUserDetailsService.setAttributes(casProtocol.getAuthoritiesAttributes());
		casUserDetailsService.setAuthoritySeparator(casProtocol.getAuthoritiesValueSeparator());
		casUserDetailsService.setConvertToUpperCase(false);
		casUserDetailsService.setDefaultAuthorities(Collections.singletonList(SecurityRealm.AUTHENTICATED_AUTHORITY2.getAuthority()));
		return casUserDetailsService;
	}

	@Bean
	public CasAuthenticationProvider casAuthenticationProvider(TicketValidator casTicketValidator, CasUserDetailsService casUserDetailsService) {
		CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
		casAuthenticationProvider.setTicketValidator(casTicketValidator);
		casAuthenticationProvider.setAuthenticationUserDetailsService(casUserDetailsService);
		casAuthenticationProvider.setKey("cas_auth_provider");
		return casAuthenticationProvider;
	}

	@Bean
	public AuthenticationManager casAuthenticationManager(CasAuthenticationProvider casAuthenticationProvider, AuthenticationEventPublisher authenticationEventPublisher) {
		ProviderManager casAuthenticationManager = new ProviderManager(casAuthenticationProvider);
		casAuthenticationManager.setAuthenticationEventPublisher(authenticationEventPublisher);
		return casAuthenticationManager;
	}

	@Bean
	public CasAuthenticationEntryPoint casAuthenticationEntryPoint(CasSecurityRealm securityRealm, ServiceProperties casServiceProperties) {
		SessionUrlCasAuthenticationEntryPoint casAuthenticationEntryPoint = new SessionUrlCasAuthenticationEntryPoint();
		casAuthenticationEntryPoint.setLoginUrl(securityRealm.casServerUrl + "login");
		casAuthenticationEntryPoint.setServiceProperties(casServiceProperties);
		casAuthenticationEntryPoint.setTargetUrlParameter("from");
		casAuthenticationEntryPoint.setTargetUrlSessionAttribute(SessionUrlAuthenticationSuccessHandler.DEFAULT_TARGET_URL_SESSION_ATTRIBUTE);
		return casAuthenticationEntryPoint;
	}

	@Bean
	public DynamicServiceAuthenticationDetailsSource casAuthenticationDetailsSource(ServiceProperties casServiceProperties) {
		return new DynamicServiceAuthenticationDetailsSource(casServiceProperties);
	}

	@Bean
	public SessionMappingStorage casSessionMappingStorage() {
		return new HashMapBackedSessionMappingStorage();
	}

	@Bean
	public HttpSessionContextIntegrationFilter2 httpSessionContextIntegrationFilter() {
		HttpSessionSecurityContextRepository httpSessionSecurityContextRepository = new HttpSessionSecurityContextRepository();
		httpSessionSecurityContextRepository.setAllowSessionCreation(false);
		return new HttpSessionContextIntegrationFilter2(httpSessionSecurityContextRepository);
	}

	@Bean
	public SingleSignOutHandler casSingleSignOutHandler(CasProtocol casProtocol, SessionMappingStorage casSessionMappingStorage) {
		SingleSignOutHandler casSingleSignOutHandler = new SingleSignOutHandler();
		casSingleSignOutHandler.setArtifactParameterName(casProtocol.getArtifactParameter());
		casSingleSignOutHandler.setSessionMappingStorage(casSessionMappingStorage);
		return casSingleSignOutHandler;
	}

	@Bean
	public CasSingleSignOutFilter casSingleSignOutFilter(CasSecurityRealm securityRealm, SingleSignOutHandler casSingleSignOutHandler) {
		CasSingleSignOutFilter casSingleSignOutFilter = new CasSingleSignOutFilter();
		casSingleSignOutFilter.setEnabled(securityRealm.enableSingleSignOut);
		casSingleSignOutFilter.setFilterProcessesUrl("/" + CasSecurityRealm.getFinishLoginUrl());
		casSingleSignOutFilter.setSingleSignOutHandler(casSingleSignOutHandler);
		return casSingleSignOutFilter;
	}

	@Bean
	public CasAuthenticationFilter casAuthenticationFilter(AuthenticationManager casAuthenticationManager, DynamicServiceAuthenticationDetailsSource casAuthenticationDetailsSource, ServiceProperties casServiceProperties, SessionMappingStorage casSessionMappingStorage) {
		CasAuthenticationFilter casAuthenticationFilter = new CasAuthenticationFilter();
		casAuthenticationFilter.setFilterProcessesUrl("/" + CasSecurityRealm.getFinishLoginUrl());
		casAuthenticationFilter.setAuthenticationManager(casAuthenticationManager);
		casAuthenticationFilter.setAuthenticationDetailsSource(casAuthenticationDetailsSource);
		casAuthenticationFilter.setServiceProperties(casServiceProperties);
		casAuthenticationFilter.setSessionAuthenticationStrategy(new CasSessionFixationProtectionStrategy(casSessionMappingStorage));
		casAuthenticationFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/" + CasSecurityRealm.getFailedLoginUrl()));
		casAuthenticationFilter.setAuthenticationSuccessHandler(new SessionUrlAuthenticationSuccessHandler("/"));
		casAuthenticationFilter.setContinueChainBeforeSuccessfulAuthentication(true); // Required to reach CasSecurityRealm.doFinishLogin()
		return casAuthenticationFilter;
	}

	@Bean
	public ChainedServletFilter2 casFilter(HttpSessionContextIntegrationFilter2 httpSessionContextIntegrationFilter, CasSingleSignOutFilter casSingleSignOutFilter, CasAuthenticationFilter casAuthenticationFilter) {
		return new ChainedServletFilter2(httpSessionContextIntegrationFilter, casSingleSignOutFilter, casAuthenticationFilter);
	}

	@Bean
	public CasRestAuthenticator casRestAuthenticator(CasSecurityRealm securityRealm, AuthenticationManager casAuthenticationManager, DynamicServiceAuthenticationDetailsSource casAuthenticationDetailsSource) {
		CasRestAuthenticator casRestAuthenticator = new CasRestAuthenticator();
		casRestAuthenticator.setCasServerUrl(securityRealm.casServerUrl);
		casRestAuthenticator.setAuthenticationManager(casAuthenticationManager);
		casRestAuthenticator.setAuthenticationDetailsSource(casAuthenticationDetailsSource);
		return casRestAuthenticator;
	}

}
