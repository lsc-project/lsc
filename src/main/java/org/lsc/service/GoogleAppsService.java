/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2011 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.GoogleAppsConnectionType;
import org.lsc.configuration.GoogleAppsServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.ClientLoginAccountType;
import com.google.gdata.client.GoogleService.CaptchaRequiredException;
import com.google.gdata.client.appsforyourdomain.AppsForYourDomainQuery;
import com.google.gdata.client.appsforyourdomain.AppsGroupsService;
import com.google.gdata.client.appsforyourdomain.EmailListRecipientService;
import com.google.gdata.client.appsforyourdomain.EmailListService;
import com.google.gdata.client.appsforyourdomain.NicknameService;
import com.google.gdata.client.appsforyourdomain.UserService;
import com.google.gdata.data.Link;
import com.google.gdata.data.appsforyourdomain.AppsForYourDomainException;
import com.google.gdata.data.appsforyourdomain.Login;
import com.google.gdata.data.appsforyourdomain.Name;
import com.google.gdata.data.appsforyourdomain.Nickname;
import com.google.gdata.data.appsforyourdomain.Quota;
import com.google.gdata.data.appsforyourdomain.provisioning.NicknameEntry;
import com.google.gdata.data.appsforyourdomain.provisioning.NicknameFeed;
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry;
import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

/**
 * This class enables a provisioning of the Google Apps accounts - Provisioning API version 2.0
 * 
 * This service reuses most of the code provided as samples by Google at the following location:
 * http://code.google.com/p
 * /gdata-java-client/source/browse/trunk/java/sample/appsforyourdomain/AppsForYourDomainClient.java
 * 
 * This service never delete users account: it only suspends them.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class GoogleAppsService implements IWritableService {

    public static final String          DATASET_NAME_ID            = "id";
    public static final String          DATASET_NAME_MAIL          = "mail";
    public static final String          DATASET_NAME_LOGIN         = "uid";
    public static final String          DATASET_NAME_PASSWORD      = "userPassword";
    public static final String          DATASET_NAME_PASSWORD_HASH = "userPasswordHash";
    public static final String          DATASET_NAME_SUSPENDED     = "isSuspended";
    public static final String          DATASET_NAME_ADMIN         = "isAdmin";
    public static final String          DATASET_NAME_AGREEDTOTERMS = "isAgreedToTerms";
    public static final String          DATASET_NAME_WHITELISTED   = "isIpWhitelisted";
    public static final String          DATASET_NAME_GIVENNAME     = "givenName";
    public static final String          DATASET_NAME_SURNAME       = "sn";
    public static final String          DATASET_NAME_QUOTA         = "quotaInMb";
    public static final String          DATASET_NAME_CREATETS      = "createTimestamp";
    public static final String          DATASET_NAME_MODIFYTS      = "modifyTimestamp";
    public static final String          DATASET_NAME_NICKNAME      = "nickname";

    private static final Logger         LOGGER                     = LoggerFactory.getLogger(GoogleAppsService.class);

    private static final String         APPS_FEEDS_URL_BASE        = "https://apps-apis.google.com/a/feeds/";

    protected static final String       SERVICE_VERSION            = "2.0";

    protected String                    domainUrlBase;

    private Map<String, UserEntry>      usersCache;

    protected EmailListRecipientService emailListRecipientService;
    protected EmailListService          emailListService;
    protected NicknameService           nicknameService;
    protected UserService               userService;
    protected AppsGroupsService         groupService;

    private GoogleAppsConnectionType    conn;
    private GoogleAppsServiceType       service;

    private Class<IBean>                beanClass;

    @SuppressWarnings("unchecked")
    public GoogleAppsService(final TaskType task) throws LscServiceException {
        service = (task.getGoogleAppsDestinationService() != null ? task.getGoogleAppsDestinationService() : task.getGoogleAppsSourceService());
        conn = (GoogleAppsConnectionType) service.getConnection().getReference();
        domainUrlBase = APPS_FEEDS_URL_BASE + conn.getUrl() + "/";
        if (!domainUrlBase.startsWith(APPS_FEEDS_URL_BASE)) {
            LOGGER.warn("It seems very strange the your Google Apps provisioning URL doesn't start with: " + APPS_FEEDS_URL_BASE);
        }

        String answer = null;

        try {
            // Configure all of the different Provisioning services
            userService = new UserService("lsc-project.org-AppsForYourDomain-UserService");
            userService.setUserCredentials(conn.getUsername(), conn.getPassword(), ClientLoginAccountType.HOSTED);
        } catch (CaptchaRequiredException e) {
            System.out.println("Please visit " + e.getCaptchaUrl());
            System.out.print("Answer to the challenge? ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try {
                answer = in.readLine();
                userService.setUserCredentials(conn.getName(), conn.getPassword(), e.getCaptchaToken(), answer);
            } catch (IOException ioex) {
                throw new LscServiceConfigurationException(ioex);
            } catch (AuthenticationException ae) {
                throw new LscServiceConfigurationException(ae);
            }

        } catch (AuthenticationException e) {
            throw new LscServiceConfigurationException(e);
        }
        try {
            nicknameService = new NicknameService("lsc-project.org-GoogleApps-NicknameService");
            nicknameService.setUserCredentials(conn.getUsername(), conn.getPassword());

            emailListService = new EmailListService("lsc-project.org-GoogleApps-EmailListService");
            emailListService.setUserCredentials(conn.getUsername(), conn.getPassword());

            emailListRecipientService = new EmailListRecipientService("lsc-GoogleApps-EmailListRecipientService");
            emailListRecipientService.setUserCredentials(conn.getUsername(), conn.getPassword());

            groupService = new AppsGroupsService(conn.getUsername(), conn.getPassword(), conn.getUrl(), "lsc-GoogleApps-AppsGroupService");

            beanClass = (Class<IBean>) Class.forName(task.getBean());
        } catch (AuthenticationException e) {
            throw new LscServiceConfigurationException(e);
        } catch (ClassNotFoundException e) {
            throw new LscServiceConfigurationException(e);
        }
    }

    @Override
    public IBean getBean(String pivotName, LscDatasets pivotAttributes, boolean fromSameService)
            throws LscServiceException {
        try {
            switch (service.getApiCategory()) {
                case USER_ACCOUNTS:
                    return convertUserEntryToBean(getUsersCache().get(pivotAttributes.getStringValueAttribute(DATASET_NAME_LOGIN) != null ? pivotAttributes.getStringValueAttribute(DATASET_NAME_LOGIN) : pivotName));
                case GROUPS:
                case ORGANIZATION_UNITS:
                default:
                    throw new UnsupportedOperationException();
            }
        } catch (InstantiationException e) {
            throw new LscServiceException(e);
        } catch (IllegalAccessException e) {
            throw new LscServiceException(e);
        } catch (AppsForYourDomainException e) {
            throw new LscServiceException(e);
        } catch (ServiceException e) {
            throw new LscServiceException(e);
        } catch (IOException e) {
            throw new LscServiceException(e);
        }
    }

    private Map<String, UserEntry> getUsersCache() throws AppsForYourDomainException, ServiceException, IOException {
        if (usersCache == null) {
            usersCache = new HashMap<String, UserEntry>();
            for (UserEntry userEntry : retrieveAllUsers().getEntries()) {
                usersCache.put(userEntry.getLogin().getUserName(), userEntry);
            }
        }
        return usersCache;
    }

    private IBean convertUserEntryToBean(UserEntry userEntry) throws InstantiationException, IllegalAccessException {
        if (userEntry == null || !userEntry.getCategories().contains(UserEntry.USER_CATEGORY)) {
            return null;
        }
        IBean result = beanClass.newInstance();
        result.setMainIdentifier(userEntry.getLogin().getUserName());
        LscDatasets datasets = new LscDatasets();

        datasets.getDatasets().put(DATASET_NAME_ID, userEntry.getId());

        if (userEntry.getEmail() != null) {
            datasets.getDatasets().put(DATASET_NAME_MAIL, userEntry.getEmail().toString());
        }

        datasets.getDatasets().put(DATASET_NAME_LOGIN, userEntry.getLogin().getUserName());
        datasets.getDatasets().put(DATASET_NAME_PASSWORD, userEntry.getLogin().getPassword());
        datasets.getDatasets().put(DATASET_NAME_SUSPENDED, userEntry.getLogin().getSuspended());
        datasets.getDatasets().put(DATASET_NAME_ADMIN, userEntry.getLogin().getAdmin());
        datasets.getDatasets().put(DATASET_NAME_AGREEDTOTERMS, userEntry.getLogin().getAgreedToTerms());
        datasets.getDatasets().put(DATASET_NAME_WHITELISTED, userEntry.getLogin().getIpWhitelisted());

        datasets.getDatasets().put(DATASET_NAME_GIVENNAME, userEntry.getName().getGivenName());
        datasets.getDatasets().put(DATASET_NAME_SURNAME, userEntry.getName().getFamilyName());

        datasets.getDatasets().put(DATASET_NAME_QUOTA, userEntry.getQuota().getLimit());

        if (userEntry.getPublished() != null) {
            datasets.getDatasets().put(DATASET_NAME_CREATETS, DateUtils.format(new Date(userEntry.getPublished().getValue())));
        }
        if (userEntry.getUpdated() != null) {
            datasets.getDatasets().put(DATASET_NAME_MODIFYTS, DateUtils.format(new Date(userEntry.getUpdated().getValue())));
        }

        result.setDatasets(datasets);
        return result;
    }

    @Override
    public Map<String, LscDatasets> getListPivots() throws LscServiceException {
        Map<String, LscDatasets> pivots = new HashMap<String, LscDatasets>();
        try {
            switch (service.getApiCategory()) {
                case USER_ACCOUNTS:
                    for (UserEntry userEntry : getUsersCache().values()) {
                        pivots.put(userEntry.getLogin().getUserName(), new LscDatasets());
                    }
                    return pivots;
                case GROUPS:
                case ORGANIZATION_UNITS:
                default:
                    throw new UnsupportedOperationException();
            }
        } catch (IOException e) {
            throw new LscServiceException(e);
        } catch (AppsForYourDomainException e) {
            throw new LscServiceException(e);
        } catch (ServiceException e) {
            throw new LscServiceException(e);
        }
    }

    @Override
    public boolean apply(LscModifications lm) throws LscServiceException {
        try {
            switch (service.getApiCategory()) {
                case USER_ACCOUNTS:
                    switch (lm.getOperation()) {
                        case CREATE_OBJECT:
                            createUser(lm.getMainIdentifier(), convertLMToUserEntry(lm, new UserEntry()));
                            break;
                        case UPDATE_OBJECT:
                        case CHANGE_ID:
                            updateUser(lm.getMainIdentifier(), convertLMToUserEntry(lm, usersCache.get(lm.getMainIdentifier())));
                            break;
                        case DELETE_OBJECT:
                            if(System.getenv("I_UNDERSTAND_THAT_GOOGLEAPPS_ACCOUNTS_WILL_BE_DELETED_WITH_THEIR_DATA") != null) {
                                deleteUser(lm.getMainIdentifier());
                            } else {
                                if (lm.getSourceBean().getDatasetById(DATASET_NAME_NICKNAME) != null) {
                                    deleteNickname(lm.getSourceBean().getDatasetFirstValueById(DATASET_NAME_NICKNAME));
                                }
                                UserEntry userEntry = usersCache.get(lm.getMainIdentifier());
                                userEntry.getLogin().setSuspended(true);
                                updateUser(lm.getMainIdentifier(), userEntry);
                            }
                            break;
                    }
                    break;
                case GROUPS:
                case ORGANIZATION_UNITS:
                default:
                    throw new UnsupportedOperationException();
            }
        } catch (IOException e) {
            throw new LscServiceException(e);
        } catch (AppsForYourDomainException e) {
            throw new LscServiceException(e);
        } catch (ServiceException e) {
            throw new LscServiceException(e);
        } catch (NamingException e) {
            throw new LscServiceException(e);
        }
        return true;
    }

    private UserEntry convertLMToUserEntry(LscModifications lm, UserEntry entry) throws NamingException {
        Login login = new Login();
        boolean loginUpdated = false;
        if (lm.getModificationsItemsByHash().containsKey(DATASET_NAME_LOGIN)) {
            login.setUserName(lm.getModificationsItemsByHash().get(DATASET_NAME_LOGIN).get(0).toString());
            loginUpdated = true;
        }
        if (lm.getModificationsItemsByHash().containsKey(DATASET_NAME_PASSWORD)) {
            login.setPassword(lm.getModificationsItemsByHash().get(DATASET_NAME_PASSWORD).get(0).toString());
            if (lm.getModificationsItemsByHash().containsKey(DATASET_NAME_PASSWORD_HASH)) {
                login.setHashFunctionName(lm.getModificationsItemsByHash().get(DATASET_NAME_PASSWORD_HASH).get(0).toString());
            }
            loginUpdated = true;
        }
        if (loginUpdated) {
            entry.addExtension(login);
        }

        // Block all edition from the administration website as it is synchronized from an external source !
        entry.setCanEdit(false);
        Name name = new Name();
        boolean nameUpdated = false;
        if (lm.getModificationsItemsByHash().containsKey(DATASET_NAME_GIVENNAME)) {
            name.setGivenName(lm.getModificationsItemsByHash().get(DATASET_NAME_GIVENNAME).get(0).toString());
            nameUpdated = true;
        }
        if (lm.getModificationsItemsByHash().containsKey(DATASET_NAME_SURNAME)) {
            name.setFamilyName(lm.getModificationsItemsByHash().get(DATASET_NAME_SURNAME).get(0).toString());
            nameUpdated = true;
        }
        if (nameUpdated) {
            entry.addExtension(name);
        }

        if (lm.getModificationsItemsByHash().containsKey(DATASET_NAME_QUOTA)) {
            Quota quota = new Quota();
            quota.setLimit(Integer.parseInt(lm.getModificationsItemsByHash().get(DATASET_NAME_QUOTA).get(0).toString()));
            entry.addExtension(quota);
        }

        return entry;
    }

    @Override
    public List<String> getWriteDatasetIds() {
        List<String> writableIds = new ArrayList<String>();
        switch (service.getApiCategory()) {
            case USER_ACCOUNTS:
                writableIds.addAll(Arrays.asList(DATASET_NAME_ID, DATASET_NAME_MAIL, DATASET_NAME_LOGIN, DATASET_NAME_PASSWORD, DATASET_NAME_PASSWORD_HASH, DATASET_NAME_SUSPENDED, DATASET_NAME_ADMIN, DATASET_NAME_AGREEDTOTERMS, DATASET_NAME_WHITELISTED, DATASET_NAME_GIVENNAME, DATASET_NAME_SURNAME, DATASET_NAME_QUOTA, DATASET_NAME_CREATETS, DATASET_NAME_MODIFYTS));
                break;
            case GROUPS:
                throw new UnsupportedOperationException();
            case ORGANIZATION_UNITS:
                throw new UnsupportedOperationException();
        }
        return writableIds;
    }

    /**
     * Creates a new user with an email account.
     * 
     * @param username The username of the new user.
     * @param givenName The given name for the new user.
     * @param familyName the family name for the new user.
     * @param password The password for the new user.
     * @return A UserEntry object of the newly created user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry createUser(String username, String givenName, String familyName, String password)
            throws AppsForYourDomainException, ServiceException, IOException {

        return createUser(username, givenName, familyName, password, null, null);
    }

    /**
     * Creates a new user with an email account.
     * 
     * @param username The username of the new user.
     * @param givenName The given name for the new user.
     * @param familyName the family name for the new user.
     * @param password The password for the new user.
     * @param quotaLimitInMb User's quota limit in megabytes. This field is only used for domains with custom quota
     *            limits.
     * @return A UserEntry object of the newly created user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     * 
     */
    public UserEntry createUser(String username, String givenName, String familyName, String password,
            Integer quotaLimitInMb) throws AppsForYourDomainException, ServiceException, IOException {

        return createUser(username, givenName, familyName, password, null, quotaLimitInMb);
    }

    /**
     * Creates a new user with an email account.
     * 
     * @param username The username of the new user.
     * @param givenName The given name for the new user.
     * @param familyName the family name for the new user.
     * @param password The password for the new user.
     * @param passwordHashFunction The name of the hash function to hash the password
     * @return A UserEntry object of the newly created user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry createUser(String username, String givenName, String familyName, String password,
            String passwordHashFunction) throws AppsForYourDomainException, ServiceException, IOException {

        return createUser(username, givenName, familyName, password, passwordHashFunction, null);
    }

    /**
     * Creates a new user with an email account.
     * 
     * @param username The username of the new user.
     * @param givenName The given name for the new user.
     * @param familyName the family name for the new user.
     * @param password The password for the new user.
     * @param passwordHashFunction Specifies the hash format of the password parameter
     * @param quotaLimitInMb User's quota limit in megabytes. This field is only used for domains with custom quota
     *            limits.
     * @return A UserEntry object of the newly created user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry createUser(String username, String givenName, String familyName, String password,
            String passwordHashFunction, Integer quotaLimitInMb) throws AppsForYourDomainException, ServiceException,
            IOException {

        LOGGER.info("Creating user '" + username + "'. Given Name: '" + givenName + "' Family Name: '" + familyName + (passwordHashFunction != null ? "' Hash Function: '" + passwordHashFunction : "") + (quotaLimitInMb != null ? "' Quota Limit: '" + quotaLimitInMb + "'." : "'."));

        UserEntry entry = new UserEntry();
        Login login = new Login();
        login.setUserName(username);
        login.setPassword(password);
        if (passwordHashFunction != null) {
            login.setHashFunctionName(passwordHashFunction);
        }
        entry.addExtension(login);

        Name name = new Name();
        name.setGivenName(givenName);
        name.setFamilyName(familyName);
        entry.addExtension(name);

        if (quotaLimitInMb != null) {
            Quota quota = new Quota();
            quota.setLimit(quotaLimitInMb);
            entry.addExtension(quota);
        }

        URL insertUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION);
        return userService.insert(insertUrl, entry);
    }

    public UserEntry createUser(String username, UserEntry entry) throws AppsForYourDomainException, IOException,
            ServiceException {
        URL insertUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION);
        return userService.insert(insertUrl, entry);
    }

    /**
     * Retrieves a user.
     * 
     * @param username The user you wish to retrieve.
     * @return A UserEntry object of the retrieved user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry retrieveUser(String username) throws AppsForYourDomainException, ServiceException, IOException {

        LOGGER.info("Retrieving user '" + username + "'.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.getEntry(retrieveUrl, UserEntry.class);
    }

    /**
     * Retrieves all users in domain. This method may be very slow for domains with a large number of users. Any changes
     * to users, including creations and deletions, which are made after this method is called may or may not be
     * included in the Feed which is returned.
     * 
     * @return A UserFeed object of the retrieved users.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserFeed retrieveAllUsers() throws AppsForYourDomainException, ServiceException, IOException {

        LOGGER.info("Retrieving all users.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/");
        UserFeed allUsers = new UserFeed();
        UserFeed currentPage;
        Link nextLink;

        do {
            currentPage = userService.getFeed(retrieveUrl, UserFeed.class);
            allUsers.getEntries().addAll(currentPage.getEntries());
            nextLink = currentPage.getLink(Link.Rel.NEXT, Link.Type.ATOM);
            if (nextLink != null) {
                retrieveUrl = new URL(nextLink.getHref());
            }
        } while (nextLink != null);

        return allUsers;
    }

    /**
     * Retrieves one page (100) of users in domain. Any changes to users, including creations and deletions, which are
     * made after this method is called may or may not be included in the Feed which is returned. If the optional
     * startUsername parameter is specified, one page of users is returned which have usernames at or after the
     * startUsername as per ASCII value ordering with case-insensitivity. A value of null or empty string indicates you
     * want results from the beginning of the list.
     * 
     * @param startUsername The starting point of the page (optional).
     * @return A UserFeed object of the retrieved users.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserFeed retrievePageOfUsers(String startUsername) throws AppsForYourDomainException, ServiceException,
            IOException {

        LOGGER.info("Retrieving one page of users" + (startUsername != null ? " starting at " + startUsername : "") + ".");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/");
        AppsForYourDomainQuery query = new AppsForYourDomainQuery(retrieveUrl);
        query.setStartUsername(startUsername);
        return userService.query(query, UserFeed.class);
    }

    /**
     * Updates a user.
     * 
     * @param username The user to update.
     * @param userEntry The updated UserEntry for the user.
     * @return A UserEntry object of the newly updated user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry updateUser(String username, UserEntry userEntry) throws AppsForYourDomainException,
            ServiceException, IOException {

        LOGGER.info("Updating user '" + username + "'.");

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.update(updateUrl, userEntry);
    }

    /**
     * Deletes a user.
     * 
     * @param username The user you wish to delete.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public void deleteUser(String username) throws AppsForYourDomainException, ServiceException, IOException {

        LOGGER.info("Deleting user '" + username + "'.");

        URL deleteUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        userService.delete(deleteUrl);
    }

    /**
     * Suspends a user. Note that executing this method for a user who is already suspended has no effect.
     * 
     * @param username The user you wish to suspend.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry suspendUser(String username) throws AppsForYourDomainException, ServiceException, IOException {

        LOGGER.info("Suspending user '" + username + "'.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        UserEntry userEntry = userService.getEntry(retrieveUrl, UserEntry.class);
        userEntry.getLogin().setSuspended(true);

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.update(updateUrl, userEntry);
    }

    /**
     * Restores a user. Note that executing this method for a user who is not suspended has no effect.
     * 
     * @param username The user you wish to restore.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry restoreUser(String username) throws AppsForYourDomainException, ServiceException, IOException {

        LOGGER.info("Restoring user '" + username + "'.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        UserEntry userEntry = userService.getEntry(retrieveUrl, UserEntry.class);
        userEntry.getLogin().setSuspended(false);

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.update(updateUrl, userEntry);
    }

    /**
     * Set admin privilege for user. Note that executing this method for a user who is already an admin has no effect.
     * 
     * @param username The user you wish to make an admin.
     * @throws AppsForYourDomainException If a Provisioning API specific error occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry addAdminPrivilege(String username) throws AppsForYourDomainException, ServiceException,
            IOException {

        LOGGER.info("Setting admin privileges for user '" + username + "'.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        UserEntry userEntry = userService.getEntry(retrieveUrl, UserEntry.class);
        userEntry.getLogin().setAdmin(true);

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.update(updateUrl, userEntry);
    }

    /**
     * Remove admin privilege for user. Note that executing this method for a user who is not an admin has no effect.
     * 
     * @param username The user you wish to remove admin privileges.
     * @throws AppsForYourDomainException If a Provisioning API specific error occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry removeAdminPrivilege(String username) throws AppsForYourDomainException, ServiceException,
            IOException {

        LOGGER.info("Removing admin privileges for user '" + username + "'.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        UserEntry userEntry = userService.getEntry(retrieveUrl, UserEntry.class);
        userEntry.getLogin().setAdmin(false);

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.update(updateUrl, userEntry);
    }

    /**
     * Require a user to change password at next login. Note that executing this method for a user who is already
     * required to change password at next login as no effect.
     * 
     * @param username The user who must change his or her password.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public UserEntry forceUserToChangePassword(String username) throws AppsForYourDomainException, ServiceException,
            IOException {

        LOGGER.info("Requiring " + username + " to change password at " + "next login.");

        URL retrieveUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        UserEntry userEntry = userService.getEntry(retrieveUrl, UserEntry.class);
        userEntry.getLogin().setChangePasswordAtNextLogin(true);

        URL updateUrl = new URL(domainUrlBase + "user/" + SERVICE_VERSION + "/" + username);
        return userService.update(updateUrl, userEntry);
    }

    /**
     * Creates a nickname for the username.
     * 
     * @param username The user for which we want to create a nickname.
     * @param nickname The nickname you wish to create.
     * @return A NicknameEntry object of the newly created nickname.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public NicknameEntry createNickname(String username, String nickname) throws AppsForYourDomainException,
            ServiceException, IOException {

        LOGGER.info("Creating nickname '" + nickname + "' for user '" + username + "'.");

        NicknameEntry entry = new NicknameEntry();
        Nickname nicknameExtension = new Nickname();
        nicknameExtension.setName(nickname);
        entry.addExtension(nicknameExtension);

        Login login = new Login();
        login.setUserName(username);
        entry.addExtension(login);

        URL insertUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION);
        return nicknameService.insert(insertUrl, entry);
    }

    /**
     * Retrieves a nickname.
     * 
     * @param nickname The nickname you wish to retrieve.
     * @return A NicknameEntry object of the newly created nickname.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public NicknameEntry retrieveNickname(String nickname) throws AppsForYourDomainException, ServiceException,
            IOException {
        LOGGER.info("Retrieving nickname '" + nickname + "'.");

        URL retrieveUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION + "/" + nickname);
        return nicknameService.getEntry(retrieveUrl, NicknameEntry.class);
    }

    /**
     * Retrieves all nicknames for the given username.
     * 
     * @param username The user for which you want all nicknames.
     * @return A NicknameFeed object with all the nicknames for the user.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public NicknameFeed retrieveNicknames(String username) throws AppsForYourDomainException, ServiceException,
            IOException {
        LOGGER.info("Retrieving nicknames for user '" + username + "'.");

        URL feedUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION);
        AppsForYourDomainQuery query = new AppsForYourDomainQuery(feedUrl);
        query.setUsername(username);
        return nicknameService.query(query, NicknameFeed.class);
    }

    /**
     * Retrieves one page (100) of nicknames in domain. Any changes to nicknames, including creations and deletions,
     * which are made after this method is called may or may not be included in the Feed which is returned. If the
     * optional startNickname parameter is specified, one page of nicknames is returned which have names at or after
     * startNickname as per ASCII value ordering with case-insensitivity. A value of null or empty string indicates you
     * want results from the beginning of the list.
     * 
     * @param startNickname The starting point of the page (optional).
     * @return A NicknameFeed object of the retrieved nicknames.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public NicknameFeed retrievePageOfNicknames(String startNickname) throws AppsForYourDomainException,
            ServiceException, IOException {

        LOGGER.info("Retrieving one page of nicknames" + (startNickname != null ? " starting at " + startNickname : "") + ".");

        URL retrieveUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION + "/");
        AppsForYourDomainQuery query = new AppsForYourDomainQuery(retrieveUrl);
        query.setStartNickname(startNickname);
        return nicknameService.query(query, NicknameFeed.class);
    }

    /**
     * Retrieves all nicknames in domain. This method may be very slow for domains with a large number of nicknames. Any
     * changes to nicknames, including creations and deletions, which are made after this method is called may or may
     * not be included in the Feed which is returned.
     * 
     * @return A NicknameFeed object of the retrieved nicknames.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public NicknameFeed retrieveAllNicknames() throws AppsForYourDomainException, ServiceException, IOException {

        LOGGER.info("Retrieving all nicknames.");

        URL retrieveUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION + "/");
        NicknameFeed allNicknames = new NicknameFeed();
        NicknameFeed currentPage;
        Link nextLink;

        do {
            currentPage = nicknameService.getFeed(retrieveUrl, NicknameFeed.class);
            allNicknames.getEntries().addAll(currentPage.getEntries());
            nextLink = currentPage.getLink(Link.Rel.NEXT, Link.Type.ATOM);
            if (nextLink != null) {
                retrieveUrl = new URL(nextLink.getHref());
            }
        } while (nextLink != null);

        return allNicknames;
    }

    /**
     * Deletes a nickname.
     * 
     * @param nickname The nickname you wish to delete.
     * @throws AppsForYourDomainException If a Provisioning API specific occurs.
     * @throws ServiceException If a generic GData framework error occurs.
     * @throws IOException If an error occurs communicating with the GData service.
     */
    public void deleteNickname(String nickname) throws IOException, AppsForYourDomainException, ServiceException {

        LOGGER.info("Deleting nickname '" + nickname + "'.");

        URL deleteUrl = new URL(domainUrlBase + "nickname/" + SERVICE_VERSION + "/" + nickname);
        nicknameService.delete(deleteUrl);
    }

    /**
     * @see org.lsc.service.IService.getSupportedConnectionType()
     */
    public Collection<Class<? extends ConnectionType>> getSupportedConnectionType() {
        Collection<Class<? extends ConnectionType>> list = new ArrayList<Class<? extends ConnectionType>>();
        list.add(GoogleAppsConnectionType.class);
        return list;
    }
}
