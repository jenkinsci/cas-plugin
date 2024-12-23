package org.jenkinsci.plugins.cas.protocols;

import java.util.Collection;

import org.codehaus.groovy.control.CompilationFailedException;
import org.apereo.cas.client.validation.TicketValidator;
import org.jenkinsci.plugins.cas.CasProtocol;
import org.jenkinsci.plugins.cas.Messages;
import org.jenkinsci.plugins.cas.validation.Cas10RoleParsingTicketValidator;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedClasspathException;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

/**
 * CAS 1.0 protocol support.
 * 
 * @author Fabien Crespel
 * @author J. David Beutel
 */
public class Cas10Protocol extends CasProtocol {

	public final String rolesValidationScript;
	public final String testValidationResponse;
	public final boolean sandbox;

	private final SecureGroovyScript secureRolesValidationScript;

	@Deprecated
	public Cas10Protocol(String rolesValidationScript, String testValidationResponse) throws FormException {
		this(rolesValidationScript, testValidationResponse, false);
	}

	@DataBoundConstructor
	public Cas10Protocol(String rolesValidationScript, String testValidationResponse, boolean sandbox) throws FormException {
		super(Cas10RoleParsingTicketValidator.DEFAULT_ROLE_ATTRIBUTE);
		this.rolesValidationScript = Util.fixEmptyAndTrim(rolesValidationScript);
		this.testValidationResponse = Util.fixEmpty(testValidationResponse);
		this.sandbox = sandbox;
		this.secureRolesValidationScript = getSecureGroovyScript(this.rolesValidationScript, this.sandbox);
	}

	@Override
	public TicketValidator createTicketValidator(String casServerUrl) {
		Cas10RoleParsingTicketValidator ticketValidator = new Cas10RoleParsingTicketValidator(casServerUrl);
		ticketValidator.setRolesValidationScript(secureRolesValidationScript);
		return ticketValidator;
	}

	private static SecureGroovyScript getSecureGroovyScript(String script, boolean sandbox) throws FormException {
		if (script == null) {
			return null;
		} else {
			return new SecureGroovyScript(script, sandbox, null).configuringWithKeyItem();
		}
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<CasProtocol> {
		@Override
		public String getDisplayName() {
			return "CAS 1.0";
		}

		@SuppressWarnings("rawtypes")
		public FormValidation doTestScript(
				@QueryParameter("rolesValidationScript") final String rolesValidationScript,
				@QueryParameter("testValidationResponse") final String testValidationResponse,
				@QueryParameter("sandbox") final boolean sandbox) {
			if (!canRunScripts()) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_noRunScriptPermissionError());
			}
			try {
				Collection roles = Cas10RoleParsingTicketValidator.parseRolesFromValidationResponse(getSecureGroovyScript(rolesValidationScript, sandbox), testValidationResponse);
				if (roles == null) {
					return FormValidation.warning(Messages.Cas10Protocol_rolesValidationScript_noResult());
				}
				return FormValidation.ok(Messages.Cas10Protocol_rolesValidationScript_result() + ": " + roles);
			} catch (CompilationFailedException e) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_compilationError() + ": " + e);
			} catch (ClassCastException e) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_returnTypeError() + ": " + e);
			} catch (RejectedAccessException e) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_rejectedAccessError() + ": " + e);
			} catch (UnapprovedUsageException e) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_unapprovedUsageError() + ": " + e);
			} catch (UnapprovedClasspathException e) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_unapprovedClasspathError() + ": " + e);
			} catch (Exception e) {
				return FormValidation.error(Messages.Cas10Protocol_rolesValidationScript_unknownError() + ": " + e);
			}
		}

		private boolean canRunScripts() {
			return Jenkins.get().getACL().hasPermission(Jenkins.ADMINISTER);
		}
	}

}
