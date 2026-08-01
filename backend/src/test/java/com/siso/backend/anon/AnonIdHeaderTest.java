package com.siso.backend.anon;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnonIdHeaderTest {

    private final AnonIdSigner signer = new AnonIdSigner("test-secret");
    private static final String ANON_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void parseAndVerify_returnsUuidForValidSignature() {
        String signature = signer.sign(ANON_ID);

        assertThat(AnonIdHeader.parseAndVerify(ANON_ID, signature, signer)).isEqualTo(UUID.fromString(ANON_ID));
    }

    @Test
    void parseAndVerify_rejectsForgedAnonIdWithoutValidSignature() {
        assertThatThrownBy(() -> AnonIdHeader.parseAndVerify(ANON_ID, "not-a-real-signature", signer))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void parseAndVerify_rejectsMissingSignature() {
        assertThatThrownBy(() -> AnonIdHeader.parseAndVerify(ANON_ID, null, signer))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void parseAndVerify_rejectsSignatureIssuedForDifferentAnonId() {
        String signatureForOtherId = signer.sign("22222222-2222-2222-2222-222222222222");

        assertThatThrownBy(() -> AnonIdHeader.parseAndVerify(ANON_ID, signatureForOtherId, signer))
                .isInstanceOf(ResponseStatusException.class);
    }
}
