# Sistema KODIE

Sistema de gestão de pessoas para a KODIE Academy, desenvolvido para a disciplina de
Projeto Back-end do Ibmec.

**Integrantes:** Guilherme Schütz · Leonardo Farias
**Matrícula:** _(preencher)_
**Professor:** Luis Fernando Barreto
**Cliente:** Marina Tiago — KODIE Academy

---

## Como rodar

```bash
./mvnw spring-boot:run
```

| Recurso | Endereço |
|---|---|
| Aplicação (redireciona para o cadastro) | http://localhost:8080 |
| Página de pessoas | http://localhost:8080/pessoas/pagina |
| Console H2 | http://localhost:8080/h2-console |

Dados de conexão do H2: URL `jdbc:h2:mem:kodiedb`, usuário `sa`, senha em branco.

O banco é em memória: ele é recriado a cada inicialização e populado pelo `DataLoader`
com dados fictícios.

**Requisitos:** JDK 25 ou superior. O Maven não precisa estar instalado — o wrapper
(`./mvnw`) baixa a versão correta sozinho.

---

## Modelagem

Duas entidades, com relacionamento **um-para-muitos** unidirecional:

```
                    ┌── N  Ocorrencia         (FK pessoa_id, aceita nulo)
Pessoa  1 ──────────┤
                    └── N  PeriodoAquisitivo  (FK pessoa_id, obrigatório)
```

**Pessoa** é o cadastro central descrito na seção 2.1 do documento de decisões: uma única
base alimenta todas as consultas. Os dados de vínculo (tipo, datas e situação) são campos
desta mesma entidade, já que no MVP uma pessoa possui um único vínculo vigente.

**Ocorrência** reúne as cinco categorias previstas na seção 2.2 — férias, folgas, licenças,
ausências e trabalho em feriados — numa só entidade, porque todas compartilham a mesma
estrutura. A chave estrangeira fica deste lado, o lado "muitos". O campo `pessoa` aceita
nulo: feriados institucionais valem para o calendário, não para uma pessoa específica.

O relacionamento é **unidirecional** — apenas `Ocorrencia` conhece `Pessoa`. Isso evita o
laço infinito na serialização do JSON. A contagem de ocorrências exibida na listagem é
montada pelo Service com `countByPessoaId` e entregue pronta ao template.

---

## Endpoints

### Pessoas

| Verbo | Caminho | Resposta |
|---|---|---|
| POST | `/pessoas` | 201 · 400 se inválido ou CPF repetido |
| GET | `/pessoas` | 200 |
| GET | `/pessoas/{id}` | 200 · 404 |
| PUT | `/pessoas/{id}` | 200 · 404 · 400 |
| DELETE | `/pessoas/{id}` | 204 · 404 |
| GET | `/pessoas/{id}/ocorrencias` | 200 · **404 se a pessoa não existir** |

### Ocorrências

| Verbo | Caminho | Resposta |
|---|---|---|
| POST | `/ocorrencias` | 201 · 400 · 404 se a pessoa informada não existir |
| GET | `/ocorrencias` | 200 |
| GET | `/ocorrencias/{id}` | 200 · 404 |
| PUT | `/ocorrencias/{id}` | 200 · 404 · 400 |
| DELETE | `/ocorrencias/{id}` | 204 · 404 |

### Férias

| Verbo | Caminho | Resposta |
|---|---|---|
| GET | `/ferias` | 200 · situação de todo o cadastro |
| GET | `/ferias/pessoa/{id}` | 200 · 404 se a pessoa não existir |

### Páginas web

| Caminho | Descrição |
|---|---|
| `/pessoas/pagina` | Lista de pessoas e formulário de cadastro |
| `/pessoas/pagina/{id}` | Ficha individual: dados, períodos aquisitivos e ocorrências |
| `/ferias/painel` | Painel de férias nos quatro estados |

Sobre `GET /pessoas/{id}/ocorrencias`: um id inexistente devolve **404**, não uma lista
vazia com 200. A verificação é feita no Service com `existsById` antes da consulta — não
encontrar ocorrências é diferente de a pessoa não existir.

---

## Validações

### Pessoa

| Campo | Anotação | O que impede |
|---|---|---|
| nome | `@NotBlank` `@Size(3,120)` | Nome vazio ou fora do tamanho |
| cpf | `@NotBlank` `@Pattern(\d{11})` | CPF vazio ou com pontos, traços ou tamanho errado |
| email | `@NotBlank` `@Email` `@Size(max=120)` | E-mail vazio ou malformado |
| cargo | `@NotBlank` `@Size(max=80)` | Cargo vazio ou longo demais |
| tipoVinculo | `@NotNull` | Pessoa sem tipo de vínculo definido |
| situacao | `@NotNull` | Pessoa sem situação definida |
| dataEntrada | `@NotNull` `@PastOrPresent` | Data ausente ou no futuro |

### Ocorrência

| Campo | Anotação | O que impede |
|---|---|---|
| tipo | `@NotNull` | Ocorrência sem categoria |
| dataInicio | `@NotNull` | Período sem início |
| dataFim | `@NotNull` | Período sem fim |
| situacao | `@NotNull` | Ocorrência sem situação |
| observacao | `@Size(max=255)` | Texto longo demais para a coluna |

Cada campo obrigatório tem também `@Column(nullable = false)`. Não é redundância: a
anotação de validação protege a aplicação e gera a mensagem para o usuário, enquanto a
restrição de coluna protege o banco — inclusive contra um INSERT feito direto pelo
console do H2.

Toda validação reprovada devolve **400** com as mensagens em português, uma por campo,
graças ao `ManipuladorDeErros` (`@RestControllerAdvice`). Sem essa classe, o Spring
devolveria 500.

---

## Regras de negócio

Implementadas no **Service**, porque dependem de consulta ao banco — uma anotação só
enxerga o valor de um campo isolado e não teria como saber o que já existe cadastrado.

**1. CPF único no cadastro central** — `existsByCpf` na criação e `existsByCpfAndIdNot`
na atualização. A variante com `AndIdNot` existe porque, ao atualizar, o valor repetido
só é problema se pertencer a outra pessoa; sem ela, salvar sem alterar o CPF acusaria
duplicata contra o próprio registro.

**2. Ocorrências não podem se sobrepor** — a mesma pessoa não pode ter duas ocorrências
ativas no mesmo intervalo de datas. Ocorrências canceladas são ignoradas na verificação.

**3. Coerência entre situação e data de saída** — pessoa desligada exige data de saída,
data de saída exige situação "Desligado", e a saída não pode anteceder a entrada.

**4. Pessoa desligada não recebe novas ocorrências.**

**5. Data de fim não pode anteceder a data de início.**

Todas devolvem 400 com mensagem explicativa. Na página web, a mensagem aparece ao lado
do campo correspondente, e não como tela de erro.

---

## Controle de férias

Implementado conforme o modelo escolhido na seção 2.3 do documento de decisões:
**períodos aquisitivos como registros**.

A cada doze meses completos de vínculo nasce um registro `PeriodoAquisitivo` com data de
início, data de fim, dias de direito e prazo final para gozo (doze meses após o fim do
período aquisitivo). O período em curso não é criado — ele só nasce quando os doze meses
se completam.

**O saldo não é armazenado.** Ele é calculado no momento da consulta, somando as
ocorrências do tipo `FERIAS` não canceladas e abatendo-as dos períodos. Assim os períodos
se acumulam naturalmente, o histórico permanece visível e nenhum dado é sobrescrito — que
era precisamente o defeito da alternativa descartada na seção 2.3.

**Não existe rotina periódica.** Os períodos faltantes são criados sob demanda, de forma
idempotente, quando a situação da pessoa é consultada. Consultar várias vezes não duplica
nada.

### Imputação dos dias gozados

As férias gozadas abatem sempre do **período mais antigo com saldo**. A ordem importa: o
período mais antigo é o que está mais perto de vencer. Abater pelo mais recente deixaria
intacto justamente o saldo em risco e produziria um alerta falso.

### Os quatro estados do painel

| Estado | Critério |
|---|---|
| **Vencido** | Existe período com saldo cujo prazo final de gozo já passou |
| **Próximo do vencimento** | Existe período com saldo vencendo dentro da antecedência configurada |
| **Regular** | Nenhum prazo em risco |
| **Sem regra CLT aplicável** | Vínculo MEI, PJ ou voluntário |

A antecedência do alerta é configurável em `application.properties`:

```properties
kodie.ferias.dias-alerta-vencimento=90
```

Definida em **90 dias** com o cliente. Alterar não exige recompilação.

### Caso de validação

O exemplo dado pela cliente na questão 7 é reproduzido pelo `DataLoader` e confere:

| | Esperado | Calculado |
|---|---|---|
| Admissão | 04/12/2024 | 04/12/2024 |
| Fim do período aquisitivo | 03/12/2025 | 03/12/2025 |
| Fim do período concessivo | 03/12/2026 | 03/12/2026 |
| Dias gozados (julho/2026) | 15 | 15 |
| Saldo | 15 | 15 |
| Situação | — | Próximo do vencimento |

### Premissas a confirmar com a cliente

- **Afastamento não suspende o período aquisitivo** — ele continua contando normalmente.
  Adequado para licenças curtas; a CLT prevê tratamento diferente para afastamentos longos.
  Esse é um dos pontos listados como pendentes na seção 5 do documento de decisões.
- **Dias de direito fixos em 30 por período.** A CLT reduz esse número conforme as faltas
  injustificadas no período; essa proporcionalidade não foi implementada.
- Para pessoas desligadas, a contagem de períodos para na data de saída.

---

## Tratamento de vínculos não CLT

Conforme a seção 2.4 do documento de decisões, prestadores MEI e PJ e voluntários **não
herdam as regras trabalhistas de férias**. Eles constam do mesmo cadastro e aparecem nas
mesmas listagens; suas pausas são registradas como ocorrências comuns. A ficha individual
indica explicitamente se a regra de férias é aplicável. O método
`TipoVinculo.temRegraDeFerias()` concentra essa decisão.

---

## Carga inicial (DataLoader)

Executada a cada inicialização, com **dados exclusivamente fictícios**, conforme a
exigência da seção 3.4 do documento de decisões. Nenhum dado real da KODIE Academy é
utilizado.

- 4 pessoas — uma CLT, uma CLT, uma PJ e uma voluntária
- 4 ocorrências vinculadas a pessoas
- 1 ocorrência sem pessoa vinculada (feriado institucional)
- 1 pessoa sem nenhuma ocorrência, para a listagem exibir o total zero

A carga foi montada para que o painel de férias exiba os quatro estados sem nenhuma
intervenção: uma pessoa com férias próximas do vencimento, uma com três períodos
acumulados e dois já vencidos, e duas sem regra CLT aplicável.

---

## Exemplos com curl

```bash
# Cadastrar uma pessoa — 201
curl -i -X POST http://localhost:8080/pessoas \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana Beatriz Moreira","cpf":"12345678901","email":"ana@exemplo.com",
       "cargo":"Coordenadora","tipoVinculo":"CLT","situacao":"ATIVO",
       "dataEntrada":"2024-12-04"}'

# Listar todas — 200
curl -i http://localhost:8080/pessoas

# Ocorrências de uma pessoa — 200, ou 404 se a pessoa não existir
curl -i http://localhost:8080/pessoas/1/ocorrencias
curl -i http://localhost:8080/pessoas/999/ocorrencias

# Validação reprovada — 400 com as mensagens em português
curl -i -X POST http://localhost:8080/pessoas \
  -H "Content-Type: application/json" -d '{"nome":"","cpf":"123"}'

# Registrar ocorrência vinculada a uma pessoa — 201
curl -i -X POST http://localhost:8080/ocorrencias \
  -H "Content-Type: application/json" \
  -d '{"tipo":"FERIAS","dataInicio":"2026-07-06","dataFim":"2026-07-20",
       "situacao":"CONFIRMADA","observacao":"15 dias","pessoa":{"id":1}}'
```

---

## Tecnologias

Java 25 · Spring Boot 4.1.1 · Spring Web · Spring Data JPA · Hibernate · Bean Validation ·
Thymeleaf · H2 em memória · Maven.

---

## Fora do escopo desta versão

Excluídos deliberadamente, conforme a seção 3.3 do documento de decisões ou por decisão
de priorização da equipe:

- **Calendário de ocorrências** em visualização mensal.
- **Exportação de informações** em CSV, Excel ou PDF — formato prioritário ainda não
  definido com a cliente.
- **Indicadores básicos** e indicadores de diversidade agregados.
- **Usuário e perfil de acesso** (administrador, operador, consulta) — previstos na seção
  2.2 do documento, não implementados.
- **Acompanhamento de aniversários** e **portal de autosserviço** — indicados pela própria
  cliente como fora do MVP.
- **Armazenamento de arquivos de documentos** — conforme a seção 3.3, a orientação é
  registrar apenas tipo, situação e link para local protegido.

### Pontos pendentes de definição com a cliente

Levantados na seção 5 do documento de decisões e ainda em aberto:

- ~~antecedência dos alertas de vencimento~~ — **definida em 90 dias**;
- ~~tratamento do período aquisitivo durante afastamento~~ — **adotada a contagem
  contínua**, a confirmar com a cliente;
- quais campos do cadastro entram efetivamente no MVP;
- formatos de exportação prioritários entre CSV, Excel e PDF;
- necessidade de acesso por dispositivos móveis.

### Limitação conhecida

A validação `@PastOrPresent` em `dataEntrada` impede cadastrar uma admissão com data
futura. É adequado para o uso corrente, mas impediria o pré-cadastro de uma contratação
já assinada — ponto a confirmar com a cliente.
