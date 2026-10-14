package com.ibmec.kodie.service;

import com.ibmec.kodie.model.*;
import com.ibmec.kodie.repository.OcorrenciaRepository;
import com.ibmec.kodie.repository.PeriodoAquisitivoRepository;
import com.ibmec.kodie.repository.PessoaRepository;
import com.ibmec.kodie.repository.UsuarioRepository;
import com.ibmec.kodie.service.backup.ArquivoDeBackup;
import com.ibmec.kodie.service.backup.ArquivoDeBackup.*;
import com.ibmec.kodie.service.backup.BackupSalvo;
import com.ibmec.kodie.service.backup.ResumoDeBackup;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backup e restauracao completa dos dados do sistema.
 *
 * Um backup e um arquivo JSON com todos os registros: usuarios, pessoas,
 * ocorrencias e periodos aquisitivos. Ele e gerado de tres formas: a pedido
 * do administrador, automaticamente todos os dias, e sempre logo antes de
 * uma restauracao - para que restaurar o arquivo errado nunca seja
 * irreversivel.
 *
 * A restauracao substitui TODOS os dados pelos do arquivo, preservando os
 * ids. Ela so comeca depois que o arquivo inteiro foi validado; se algo
 * falhar durante a gravacao, a transacao desfaz tudo e os dados atuais
 * permanecem intactos.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    /** kodie-backup-20261014-153000-123-manual.json */
    private static final Pattern NOME_VALIDO =
            Pattern.compile("kodie-backup-\\d{8}-\\d{6}-\\d{3}-([a-z-]+)\\.json");
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Pattern HASH_BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$.{53}");

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final PeriodoAquisitivoRepository periodoRepository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final JsonMapper json;
    private final Validator validador;
    private final ObjectProvider<Flyway> flyway;
    private final EntityManager entityManager;
    private final Path diretorio;
    private final int quantidadeAutomaticaMantida;

    public BackupService(UsuarioRepository usuarioRepository,
                         PessoaRepository pessoaRepository,
                         OcorrenciaRepository ocorrenciaRepository,
                         PeriodoAquisitivoRepository periodoRepository,
                         JdbcTemplate jdbc,
                         PlatformTransactionManager gerenciadorDeTransacoes,
                         JsonMapper json,
                         Validator validador,
                         ObjectProvider<Flyway> flyway,
                         EntityManager entityManager,
                         @Value("${kodie.backup.diretorio}") String diretorio,
                         @Value("${kodie.backup.manter}") int quantidadeAutomaticaMantida) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.periodoRepository = periodoRepository;
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(gerenciadorDeTransacoes);
        this.json = json;
        this.validador = validador;
        this.flyway = flyway;
        this.entityManager = entityManager;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.quantidadeAutomaticaMantida = quantidadeAutomaticaMantida;
    }

    // ==================================================================
    // Geracao
    // ==================================================================

    /** Fotografa o estado atual de todos os dados. */
    public ArquivoDeBackup montar(String motivo) {
        List<UsuarioRegistro> usuarios = usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .map(u -> new UsuarioRegistro(u.getId(), u.getLogin(), u.getNome(), u.getSenhaHash(),
                        u.getPerfil(), u.isAtivo(), u.getCriadoEm()))
                .toList();

        List<PessoaRegistro> pessoas = pessoaRepository.findAll().stream()
                .sorted(Comparator.comparing(Pessoa::getId))
                .map(p -> new PessoaRegistro(p.getId(), p.getNome(), p.getCpf(), p.getEmail(), p.getCargo(),
                        p.getTipoVinculo(), p.getSituacao(), p.getDataEntrada(), p.getDataSaida()))
                .toList();

        List<OcorrenciaRegistro> ocorrencias = ocorrenciaRepository.findAll().stream()
                .sorted(Comparator.comparing(Ocorrencia::getId))
                .map(o -> new OcorrenciaRegistro(o.getId(), o.getTipo(), o.getDataInicio(), o.getDataFim(),
                        o.getSituacao(), o.getObservacao(),
                        o.getPessoa() == null ? null : o.getPessoa().getId()))
                .toList();

        List<PeriodoRegistro> periodos = periodoRepository.findAll().stream()
                .sorted(Comparator.comparing(PeriodoAquisitivo::getId))
                .map(p -> new PeriodoRegistro(p.getId(), p.getPessoa().getId(), p.getNumero(), p.getDataInicio(),
                        p.getDataFim(), p.getDiasDeDireito(), p.getDataLimiteGozo()))
                .toList();

        return new ArquivoDeBackup(
                ArquivoDeBackup.FORMATO_ATUAL,
                LocalDateTime.now(),
                versaoDoSchema(),
                motivo,
                new Contagens(usuarios.size(), pessoas.size(), ocorrencias.size(), periodos.size()),
                usuarios, pessoas, ocorrencias, periodos);
    }

    public byte[] gerarJson(String motivo) {
        return json.writerWithDefaultPrettyPrinter().writeValueAsBytes(montar(motivo));
    }

    /** Grava um backup na pasta de backups do servidor e devolve o nome do arquivo. */
    public String salvarNoServidor(String motivo) {
        byte[] conteudo = gerarJson(motivo);
        String nome = "kodie-backup-" + LocalDateTime.now().format(CARIMBO) + "-" + motivo + ".json";
        gravar(diretorio.resolve(nome), conteudo);
        log.info(">>> Backup gravado: {} ({} bytes)", nome, conteudo.length);
        return nome;
    }

    /**
     * Backup automatico diario. O horario segue o fuso de Sao Paulo mesmo que
     * o servidor esteja configurado em UTC, como e comum em VPS.
     */
    @Scheduled(cron = "${kodie.backup.agendamento}", zone = "America/Sao_Paulo")
    public void backupAutomatico() {
        try {
            salvarNoServidor("automatico");
            aplicarRetencao();
        } catch (RuntimeException e) {
            // Uma falha aqui nao pode derrubar o agendador: o proximo dia tenta de novo.
            log.error(">>> Falha no backup automatico", e);
        }
    }

    /** Mantem apenas os N backups automaticos mais recentes. Os demais tipos nunca sao apagados sozinhos. */
    private void aplicarRetencao() {
        List<BackupSalvo> automaticos = listar().stream()
                .filter(b -> b.motivo().equals("automatico"))
                .toList();                                   // listar() ja vem do mais novo para o mais antigo
        automaticos.stream()
                .skip(quantidadeAutomaticaMantida)
                .forEach(b -> excluir(b.nome()));
    }

    // ==================================================================
    // Arquivos no servidor
    // ==================================================================

    /** Backups guardados no servidor, do mais recente para o mais antigo. */
    public List<BackupSalvo> listar() {
        if (!Files.isDirectory(diretorio)) {
            return List.of();
        }
        try (Stream<Path> arquivos = Files.list(diretorio)) {
            return arquivos
                    .filter(p -> NOME_VALIDO.matcher(p.getFileName().toString()).matches())
                    .map(this::descrever)
                    .sorted(Comparator.comparing(BackupSalvo::nome).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel listar a pasta de backups", e);
        }
    }

    public byte[] lerArquivo(String nome) {
        try {
            return Files.readAllBytes(caminhoSeguro(nome));
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o backup " + nome, e);
        }
    }

    public void excluir(String nome) {
        try {
            Files.deleteIfExists(caminhoSeguro(nome));
            log.info(">>> Backup excluido: {}", nome);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel excluir o backup " + nome, e);
        }
    }

    /**
     * Guarda no servidor um arquivo enviado pelo administrador. Ele so e
     * aceito se for um backup legivel; a validacao completa acontece na
     * tela de confirmacao, antes de qualquer restauracao.
     */
    public String receberEnvio(byte[] conteudo) {
        try {
            ArquivoDeBackup lido = json.readValue(conteudo, ArquivoDeBackup.class);
            if (lido == null || lido.formato() < 1) {
                throw new IllegalArgumentException("O arquivo nao e um backup do KODIE");
            }
        } catch (JacksonException e) {
            throw new IllegalArgumentException("O arquivo nao e um backup do KODIE valido: JSON ilegivel "
                    + "ou com valores que o sistema nao reconhece");
        }
        String nome = "kodie-backup-" + LocalDateTime.now().format(CARIMBO) + "-enviado.json";
        gravar(diretorio.resolve(nome), conteudo);
        return nome;
    }

    /**
     * Resolve o nome dentro da pasta de backups, recusando qualquer coisa
     * fora do padrao - inclusive tentativas como "../../etc/passwd".
     */
    private Path caminhoSeguro(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) {
            throw new IllegalArgumentException("Nome de backup invalido");
        }
        Path caminho = diretorio.resolve(nome).normalize();
        if (!caminho.startsWith(diretorio)) {
            throw new IllegalArgumentException("Nome de backup invalido");
        }
        if (!Files.exists(caminho)) {
            throw new RecursoNaoEncontradoException("Backup nao encontrado: " + nome);
        }
        return caminho;
    }

    private BackupSalvo descrever(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        var m = NOME_VALIDO.matcher(nome);
        String motivo = m.matches() ? m.group(1) : "desconhecido";
        try {
            LocalDateTime criadoEm = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(arquivo).toInstant(), ZoneId.systemDefault());
            return new BackupSalvo(nome, motivo, Files.size(arquivo), criadoEm);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** O backup contem dados pessoais e hashes de senha: so o dono do processo le o arquivo. */
    private void gravar(Path destino, byte[] conteudo) {
        try {
            Files.createDirectories(diretorio);
            Files.write(destino, conteudo);
            try {
                Files.setPosixFilePermissions(destino, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignorado) {
                // Windows: sem permissoes POSIX
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar o backup em " + destino, e);
        }
    }

    // ==================================================================
    // Validacao
    // ==================================================================

    /**
     * Le um backup do servidor e lista tudo o que impediria restaura-lo com
     * seguranca. Nada e alterado aqui.
     */
    public ResumoDeBackup analisar(String nome) {
        ArquivoDeBackup conteudo;
        try {
            conteudo = json.readValue(lerArquivo(nome), ArquivoDeBackup.class);
        } catch (JacksonException e) {
            return new ResumoDeBackup(nome, null, List.of("Arquivo ilegivel: nao e um JSON de backup do KODIE"));
        }
        return new ResumoDeBackup(nome, conteudo, verificar(conteudo));
    }

    private List<String> verificar(ArquivoDeBackup b) {
        List<String> problemas = new ArrayList<>();

        if (b.formato() != ArquivoDeBackup.FORMATO_ATUAL) {
            problemas.add("Formato " + b.formato() + " nao suportado por esta versao do sistema (esperado "
                    + ArquivoDeBackup.FORMATO_ATUAL + ")");
            return problemas;
        }
        if (b.usuarios() == null || b.pessoas() == null || b.ocorrencias() == null || b.periodosAquisitivos() == null) {
            problemas.add("Arquivo incompleto: faltam secoes de dados");
            return problemas;
        }

        // Sem um administrador ativo, ninguem conseguiria entrar depois da restauracao.
        boolean temAdmin = b.usuarios().stream()
                .anyMatch(u -> u.ativo() && u.perfil() == PerfilAcesso.ADMIN);
        if (!temAdmin) {
            problemas.add("O backup nao tem nenhum administrador ativo: ninguem conseguiria entrar no sistema depois");
        }

        repetidos(b.usuarios(), UsuarioRegistro::id, "Usuario com id repetido: ", problemas);
        repetidos(b.usuarios(), UsuarioRegistro::login, "Login repetido: ", problemas);
        repetidos(b.pessoas(), PessoaRegistro::id, "Pessoa com id repetido: ", problemas);
        repetidos(b.pessoas(), PessoaRegistro::cpf, "CPF repetido: ", problemas);
        repetidos(b.ocorrencias(), OcorrenciaRegistro::id, "Ocorrencia com id repetido: ", problemas);
        repetidos(b.periodosAquisitivos(), PeriodoRegistro::id, "Periodo aquisitivo com id repetido: ", problemas);

        Set<Long> idsDePessoas = b.pessoas().stream().map(PessoaRegistro::id).collect(Collectors.toSet());

        for (UsuarioRegistro u : b.usuarios()) {
            if (u.id() == null) problemas.add("Usuario sem id: " + u.login());
            if (u.senhaHash() == null || !HASH_BCRYPT.matcher(u.senhaHash()).matches()) {
                problemas.add("Usuario " + u.login() + ": senha em formato invalido (esperado hash BCrypt)");
            }
            Usuario entidade = new Usuario(u.login(), u.nome(), u.senhaHash(), u.perfil());
            validar(entidade, "Usuario " + u.login(), problemas);
        }

        for (PessoaRegistro p : b.pessoas()) {
            if (p.id() == null) problemas.add("Pessoa sem id: " + p.nome());
            Pessoa entidade = new Pessoa(p.nome(), p.cpf(), p.email(), p.cargo(), p.tipoVinculo(), p.dataEntrada());
            entidade.setSituacao(p.situacao());
            entidade.setDataSaida(p.dataSaida());
            validar(entidade, "Pessoa " + p.nome(), problemas);
        }

        for (OcorrenciaRegistro o : b.ocorrencias()) {
            if (o.id() == null) problemas.add("Ocorrencia sem id");
            if (o.pessoaId() != null && !idsDePessoas.contains(o.pessoaId())) {
                problemas.add("Ocorrencia " + o.id() + " aponta para a pessoa " + o.pessoaId() + ", que nao esta no backup");
            }
            if (o.dataInicio() != null && o.dataFim() != null && o.dataFim().isBefore(o.dataInicio())) {
                problemas.add("Ocorrencia " + o.id() + ": data de fim anterior a de inicio");
            }
            Ocorrencia entidade = new Ocorrencia(o.tipo(), o.dataInicio(), o.dataFim(), o.situacao(), o.observacao(), null);
            validar(entidade, "Ocorrencia " + o.id(), problemas);
        }

        for (PeriodoRegistro p : b.periodosAquisitivos()) {
            if (p.id() == null || p.numero() == null || p.dataInicio() == null || p.dataFim() == null
                    || p.diasDeDireito() == null || p.dataLimiteGozo() == null) {
                problemas.add("Periodo aquisitivo " + p.id() + " com campos faltando");
            }
            if (p.pessoaId() == null || !idsDePessoas.contains(p.pessoaId())) {
                problemas.add("Periodo aquisitivo " + p.id() + " aponta para a pessoa " + p.pessoaId() + ", que nao esta no backup");
            }
        }
        return problemas;
    }

    /** Reaproveita as mesmas anotacoes de validacao das entidades (@NotBlank, @Email, @Pattern...). */
    private void validar(Object entidade, String rotulo, List<String> problemas) {
        for (ConstraintViolation<Object> violacao : validador.validate(entidade)) {
            problemas.add(rotulo + ": " + violacao.getMessage());
        }
    }

    private static <T, K> void repetidos(List<T> itens, Function<T, K> chave, String mensagem, List<String> problemas) {
        Set<K> vistos = new HashSet<>();
        for (T item : itens) {
            K valor = chave.apply(item);
            if (valor != null && !vistos.add(valor)) {
                problemas.add(mensagem + valor);
            }
        }
    }

    // ==================================================================
    // Restauracao
    // ==================================================================

    /**
     * Substitui todos os dados pelos do backup indicado.
     *
     * @return nome do backup de seguranca gravado antes da substituicao
     */
    public String restaurar(String nome) {
        ResumoDeBackup resumo = analisar(nome);
        if (!resumo.isRestauravel()) {
            throw new IllegalArgumentException("O backup nao pode ser restaurado: "
                    + String.join("; ", resumo.problemas()));
        }

        // Rede de seguranca: o estado atual vira um backup antes de ser substituido.
        String seguranca = salvarNoServidor("antes-da-restauracao");

        ArquivoDeBackup b = resumo.conteudo();
        try {
            transacao.executeWithoutResult(status -> substituirDados(b));
        } catch (DataAccessException e) {
            // A transacao ja foi desfeita: os dados atuais continuam intactos.
            Throwable causa = NestedExceptionUtils.getMostSpecificCause(e);
            log.error(">>> Restauracao de {} recusada pelo banco; nada foi alterado", nome, e);
            throw new IllegalArgumentException("O banco recusou os dados do backup e nada foi alterado. Detalhe: "
                    + causa.getMessage());
        }
        reiniciarSequencias();
        entityManager.clear();

        log.warn(">>> Dados restaurados a partir de {} ({} pessoas, {} ocorrencias, {} usuarios). Backup de seguranca: {}",
                nome, b.pessoas().size(), b.ocorrencias().size(), b.usuarios().size(), seguranca);
        return seguranca;
    }

    /** Roda dentro de uma unica transacao: ou tudo e gravado, ou nada muda. */
    private void substituirDados(ArquivoDeBackup b) {
        // Ordem imposta pelas chaves estrangeiras: primeiro os filhos.
        jdbc.update("delete from ocorrencia");
        jdbc.update("delete from periodo_aquisitivo");
        jdbc.update("delete from pessoa");
        jdbc.update("delete from usuario");

        for (UsuarioRegistro u : b.usuarios()) {
            jdbc.update("insert into usuario (id, login, nome, senha_hash, perfil, ativo, criado_em) values (?, ?, ?, ?, ?, ?, ?)",
                    u.id(), u.login(), u.nome(), u.senhaHash(), u.perfil().name(), u.ativo(),
                    u.criadoEm() != null ? u.criadoEm() : LocalDateTime.now());
        }
        for (PessoaRegistro p : b.pessoas()) {
            jdbc.update("insert into pessoa (id, nome, cpf, email, cargo, tipo_vinculo, situacao, data_entrada, data_saida) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    p.id(), p.nome(), p.cpf(), p.email(), p.cargo(), p.tipoVinculo().name(), p.situacao().name(),
                    p.dataEntrada(), p.dataSaida());
        }
        for (OcorrenciaRegistro o : b.ocorrencias()) {
            jdbc.update("insert into ocorrencia (id, tipo, data_inicio, data_fim, situacao, observacao, pessoa_id) values (?, ?, ?, ?, ?, ?, ?)",
                    o.id(), o.tipo().name(), o.dataInicio(), o.dataFim(), o.situacao().name(), o.observacao(), o.pessoaId());
        }
        for (PeriodoRegistro p : b.periodosAquisitivos()) {
            jdbc.update("insert into periodo_aquisitivo (id, pessoa_id, numero, data_inicio, data_fim, dias_de_direito, data_limite_gozo) values (?, ?, ?, ?, ?, ?, ?)",
                    p.id(), p.pessoaId(), p.numero(), p.dataInicio(), p.dataFim(), p.diasDeDireito(), p.dataLimiteGozo());
        }
    }

    /**
     * Como os ids foram gravados explicitamente, o contador automatico de
     * cada tabela precisa continuar depois do maior id restaurado - senao o
     * proximo cadastro tentaria reutilizar um id existente.
     *
     * Roda depois da transacao porque, no H2, comandos ALTER confirmam a
     * transacao em andamento.
     */
    private void reiniciarSequencias() {
        for (String tabela : List.of("usuario", "pessoa", "ocorrencia", "periodo_aquisitivo")) {
            Long maior = jdbc.queryForObject("select coalesce(max(id), 0) from " + tabela, Long.class);
            jdbc.execute("alter table " + tabela + " alter column id restart with " + (maior + 1));
        }
    }

    private String versaoDoSchema() {
        Flyway f = flyway.getIfAvailable();
        if (f == null || f.info().current() == null) {
            return "desconhecida";
        }
        return f.info().current().getVersion().getVersion();
    }
}
