package com.ibmec.kodie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O comportamento do perfil de producao (kodie.seguranca.exigir-https=true).
 *
 * Aplicacao separada, com banco proprio, porque a propriedade muda a cadeia
 * de seguranca. No servidor, quem diz se o pedido chegou por https e o
 * cabecalho X-Forwarded-Proto do App Service (lido pelo ForwardedHeaderFilter,
 * que troca o esquema do pedido para https); aqui, o proprio endereco faz o papel.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:kodie-https;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false",
        "kodie.seguranca.exigir-https=true",
        "kodie.backup.diretorio=target/backups-de-teste"
})
@AutoConfigureMockMvc
class HttpsObrigatorioTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void pedidoPorHttpERedirecionadoParaHttps() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost/login"));
    }

    @Test
    void pedidoDiretoNaPortaInternaTambemERedirecionadoSemErro() throws Exception {
        // Sem o mapeamento para a porta 443, isto dava erro 500
        // ("HTTP Port '8081' does not have a corresponding HTTPS Port").
        // ":443" explicito e o mesmo endereco que sem a porta.
        mvc.perform(get("http://localhost:8081/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://localhost:443/login"));
    }

    @Test
    void pedidoPorHttpsSegueNormalEAtivaOHsts() throws Exception {
        mvc.perform(get("https://localhost/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }
}
