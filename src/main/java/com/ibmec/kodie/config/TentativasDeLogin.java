package com.ibmec.kodie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protecao contra quem tenta adivinhar senhas (ataque de forca bruta).
 *
 * Depois de LIMITE senhas erradas seguidas para o mesmo login, esse login
 * fica bloqueado por BLOQUEIO: nem a senha certa entra nesse intervalo.
 * Vale para a tela de login e para a API (HTTP Basic), porque as duas passam
 * pelo mesmo Spring Security, que publica um evento a cada tentativa.
 *
 * Por que contar por login, e nao pelo IP de quem tenta: no App Service, o
 * IP chega num cabecalho (X-Forwarded-For) que o proprio atacante consegue
 * forjar. O login, nao. O preco e que alguem pode bloquear de proposito o
 * acesso de outra pessoa por 15 minutos; adivinhar a senha, porem, deixa de
 * ser viavel - 10 tentativas a cada 15 minutos sao menos de mil por dia.
 *
 * Logins que nao existem tambem sao contados e bloqueados, exatamente como
 * os que existem: a resposta nao pode revelar quais logins sao validos.
 *
 * Os registros ficam na memoria. Reiniciar a aplicacao zera a contagem, o
 * que e aceitavel: o objetivo e frear tentativas em serie, nao guardar
 * historico.
 */
@Component
public class TentativasDeLogin {

    private static final Logger log = LoggerFactory.getLogger(TentativasDeLogin.class);

    /** Senhas erradas seguidas que disparam o bloqueio. */
    public static final int LIMITE = 10;

    /** Quanto tempo o login fica bloqueado, contado a partir da ultima senha errada. */
    public static final Duration BLOQUEIO = Duration.ofMinutes(15);

    /** Acima disto, os registros antigos sao descartados para a memoria nao crescer sem fim. */
    private static final int MAXIMO_DE_REGISTROS = 1_000;

    /** Quantas senhas erradas seguidas e quando foi a ultima. */
    private record Registro(int falhas, Instant ultimaFalha) {
    }

    private final Map<String, Registro> porLogin = new ConcurrentHashMap<>();
    private final Clock relogio;

    public TentativasDeLogin() {
        this(Clock.systemUTC());
    }

    /** Usado pelos testes, com um relogio que eles controlam. */
    TentativasDeLogin(Clock relogio) {
        this.relogio = relogio;
    }

    /** Senha errada (ou login inexistente): conta mais uma falha. */
    @EventListener
    public void senhaErrada(AuthenticationFailureBadCredentialsEvent evento) {
        registrarFalha(evento.getAuthentication().getName());
    }

    /** Entrou: a contagem daquele login recomeca do zero. */
    @EventListener
    public void entrou(AuthenticationSuccessEvent evento) {
        porLogin.remove(normalizar(evento.getAuthentication().getName()));
    }

    public boolean estaBloqueado(String login) {
        Registro registro = porLogin.get(normalizar(login));
        return registro != null
                && registro.falhas() >= LIMITE
                && !expirou(registro);
    }

    void registrarFalha(String login) {
        String chave = normalizar(login);
        if (chave.isEmpty()) {
            return;
        }

        // compute() faz ler, somar e gravar de uma vez so: duas tentativas
        // simultaneas nao conseguem se perder uma por cima da outra.
        Registro atualizado = porLogin.compute(chave, (k, anterior) -> {
            // Falhas antigas nao se acumulam: passados 15 minutos, recomeca.
            int falhas = (anterior == null || expirou(anterior)) ? 1 : anterior.falhas() + 1;
            return new Registro(falhas, relogio.instant());
        });

        if (atualizado.falhas() == LIMITE) {
            log.warn(">>> Login '{}' bloqueado por {} minutos depois de {} senhas erradas seguidas",
                    paraOLog(chave), BLOQUEIO.toMinutes(), LIMITE);
        }

        if (porLogin.size() > MAXIMO_DE_REGISTROS) {
            porLogin.values().removeIf(this::expirou);
        }
    }

    private boolean expirou(Registro registro) {
        return relogio.instant().isAfter(registro.ultimaFalha().plus(BLOQUEIO));
    }

    /** O mesmo tratamento que o UsuarioDetailsService da ao login digitado. */
    private static String normalizar(String login) {
        return login == null ? "" : login.trim().toLowerCase();
    }

    /** O login vem de quem tenta entrar: quebras de linha forjariam linhas falsas no log. */
    private static String paraOLog(String texto) {
        return texto.replaceAll("[\\p{Cntrl}]", "_");
    }
}
