package com.siso.backend.anon;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnonIdSignerTest {

    private final AnonIdSigner signer = new AnonIdSigner("test-secret");

    @Test
    void verify_acceptsSignatureFromSameSecret() {
        String anonId = "11111111-1111-1111-1111-111111111111";
        String signature = signer.sign(anonId);

        assertThat(signer.verify(anonId, signature)).isTrue();
    }

    @Test
    void verify_rejectsSignatureFromDifferentSecret() {
        String anonId = "11111111-1111-1111-1111-111111111111";
        String forgedSignature = new AnonIdSigner("attacker-guessed-secret").sign(anonId);

        assertThat(signer.verify(anonId, forgedSignature)).isFalse();
    }

    @Test
    void verify_rejectsSignatureForDifferentAnonId() {
        String signature = signer.sign("11111111-1111-1111-1111-111111111111");

        assertThat(signer.verify("22222222-2222-2222-2222-222222222222", signature)).isFalse();
    }

    @Test
    void verify_rejectsNullSignature() {
        assertThat(signer.verify("11111111-1111-1111-1111-111111111111", null)).isFalse();
    }
}
