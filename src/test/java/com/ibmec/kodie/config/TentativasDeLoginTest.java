package com.ibmec.kodie.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regras do bloqueio por senhas erradas seguidas, sem subir a aplicacao.
 *
 * O relogio e controlado pelo teste: em vez de esperar 15 minutos de
 * verdade, o teste simplesmente avanca o relogio.
 */
class TentativasDeLoginTest {

    /** Relogio que so anda quando o teste manda. */
    static class RelogioDeTeste extends Clock {
        private Instant agora = Instant.parse("2026-11-15T12:00:00Z");

        void avancar(Duration quanto) {
            agora = agora.plus(quanto);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    private final RelogioDeTeste relogio = new RelogioDeTeste();
    private final TentativasDeLogin tentativas = new TentativasDeLogin(relogio);

    private void errarASenha(String login, int vezes) {
        for (int i = 0; i < vezes; i++) {
            tentativas.registrarFalha(login);
        }
    }

    @Test
    void bloqueiaNaDecimaSenhaErradaSeguida() {
        errarASenha("ana", TentativasDeLogin.LIMITE - 1);
        assertThat(tentativas.estaBloqueado("ana")).isFalse();

        errarASenha("ana", 1);
        assertThat(tentativas.estaBloqueado("ana")).isTrue();
    }

    @Test
    void oBloqueioDeUmLoginNaoAtingeOutro() {
        errarASenha("ana", TentativasDeLogin.LIMITE);
        assertThat(tentativas.estaBloqueado("carlos")).isFalse();
    }

    @Test
    void oBloqueioTerminaDepoisDe15Minutos() {
        errarASenha("ana", TentativasDeLogin.LIMITE);

        relogio.avancar(TentativasDeLogin.BLOQUEIO.minusSeconds(1));
        assertThat(tentativas.estaBloqueado("ana")).isTrue();

        relogio.avancar(Duration.ofSeconds(2));
        assertThat(tentativas.estaBloqueado("ana")).isFalse();

        // Passado o bloqueio, a contagem recomeca: uma senha errada nao bloqueia de novo.
        errarASenha("ana", 1);
        assertThat(tentativas.estaBloqueado("ana")).isFalse();
    }

    @Test
    void senhasErradasEspacadasNaoSeAcumulam() {
        errarASenha("ana", TentativasDeLogin.LIMITE - 1);
        relogio.avancar(Duration.ofMinutes(16));
        errarASenha("ana", 1);

        assertThat(tentativas.estaBloqueado("ana")).isFalse();
    }

    @Test
    void entrarComASenhaCertaZeraAContagem() {
        errarASenha("ana", TentativasDeLogin.LIMITE - 1);

        tentativas.entrou(new AuthenticationSuccessEvent(
                UsernamePasswordAuthenticationToken.authenticated("ana", null, List.of())));

        errarASenha("ana", TentativasDeLogin.LIMITE - 1);
        assertThat(tentativas.estaBloqueado("ana")).isFalse();
    }

    @Test
    void maiusculasEEspacosContamComoOMesmoLogin() {
        errarASenha(" Ana ", TentativasDeLogin.LIMITE);
        assertThat(tentativas.estaBloqueado("ana")).isTrue();
    }

    @Test
    void loginEmBrancoNaoEContado() {
        errarASenha("", TentativasDeLogin.LIMITE);
        assertThat(tentativas.estaBloqueado("")).isFalse();
    }
}
