package com.jayway.facebooktestjavaapi.testuser;

/**
 * Changes account settings for at test user.
 * <p/>
 * The settings to perform are specified by calling the methods starting with 'new', such as {@link #newName(String)}
 * and {@link #newPassword(String)}.
 * </p>
 * The new settings are applied by calling the {@link #apply()} method.
 */
public interface AccountSettingsChanger
{
    /**
     * Sets the new name of the test user.
     * @param name The new name.
     * @return {@code This} instance.
     */
    AccountSettingsChanger newName(String name);

    /**
     * Sets the new password for the test user.
     * @param password The new password.
     * @return {@code This} instance.
     */
    AccountSettingsChanger newPassword(String password);

    /**
     * Applies the changes.
     */
    void apply();
}
