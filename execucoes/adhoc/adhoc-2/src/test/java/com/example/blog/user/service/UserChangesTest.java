package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserChangesTest {

    @Test
    void deveSerVaziaQuandoNenhumCampoFoiInformado() {
        assertThat(new UserChanges(null, null, null, false, null, false, null).isEmpty()).isTrue();
    }

    @Test
    void naoDeveSerVaziaQuandoBioFoiEnviadaComoNula() {
        assertThat(new UserChanges(null, null, null, true, null, false, null).isEmpty()).isFalse();
    }

    @Test
    void naoDeveSerVaziaQuandoEmailFoiInformado() {
        assertThat(new UserChanges("a@example.test", null, null, false, null, false, null).isEmpty()).isFalse();
    }
}
