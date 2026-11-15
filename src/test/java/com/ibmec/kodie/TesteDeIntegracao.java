package com.ibmec.kodie;

import com.ibmec.kodie.model.Pessoa;
import com.ibmec.kodie.model.TipoVinculo;
import com.ibmec.kodie.service.PessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Base dos testes que sobem a aplicacao inteira.
 *
 * As classes que herdam desta compartilham uma unica aplicacao (o Spring a
 * guarda em cache entre elas), com banco H2 em memoria proprio - os testes
 * nunca tocam a pasta de dados local - e a carga de demonstracao do
 * DataLoader. Testes marcados com @Transactional desfazem ao terminar tudo
 * o que gravaram, inclusive o que passou pelas telas via MockMvc.
 *
 * As propriedades abaixo fixam o administrador e a carga de demonstracao,
 * para o resultado nao depender de variaveis de ambiente da maquina.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:kodie-testes;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false",
        "kodie.dados-demonstracao=true",
        "kodie.admin.login=admin",
        "kodie.admin.senha=kodie2026",
        "kodie.seguranca.exigir-https=false",
        "kodie.backup.diretorio=target/backups-de-teste"
})
@AutoConfigureMockMvc
public abstract class TesteDeIntegracao {

    /** MockMvc faz requisicoes as telas e a API sem abrir uma porta de rede. */
    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected PessoaService pessoaService;

    /** CPFs ficticios e unicos para as pessoas criadas nos testes. */
    private static final AtomicLong PROXIMO_CPF = new AtomicLong(90_000_000_000L);

    /** Entra como o administrador criado pelo UsuarioInicial. */
    protected static RequestPostProcessor comoAdmin() {
        return httpBasic("admin", "kodie2026");
    }

    /** Entra como o usuario de consulta criado pelo DataLoader. */
    protected static RequestPostProcessor comoConsulta() {
        return httpBasic("consulta", "consulta2026");
    }

    /** Pessoa CLT ainda nao gravada, com CPF que nenhuma outra tem. */
    protected static Pessoa novaPessoaClt(String nome, LocalDate entrada) {
        return new Pessoa(nome, String.valueOf(PROXIMO_CPF.incrementAndGet()),
                "teste@exemplo.com", "Cargo de teste", TipoVinculo.CLT, entrada);
    }

    /**
     * Copia os campos de uma pessoa num objeto novo, como chegariam do
     * formulario de edicao ou do PUT da API: o service sempre recebe um
     * objeto separado do que esta gravado no banco.
     */
    protected static Pessoa copiaDe(Pessoa original) {
        Pessoa copia = new Pessoa(original.getNome(), original.getCpf(), original.getEmail(),
                original.getCargo(), original.getTipoVinculo(), original.getDataEntrada());
        copia.setSituacao(original.getSituacao());
        copia.setDataSaida(original.getDataSaida());
        return copia;
    }
}
