package com.ibmec.kodie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe a aplicacao inteira: se uma migracao do Flyway falhar, ou se as
 * entidades nao baterem com as tabelas (ddl-auto=validate), este teste falha.
 *
 * Usa um banco em memoria proprio, para que rodar os testes nunca toque na
 * pasta de dados local.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testes;DB_CLOSE_DELAY=-1")
class KodieApplicationTests {

	@Test
	void contextLoads() {
	}

}
