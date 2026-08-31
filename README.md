[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/cas-plugin)](https://plugins.jenkins.io/cas-plugin)
[![Jenkins Plugin installs](https://img.shields.io/jenkins/plugin/i/cas-plugin?color=blue)](https://plugins.jenkins.io/cas-plugin)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fcas-plugin%2Fmaster)](https://ci.jenkins.io/job/Plugins/job/cas-plugin/job/master/)

# Jenkins CAS Plugin

This is a [Jenkins](https://jenkins.io) plugin providing authentication with [CAS](https://apereo.github.io/cas/), with Single Sign-On (SSO) and Single Logout (SLO) support.


## Installation

The latest version is available for download from the Update Center and from the [Download Site](https://updates.jenkins.io/download/plugins/cas-plugin/).

### Upgrade notice

Some Jenkins versions require specific CAS plugin versions for compatibility reasons:

| Jenkins version    | CAS plugin version |
|--------------------|--------------------|
| 2.476 and higher   | 1.7.0 and higher   |
| 2.475              | not compatible     |
| 2.266 to 2.474     | 1.5.0 to 1.6.3     |
| 2.160 to 2.265     | 1.4.3              |
| 2.150.2 to 2.150.3 | 1.4.3              |
| Older releases     | 1.4.2 and lower    |

In these cases, you will need to upgrade Jenkins and CAS plugin together to avoid issues. This means manually downloading and updating the `cas-plugin.hpi` file in your Jenkins `plugins` directory (rename to `cas-plugin.jpi` as needed).


## Setup

### Basic Setup

1. Install the plugin from **Manage Jenkins** > **Manage Plugins** > **Available** > **CAS Plugin**.
2. Go to **Manage Jenkins** > **Configure Global Security**, check **Enable Security** and select **CAS (Central Authentication Service)** as the Security Realm.
3. Next to **CAS Server URL**, enter the base URL to your CAS server, e.g. `https://cas.example.com/cas`
4. Next to **CAS Protocol**, select the protocol to use to communicate with CAS, e.g. **SAML 1.1** if you are using Apereo CAS Server 3.x or higher, or **CAS 3.0** if you are using Apereo CAS Server 4.x or higher.
5. If there are no warnings, click the **Save** button at the bottom and attempt logging in.

### Advanced Setup

Additional configuration options are available under the **Security Realm** section:

- **Force authentication renewal:** when checked, Single Sign-On (SSO) is disabled: even if a CAS session is already open, the user will have to provide credentials again to confirm his/her identity.
- **Use CAS REST API for external/scripted clients:** when checked, the [CAS REST API](https://apereo.github.io/cas/development/protocol/REST-Protocol.html) will be used to authenticate Jenkins API requests (in addition to Jenkins API keys) using a username/password.
- **Process Single Logout (SLO) requests from CAS:** when checked, Single Logout is enabled: whenever the user logs out of CAS (e.g. when logging out of another CAS-enabled application), the corresponding Jenkins session will be destroyed and the local user logged out as well. Note that for this to work, the CAS server must be able to communicate with Jenkins using the service URL that was passed to it during login.
- **Logout from CAS when logging out of Jenkins:** when checked, Jenkins will redirect to CAS after logging out the local user, in order to destroy the SSO session.

Several protocols implemented by CAS are available in the **CAS Protocol** dropdown (click the **Advanced...** button to reveal more options):

- **CAS 1.0:** a text-based legacy protocol. Custom extensions may provide support for roles, which can be parsed with a Groovy script specified in **Roles Validation Script**.
- **CAS 2.0:** a XML-based protocol. It supports **Proxy Tickets**, allowing external applications already secured with CAS to authenticate in Jenkins without requiring user input or password. Custom extensions may provide support for attributes.
- **CAS 3.0:** a XML or JSON-based protocol. It supports **Proxy Tickets**, allowing external applications already secured with CAS to authenticate in Jenkins without requiring user input or password. It fully supports attributes out-of-the-box, without requiring custom extensions. **This is a recommended protocol for Apereo CAS Server 4.x and higher.**
- **SAML 1.1:** a XML-based protocol. It fully supports attributes out-of-the-box, without requiring custom extensions. **This is a recommended protocol for Apereo CAS Server 3.x and higher.**

[Attributes](https://apereo.github.io/cas/development/integration/Attribute-Release.html) are an easy (and recommended) way to add full name and email address information to an authenticated user, as well as roles/groups membership. CAS 1.0 response parsing with a custom Groovy script is made available as a legacy option for backward compatibility with the [CAS1 Plugin](https://wiki.jenkins.io/display/JENKINS/CAS1+Plugin).

If the CAS server returns multiple roles as one delimited attribute value, configure **Roles Value Separator** for the selected protocol.


## Usage

### Access from external/scripted clients

By default, when using the CAS plugin for authentication, you **cannot use a regular username/password** for remote authentication into Jenkins. This is by design, as the CAS protocol does not allow "direct" authentication and works with secure redirections, which are not compatible with remote calls such as SVN or GitHub hooks.

You have two options:

- Use the user's **API token** as the password; you can find it by going to the **Configuration** page of the **Jenkins user** you intend to use for external access. This API token does not expire and you may regenerate it as you need.
- Enable the **REST API** option in the plugin configuration, to use the [CAS REST API](https://apereo.github.io/cas/development/protocol/REST-Protocol.html) to process the real username/password. The CAS REST protocol must be enabled server-side for this option to work.

See the following page for more information: [Authenticating scripted clients](https://www.jenkins.io/doc/book/system-administration/authenticating-scripted-clients/)

### Jenkins URL when used behind a reverse proxy

When using Jenkins behind a reverse proxy, depending on configuration the URL users get redirected to after authentication may be wrong. If this is the case:

1. Go to **Manage Jenkins** > **Configure System**.
2. Under **Jenkins Location**, make sure the **Jenkins URL** is valid and can be reached by users. It will be used by CAS to redirect back to Jenkins after authentication.


## Troubleshooting

### SSL certificate issues

Please see the [Troubleshooting Guide](https://apereo.github.io/cas/development/installation/Troubleshooting-Guide.html#pkix-path-building-failed) from the CAS Project.

### Issue validating SAML 1.1 tickets

If Jenkins systematically fails to validate SAML 1.1 tickets, make sure to check whether the **system clock** of your Jenkins and CAS servers are **synchronized**. Indeed, the timestamp at which SAML 1.1 tickets were generated is checked when validating them, with a configurable tolerance (see "Time Tolerance" plugin option). Out-of-sync clocks may log errors such as "skipping assertion that's not yet valid" in Jenkins.

### Failure to authenticate external/scripted clients

By default, using normal username/password is not possible from external/scripted clients when using CAS. You may use an **API token** instead and/or enable the **REST API** support. See "Usage" section above for more details.

### Checking group memberships

To check actual group memberships extracted from configured CAS attributes, go to `http://your-jenkins-server/whoAmI/` and look for the **Authorities** list.

### Missing group memberships when logging with external/scripted clients

This issue ([JENKINS-20064](https://issues.jenkins-ci.org/browse/JENKINS-20064)) is fixed in Jenkins 1.556 and higher, provided that the user logged in through the web interface at least once. This limitation does not apply when the REST API option is enabled along with the real username/password.

### Invalid Jenkins URL after logging in through CAS

If Jenkins is behind a reverse proxy, it may not be able to detect its own URL by itself. In this case, you need to manually configure the Jenkins URL. See "Usage" section above for a solution.

### Analyzing detailed logs

If you cannot understand the root cause of an issue, you may try to enable detailed logs:

1. Go to **Manage Jenkins** > **System Log**.
2. Click **Add recorder**, type a name (e.g. `cas-plugin`) and click **Create**.
3. **Add** the following loggers:
   - `org.apereo.cas`
   - `org.jenkinsci.plugins.cas`
   - `org.springframework.security.cas`
4. Click **Save**, try to reproduce the issue, then refresh the logs view.
5. If you need even more details, you may try adding these loggers as well:
   - `hudson.security`
   - `jenkins.security`
   - `org.springframework.security`


## Building and testing

### Building from source

1. Checkout or download the source code from the current master or latest tag on GitHub.
2. Execute `mvn clean verify` from your local source code folder (install [Maven](https://maven.apache.org) if not already done).
3. Find the `cas-plugin.hpi` file in the `target` subfolder.
4. Upload it to Jenkins from the _Advanced_ tab of the _Manage Plugins_ page.

### Testing locally

The `test` directory contains Docker containers and configuration to run a CAS server locally alongside Jenkins for testing.

1. As a prerequisite, make sure [Docker](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/) are installed.
2. Run `./test.sh` in this directory to start the test environment.
3. Open Jenkins at [http://localhost:8080](http://localhost:8080) and click **Login** to be redirected to CAS.
4. Authenticate with the default user `casuser` and password `Mellon`.
5. Check that Jenkins is now authenticated as `casuser`.


## Documentation

- [Changelog](CHANGELOG.md)
- [Javadoc](https://javadoc.jenkins.io/plugin/cas-plugin/)
- [CAS documentation](https://apereo.github.io/cas/)
