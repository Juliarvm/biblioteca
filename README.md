# TP Biblioteca - Fase 3

Aplicacao Java com arquitetura em camadas (model, dao, controller), persistencia em disco, relacionamento 1:N com Hash Extensivel e relacionamento N:N com tabela intermediaria.

## Estrutura

- `App.java`: inicializa o servidor web local
- `src/model`: entidades (`Livro`, `Exemplar`, `Leitor`, `Reserva`)
- `src/dao`: persistencia, indices, hash extensivel e indice de reservas
- `src/controller`: camada de controle dos CRUDs
- `src/view`: front-end web e rotas HTTP (`ServidorWeb`)
- `data/`: arquivos de dados e indices

## Requisitos atendidos (Fase 3)

- CRUD completo de Livro, Exemplar e Leitor
- Relacionamento N:N Leitor <-> Livro via tabela intermediaria `Reserva`
- Chave primaria composta em `Reserva` (idLeitor + idLivro)
- Acesso ao N:N pelos dois lados (reservas por leitor e por livro)
- Dois HashExtensivel para indexar a tabela intermediaria
- Indice primario para todas as tabelas (`IndiceDireto`)
- Relacionamento 1:N Livro -> Exemplar com Hash Extensivel (mantido)
- Ordenacao externa por atributo (`titulo` de Livro) (mantido)
- Arvore B+ com insercao e busca (mantido)
- Persistencia em disco entre execucoes
- Exclusao logica com lapide em todos os arquivos
- Integridade referencial em cascata
- Validacao de entradas e erros comuns

## Compilacao

No Windows PowerShell, na raiz do projeto:

```powershell
javac -encoding UTF-8 -d . App.java src/model/*.java src/dao/*.java src/controller/*.java src/view/*.java
```

## Execucao

```powershell
java App
```

Abra no navegador:

`http://localhost:8080`

## Persistencia

Arquivos gerados na pasta `data/`:

- `livros.db` e `livros.idx`
- `exemplares.db` e `exemplares.idx`
- `leitores.db` e `leitores.idx`
- `reservas.db`
- `reserva.rel`
- `hash/livro_exemplar.dir` e `hash/livro_exemplar.bkt`
- `hash/reserva_leitor.dir` e `hash/reserva_leitor.bkt`
- `hash/reserva_livro.dir` e `hash/reserva_livro.bkt`
- `livros_ordenado_titulo.db`
- `sort_tmp/` (temporarios da ordenacao externa)

## Observacoes

- A exclusao de livro faz exclusao logica em cascata dos exemplares e reservas vinculados.
- A exclusao de leitor faz exclusao logica em cascata das reservas vinculadas.
- As validacoes incluem:
  - ISBN duplicado
  - codigo de patrimonio duplicado
  - email de leitor duplicado
  - reserva duplicada para o mesmo par (idLeitor, idLivro)
  - FK de livro inexistente ao inserir exemplar
  - busca/exclusao de chave inexistente
