package com.siso.backend.comment;

import com.siso.backend.anon.NicknameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NicknameGeneratorTest {

    @Test
    void generate_isDeterministicForSameAnonId() {
        String anonId = "550e8400-e29b-41d4-a716-446655440000";

        assertThat(NicknameGenerator.generate(anonId)).isEqualTo(NicknameGenerator.generate(anonId));
    }

    @Test
    void generate_differsForDifferentAnonIds() {
        String a = NicknameGenerator.generate("550e8400-e29b-41d4-a716-446655440000");
        String b = NicknameGenerator.generate("11111111-1111-1111-1111-111111111111");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void generate_endsWithFourCharSuffixFromAnonId() {
        String nickname = NicknameGenerator.generate("550e8400-e29b-41d4-a716-446655440000");

        assertThat(nickname).endsWith("550e");
    }
}
