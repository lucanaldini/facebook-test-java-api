package com.jayway.facebooktestjavaapi.testuser.impl;

import com.jayway.facebooktestjavaapi.testuser.FacebookTestUserAccount;
import com.jayway.facebooktestjavaapi.testuser.FacebookTestUserStore;
import com.jayway.jsonassert.JsonAssert;
import com.jayway.jsonassert.JsonAsserter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * To execute this integration test, remove the &#64;Ignore annotation and create the file
 * <code>facebook-app.properties</code> in the <code>resources</code> directory. It should contain the lines
 * <pre>
 * facebook.appId1=&lt;appId&gt;
 * facebook.appSecret1=&lt;appSecret&gt;
 * facebook.appId2=&lt;appId&gt;
 * facebook.appSecret2=&lt;appSecret&gt;
 * </pre>
 * Where <code>appId1</code>, <code>appSecret1</code>, <code>appId2</code> and <code>appSecret2</code>
 * are replaced with the real values from two Facebook applications.
 * <p/>
 * User: tobias
 * Date: 1/13/11
 * Time: 7:29 AM
 */

public class TestHttpClientFacebookTestUserStore {

    private static HttpClientFacebookTestUserStore facebookStore1;
    private static HttpClientFacebookTestUserStore facebookStore2;

    private JSONParser parser = new JSONParser();
    private static FacebookTestUserAccount account;
    private List<FacebookTestUserAccount> createdAccounts = new LinkedList<FacebookTestUserAccount>();

    @BeforeClass
    public static void beforeAllTests() throws IOException {
        Properties properties = getFacebookConnectionProperties();
        facebookStore1 = new HttpClientFacebookTestUserStore(properties.getProperty("facebook.appId1"), properties.getProperty("facebook.appSecret1"));
        facebookStore1.deleteAllTestUsers();

        facebookStore2 = new HttpClientFacebookTestUserStore(properties.getProperty("facebook.appId2"), properties.getProperty("facebook.appSecret2"));
        facebookStore2.deleteAllTestUsers();

        account = facebookStore1.createTestUser(true, "read_stream");
    }

    private static Properties getFacebookConnectionProperties() throws IOException {
        Properties properties = new Properties();
        InputStream stream = TestHttpClientFacebookTestUserStore.class.getClassLoader().getResourceAsStream("facebook-app.properties");
        if (stream == null) {
            fail("Could not load 'facebook-app.properties");
        }
        properties.load(stream);
        stream.close();
        return properties;
    }

    @AfterClass
    public static void afterAllTests() {
        if (facebookStore1 != null) {
            facebookStore1.deleteAllTestUsers();
        }

        if (facebookStore2 != null) {
            facebookStore2.deleteAllTestUsers();
        }
    }

    @After
    public void afterEachTest() {
        deleteCreatedAccounts();
    }

    /**
     * This test provides a mock HttpClient which will cause an IllegalArgumentException when trying to get
     * test users, because no communication takes place to the Facebook server.
     *
     * @throws Exception If something else fails
     */
    @Test
    public void testCreateTestUserStoreWithProvidedHttpClient() throws Exception {
        Properties properties = getFacebookConnectionProperties();

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(entity);

        HttpClientFacebookTestUserStore testUserStore = new HttpClientFacebookTestUserStore(properties.getProperty("facebook.appId1"), properties.getProperty("facebook.appSecret1"), httpClient);

        try {
            testUserStore.getAllTestUsers();
        } catch (IllegalArgumentException e) {
            assertEquals("Could not get access token for provided authentication", e.getMessage());
        }
    }

    @Test
    public void testCreateFacebookAccount() {

        FacebookTestUserAccount createdAccount = createAccount();

        assertNotNull(createdAccount);
        assertNotNull(createdAccount.id());
        assertNotNull(createdAccount.accessToken());
        assertNotNull(createdAccount.loginUrl());

        assertEquals(2, facebookStore1.getAllTestUsers().size());
    }

    @Test
    public void testDeleteFacebookAccount() {
        FacebookTestUserAccount createdAccount = facebookStore1.createTestUser(true, "");

        assertEquals(2, facebookStore1.getAllTestUsers().size());

        createdAccount.delete();

        assertEquals(1, facebookStore1.getAllTestUsers().size());
    }

    @Test
    public void testMakeFriends() {
        FacebookTestUserAccount account1 = createAccount();
        FacebookTestUserAccount account2 = createAccount();

        account1.makeFriends(account2);

        String friends1 = account1.getFriends();
        String friends2 = account2.getFriends();

        assertTrue("The friends list for account1 does not contain account2", friends1.contains(account2.id()));
        assertTrue("The friends list for account2 does not contain account1", friends2.contains(account1.id()));
    }

    @Test
    public void testChangeNameAndPassword() throws java.text.ParseException
    {
        FacebookTestUserAccount account = createAccount();
        final String accountId = account.id();

        account.changeAccountSettings().newName("nyttNamn").newPassword("mittlogin").apply();

        assertNameChange(accountId, "nyttNamn");
    }


    private void assertNameChange(String accountId, String name) throws java.text.ParseException
    {
        final List<FacebookTestUserAccount> users = facebookStore1.getAllTestUsers();

        for (FacebookTestUserAccount user: users) {
            if (user.id().equals(accountId)) {
                JsonAssert.with(user.getUserDetails()).assertThat("$.name", equalTo(name));
            }
        }
    }


    @Test
    public void testGetUserDetails() throws java.text.ParseException {
        String userDetails = account.getUserDetails();

        JsonAssert.with(userDetails).assertThat("$.name", RegExMatcher.regExMatches("\\w+(?:\\s+\\w+)*"));
        JsonAssert.with(userDetails).assertThat("$.first_name", RegExMatcher.regExMatches("\\w+"));
        JsonAssert.with(userDetails).assertThat("$.middle_name", RegExMatcher.regExMatches("(?:\\w+)*"));
        JsonAssert.with(userDetails).assertThat("$.last_name", RegExMatcher.regExMatches("\\w+"));
    }

    @Test
    public void testGetUserPassword()
    {
        final String password = account.getPassword();

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }

    private static class RegExMatcher extends TypeSafeMatcher<String> {

        private final Pattern pattern;

        public RegExMatcher(String matchExpression) {
            pattern = Pattern.compile(matchExpression);
        }

        @Override
        public boolean matchesSafely(String toCompare) {
            java.util.regex.Matcher matcher = pattern.matcher(toCompare);
            return matcher.matches();
        }

        public void describeTo(Description description) {
            description.appendText("does not match expression '" + pattern.pattern() + "'");
        }

        @Factory
        public static <T> Matcher<String> regExMatches(String matchExpression) {
            return new RegExMatcher(matchExpression);
        }
    }

    @Test
    public void testGetNewsFeed() throws Exception {
        assertContainsData(account.getNewsFeed());
    }

    @Test
    public void testGetProfileFeed() throws Exception {
        assertContainsData(account.getProfileFeed());
    }

    @Test
    public void testGetLikes() throws Exception {
        assertContainsData(account.getLikes());
    }

    @Test
    public void testGetMovies() throws Exception {
        assertContainsData(account.getMovies());
    }

    @Test
    public void testGetMusic() throws Exception {
        assertContainsData(account.getMusic());
    }

    @Test
    public void testGetBooks() throws Exception {
        assertContainsData(account.getBooks());
    }

    @Test
    public void testGetNotes() throws Exception {
        assertContainsData(account.getNotes());
    }

    @Test
    public void testGetPhotoTags() throws Exception {
        assertContainsData(account.getPhotoTags());
    }

    @Test
    public void testGetPhotoAlbums() throws Exception {
        assertContainsData(account.getPhotoAlbums());
    }

    @Test
    public void testGetVideoTags() throws Exception {
        assertContainsData(account.getVideoTags());
    }

    @Test
    public void testGetVideoUploads() throws Exception {
        assertContainsData(account.getVideoUploads());
    }

    @Test
    public void testGetEvents() throws Exception {
        assertContainsData(account.getEvents());
    }

    @Test
    public void testGetGroups() throws Exception {
        assertContainsData(account.getGroups());
    }

    @Test
    public void testGetCheckins() throws Exception {
        assertContainsData(account.getCheckins());
    }

    @Test
    public void testCopyTestUsersToOtherApplication() {
        account.copyToOtherApplication(facebookStore2.getApplicationId(), facebookStore2.getAccessToken(), false, "email");
        assertEquals(1, facebookStore2.getAllTestUsers().size());
        facebookStore2.deleteAllTestUsers();
    }

    @Test
    public void testCopyTestUsersToTestUserStore() {
        account.copyToTestUserStore(facebookStore2, false, "email");
        assertEquals(1, facebookStore2.getAllTestUsers().size());
        facebookStore2.deleteAllTestUsers();
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTestUserStoreShouldThrowException() {
        account.copyToTestUserStore(new FacebookTestUserStore() {
            public FacebookTestUserAccount createTestUser(boolean appInstalled, String permissions) {
                return null;
            }

            public List<FacebookTestUserAccount> getAllTestUsers() {
                return null;
            }

            public void deleteAllTestUsers() {

            }
        }, false, "");
    }

    @Test
    public void testCreateFacebookAccountThatHaveToAcceptPermissions() {
        FacebookTestUserAccount createdAccount = facebookStore1.createTestUser(false, "email,user_about_me,user_birthday");

        assertNull(createdAccount.accessToken());
    }

    // Helpers
    private void assertContainsData(String json) throws ParseException {
        Object result = parser.parse(json);
        assertTrue("The result was not of type JSON", result instanceof JSONObject);
        JSONObject container = (JSONObject) result;
        assertNotNull("The result did not contain the 'data' field", container.get("data"));
        assertTrue("The value of the 'data' fields is not of type JSON Array", container.get("data") instanceof JSONArray);
    }

    private void deleteCreatedAccounts() {
        for (FacebookTestUserAccount createdAccount : createdAccounts) {
            createdAccount.delete();
        }
    }

    private FacebookTestUserAccount createAccount() {
        FacebookTestUserAccount account = facebookStore1.createTestUser(true, "");
        createdAccounts.add(account);
        return account;
    }

}
