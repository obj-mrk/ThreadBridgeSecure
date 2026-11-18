package mrk.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiTokenServiceTest {

    private final ApiTokenService apiTokenService =
            new ApiTokenService();

    @Test
    void shouldGenerateDifferentTokens() {
        String first = apiTokenService.generateToken();
        String second = apiTokenService.generateToken();

        assertNotNull(first);
        assertFalse(first.isBlank());

        assertNotEquals(first, second);
    }

    @Test
    void shouldHashTokenDeterministically() {
        String token = apiTokenService.generateToken();

        String firstHash = apiTokenService.hash(token);
        String secondHash = apiTokenService.hash(token);

        assertEquals(firstHash, secondHash);
        assertNotEquals(token, firstHash);
        assertEquals(64, firstHash.length());
    }
}