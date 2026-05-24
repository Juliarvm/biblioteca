# Documentacao Tecnica - Fase 3

## 1. Decisoes de projeto

- Arquitetura em camadas mantida: `model`, `dao`, `controller` e `view` (ServidorWeb).
- Persistencia com `RandomAccessFile` para todos os novos arquivos, seguindo o mesmo padrao da Fase 2.
- Exclusao logica por lapide (`boolean`) mantida em todos os registros e nos de relacionamento.
- Relacao N:N implementada com dois Hash Extensiveis:
  - `hashPorLeitor` mapeia `idLeitor -> ponteiro da cabeca` da lista de reservas daquele leitor.
  - `hashPorLivro` mapeia `idLivro -> ponteiro da cabeca` da lista de reservas daquele livro.
  - arquivo de relacao guarda nos com dois encadeamentos independentes (por leitor e por livro).
- Entidade `Leitor` promovida a entidade completa com CRUD proprio e persistencia dedicada.

## 2. Estruturas de dados e indice em disco

### 2.1 Registros principais

- `Livro` em `data/livros.db` (mantido)
- `Exemplar` em `data/exemplares.db` (mantido)
- `Leitor` em `data/leitores.db` (novo)
- `Reserva` em `data/reservas.db` (novo)

Formato geral por registro:

1. `lapide` (`boolean`)
2. `tamanho` (`int`)
3. campos serializados (incluindo `int` de tamanho para strings)

Campos de `Leitor`:

- `id` (`int`)
- `dataNascimento` (`long`)
- `nome` (`String` UTF-8 com tamanho precedente)
- `email` (`String` UTF-8 com tamanho precedente)

Campos de `Reserva`:

- `idLeitor` (`int`)
- `idLivro` (`int`)
- `dataReserva` (`long`)
- `status` (`String` UTF-8 com tamanho precedente)

### 2.2 Indices primarios

- `data/livros.idx` (mantido)
- `data/exemplares.idx` (mantido)
- `data/leitores.idx` (novo)

Formato de cada entrada:

- `id` (`int`)
- `posicao` (`long`)

A tabela `Reserva` nao usa indice direto por id sequencial, pois sua identidade e definida pela chave composta (idLeitor, idLivro). O acesso e feito exclusivamente pelos dois Hash Extensiveis.

### 2.3 Hash extensivel (relacionamento N:N)

Arquivos:

- `data/hash/reserva_leitor.dir` e `data/hash/reserva_leitor.bkt`
- `data/hash/reserva_livro.dir` e `data/hash/reserva_livro.bkt`

Mesma estrutura interna do hash ja existente (Fase 2):

- Diretorio: `profundidadeGlobal` (`int`) + vetor de ponteiros para buckets (`long`)
- Bucket: `profundidadeLocal` (`int`), `quantidade` (`int`), pares (`chave:int`, `valor:long`)

Operacoes: insercao com split, duplicacao de diretorio, busca e remocao.

### 2.4 Arquivo de relacionamento N:N

- `data/reserva.rel`

No da lista encadeada (tamanho fixo por no):

- `idLeitor` (`int`)
- `idLivro` (`int`)
- `posRegistro` (`long`) — posicao do registro correspondente em `reservas.db`
- `proxPorLeitor` (`long`) — ponteiro para o proximo no na cadeia deste leitor
- `proxPorLivro` (`long`) — ponteiro para o proximo no na cadeia deste livro
- `lapide` (`boolean`)

Cada `idLeitor` aponta para a cabeca da sua cadeia via `hashPorLeitor`.
Cada `idLivro` aponta para a cabeca da sua cadeia via `hashPorLivro`.

Isso permite navegar o relacionamento pelos dois lados sem varredura sequencial.

## 3. Integridade referencial

- Insercao de reserva valida unicidade da combinacao (idLeitor, idLivro).
- Exclusao de livro cancela logicamente todas as reservas vinculadas (`cancelarPorLivro`).
- Exclusao de leitor cancela logicamente todas as reservas vinculadas (`cancelarPorLeitor`).
- Exclusao de livro tambem remove logicamente os exemplares vinculados (cascata da Fase 2, mantida).

## 4. Validacoes

- ISBN duplicado: bloqueado no `LivroDAO` (mantido).
- Codigo de patrimonio duplicado: bloqueado no `ExemplarDAO` (mantido).
- Email de leitor duplicado: bloqueado no `LeitorDAO`.
- Reserva duplicada: par (idLeitor, idLivro) ja existente e bloqueado no `ReservaDAO`.
- Busca por chave inexistente: retorno nulo com mensagem de erro.
- Exclusao de inexistente: retorno falso com mensagem.
- FK inexistente no exemplar: erro de validacao (mantido).

## 5. Front-end

- Interface web (HTML/CSS/JS) acessada em `http://localhost:8080` (mantida e ampliada).
- O `ServidorWeb.java` expoe as seguintes rotas:

| Rota | Metodo | Descricao |
|------|--------|-----------|
| `/api/livros` | GET, POST | listar e inserir livros |
| `/api/livros/{id}` | GET, PUT, DELETE | buscar, atualizar e excluir livro |
| `/api/exemplares` | GET, POST | listar e inserir exemplares |
| `/api/exemplares/{id}` | GET, PUT, DELETE | buscar, atualizar e excluir exemplar |
| `/api/leitores` | GET, POST | listar e inserir leitores |
| `/api/leitores/{id}` | GET, PUT, DELETE | buscar, atualizar e excluir leitor |
| `/api/reservas` | GET, POST, DELETE | listar todas, criar e cancelar reserva |
| `/api/reservas/leitor?id=X` | GET | listar todas as reservas de um leitor |
| `/api/reservas/livro?id=X` | GET | listar todos os leitores que reservaram um livro |
| `/api/relacao/{livroId}` | GET | exemplares de um livro (1:N, mantido) |
| `/api/livros-ordenado` | GET | livros ordenados por titulo |
| `/api/arvore-bmais/*` | GET, POST | operacoes da Arvore B+ |

- A interface possui quatro cards principais: Livro, Exemplar, Leitor e Reserva.
- No Modo Tecnico (expansivel), ha cards para: Ordenacao Externa, Arvore B+, Relacao 1:N e Relacao N:N.
- O card Relacao N:N permite demonstrar o acesso pelos dois lados: livros de um leitor e leitores de um livro.

## 6. Ordenacao externa (mantida da Fase 2)

- Implementada em `OrdenacaoExternaLivros`.
- Estrategia:
  1. leitura dos registros ativos de Livro,
  2. geracao de particoes ordenadas em blocos limitados (`data/sort_tmp/particao_*.bin`),
  3. intercalacao k-way por fila de prioridade,
  4. escrita do resultado em `data/livros_ordenado_titulo.db`.

## 7. Arvore B+ (mantida da Fase 2)

- Implementada em `ArvoreBMaisIndice` (ordem 4).
- Operacoes: `inserir(int chave, long valor)` e `buscar(int chave)`.
- Usada para indexar IDs de Livro com suas posicoes no arquivo de dados.
- Acessivel no front-end via Modo Tecnico.

## 8. Formulario — Fase 3

### a) Qual foi o relacionamento N:N escolhido e quais tabelas ele conecta?

O relacionamento N:N escolhido foi entre `Leitor` e `Livro`, representando o conceito de
Reserva: um leitor pode reservar varios livros, e um livro pode ser reservado por varios
leitores. A tabela intermediaria `Reserva` conecta as duas entidades principais que ja
existiam no sistema desde a Fase 1.

### b) Qual estrutura de indice foi utilizada? Justifique a escolha.

Foi utilizado o Hash Extensivel, com dois indices independentes: `hashPorLeitor` e
`hashPorLivro`. A escolha se justifica porque as consultas ao N:N sao sempre por igualdade
de chave (todas as reservas de um leitor ou todos os leitores de um livro), sem necessidade
de ordenacao ou varredura por intervalo. O Hash Extensivel oferece acesso O(1) para esse
padrao. A Arvore B+ ja e utilizada na ordenacao de livros por titulo, cumprindo o requisito
de demonstracao pratica da estrutura no sistema.

### c) Como foi implementada a chave composta da tabela intermediaria?

A chave composta e formada por `idLeitor` e `idLivro`. No arquivo `reservas.db`, cada
registro armazena ambos os inteiros sequencialmente. A unicidade e garantida pelo metodo
`create()` do `ReservaDAO`, que verifica a existencia do par antes de inserir. Nao e gerado
id sequencial para `Reserva`; a identidade do registro e definida exclusivamente pela
combinacao das duas chaves estrangeiras.

### d) Como e feita a busca eficiente de registros por meio do indice?

A classe `IndiceReserva` mantem dois `HashExtensivel`. Cada hash mapeia uma chave inteira
para a posicao do primeiro no em `reserva.rel`. Cada no armazena idLeitor, idLivro, posicao
do registro em `reservas.db`, ponteiro para o proximo no pela cadeia do leitor, ponteiro
pela cadeia do livro e lapide. Para listar reservas de um leitor, consulta-se o
`hashPorLeitor` e percorre-se a cadeia; para listar leitores de um livro, consulta-se o
`hashPorLivro`.

### e) Como o sistema trata a integridade referencial entre as tabelas?

Ao excluir um Livro, o `LivroController` chama `reservaDAO.cancelarPorLivro(id)` antes de
deletar, marcando com lapide todas as reservas daquele livro. Ao excluir um Leitor, o
`LeitorController` chama `reservaDAO.cancelarPorLeitor(id)`. Nao e possivel criar reserva
duplicada para o mesmo par (idLeitor, idLivro).

### f) Como foi organizada a persistencia dos dados da tabela Reserva?

O arquivo `reservas.db` segue o mesmo padrao dos demais: 4 bytes de cabecalho (contador),
seguido de registros com lapide (boolean), tamanho (int) e campos binarios. O arquivo
`reserva.rel` armazena nos da lista encadeada com lapide propria. Os hashes ficam em
`data/hash/` no mesmo padrao do `livro_exemplar` existente.

### g) Como o codigo da tabela intermediaria se integra com o CRUD das tabelas principais?

O `ReservaController` e instanciado no `ServidorWeb` junto com os demais controllers. O
`LivroController` recebe o `ReservaDAO` no construtor e chama `cancelarPorLivro()` ao
excluir. O `LeitorController` faz o mesmo com `cancelarPorLeitor()`. As rotas `/api/reservas`
suportam GET, POST e DELETE. A interface exibe um card dedicado de Reserva e um card de
Relacao N:N no Modo Tecnico.

### h) Como esta organizada a estrutura de diretorios apos a Fase 3?

- Raiz com `App.java`, pasta `src/` separada em `model`, `dao`, `controller`, e pasta `data/` para persistencia.
- Arquitetura em camadas com responsabilidade separada.