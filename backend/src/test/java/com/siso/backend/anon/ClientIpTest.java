package com.siso.backend.anon;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpTest {

    private final AnonIdSigner signer = new AnonIdSigner("test-secret");

    private MockHttpServletRequest request(String ip, String sig) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        if (ip != null) req.addHeader("X-Client-Ip", ip);
        if (sig != null) req.addHeader("X-Client-Ip-Sig", sig);
        return req;
    }

    @Test
    void signedHeader_isTrusted() {
        String sig = signer.sign(ClientIp.SIGN_PREFIX + "1.2.3.4");
        assertThat(ClientIp.resolve(request("1.2.3.4", sig), signer)).isEqualTo("1.2.3.4");
    }

    @Test
    void unsignedHeader_fallsBackToRemoteAddr() {
        assertThat(ClientIp.resolve(request("1.2.3.4", null), signer)).isEqualTo("10.0.0.1");
    }

    @Test
    void signatureForAnotherIp_fallsBackToRemoteAddr() {
        String sig = signer.sign(ClientIp.SIGN_PREFIX + "5.6.7.8");
        assertThat(ClientIp.resolve(request("1.2.3.4", sig), signer)).isEqualTo("10.0.0.1");
    }

    @Test
    void anonIdSignatureCannotBeReusedAsIpSignature() {
        // 익명 ID 서명과 IP 서명이 같은 키라도 접두어가 달라 서로 대신 쓸 수 없다
        String sig = signer.sign("1.2.3.4");
        assertThat(ClientIp.resolve(request("1.2.3.4", sig), signer)).isEqualTo("10.0.0.1");
    }

    @Test
    void noHeader_usesRemoteAddr() {
        assertThat(ClientIp.resolve(request(null, null), signer)).isEqualTo("10.0.0.1");
    }
}
