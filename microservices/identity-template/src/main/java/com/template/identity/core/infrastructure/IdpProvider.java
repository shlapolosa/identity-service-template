package com.template.identity.core.infrastructure;

import java.util.Map;

public interface IdpProvider {
    
    String createUser(String email, String password, Map<String, Object> metadata);
    
    void updateUser(String externalId, Map<String, Object> updates);
    
    void deleteUser(String externalId);
    
    boolean verifyPassword(String email, String password);
    
    String generatePasswordResetToken(String email);
    
    void resetPassword(String token, String newPassword);
    
    void verifyEmail(String externalId);
    
    Map<String, Object> getUserInfo(String externalId);
    
    String refreshToken(String refreshToken);
    
    void logout(String externalId);
    
    boolean isEmailVerified(String externalId);
    
    void enableMfa(String externalId, String method);
    
    void disableMfa(String externalId);
}