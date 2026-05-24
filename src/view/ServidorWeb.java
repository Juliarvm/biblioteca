package view;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import controller.ExemplarController;
import controller.LeitorController;
import controller.LivroController;
import controller.ReservaController;
import dao.ArvoreBMaisIndice;
import dao.ExemplarDAO;
import dao.LeitorDAO;
import dao.LivroDAO;
import dao.OrdenacaoExternaLivros;
import dao.ReservaDAO;
import model.Exemplar;
import model.Leitor;
import model.Livro;
import model.Reserva;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServidorWeb {
    private final HttpServer servidor;
    private final LivroDAO livroDAO;
    private final ExemplarDAO exemplarDAO;
    private final LeitorDAO leitorDAO;
    private final ReservaDAO reservaDAO;
    private final LivroController livroController;
    private final ExemplarController exemplarController;
    private final LeitorController leitorController;
    private final ReservaController reservaController;
    private final OrdenacaoExternaLivros ordenacaoExterna;
    private final ArvoreBMaisIndice arvoreBMais;

    public ServidorWeb(int porta) throws IOException {
        this.livroDAO = new LivroDAO();
        this.exemplarDAO = new ExemplarDAO(livroDAO);
        this.leitorDAO = new LeitorDAO();
        this.reservaDAO = new ReservaDAO();
        this.livroController = new LivroController(livroDAO, reservaDAO);
        this.exemplarController = new ExemplarController(exemplarDAO);
        this.leitorController = new LeitorController(leitorDAO, reservaDAO);
        this.reservaController = new ReservaController();
        this.ordenacaoExterna = new OrdenacaoExternaLivros(livroDAO);
        this.arvoreBMais = new ArvoreBMaisIndice(4);

        this.servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        registrarRotas();
        carregarArvoreBMais();
    }

    private void carregarArvoreBMais() {
        try {
            List<Livro> livros = livroController.listarTodos();
            for (Livro livro : livros) {
                Long posicao = livroDAO.getPosicaoPorId(livro.getId());
                if (posicao != null) arvoreBMais.inserir(livro.getId(), posicao);
            }
        } catch (Exception e) {
            System.err.println("Aviso: erro ao carregar Arvore B+.");
        }
    }

    public void iniciar() {
        servidor.start();
    }

    private void registrarRotas() {
        servidor.createContext("/", this::tratarInicio);
        servidor.createContext("/api/livros", this::tratarLivros);
        servidor.createContext("/api/exemplares", this::tratarExemplares);
        servidor.createContext("/api/leitores", this::tratarLeitores);
        servidor.createContext("/api/reservas", this::tratarReservas);
        servidor.createContext("/api/relacao", this::tratarRelacao);
        servidor.createContext("/api/livros-ordenado", this::tratarLivrosOrdenado);
        servidor.createContext("/api/ordenacao", this::tratarOrdenacao);
        servidor.createContext("/api/arvore-bmais", this::tratarArvoreBMais);
    }

    private void tratarInicio(HttpExchange req) throws IOException {
        if (!"GET".equalsIgnoreCase(req.getRequestMethod())) {
            enviarResposta(req, 405, "text/plain", "Metodo nao permitido.");
            return;
        }
        enviarResposta(req, 200, "text/html; charset=utf-8", gerarHtml());
    }

    private void tratarLivros(HttpExchange req) throws IOException {
        try {
            String caminho = req.getRequestURI().getPath();
            String metodo = req.getRequestMethod();

            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/livros")) {
                List<Livro> livros = livroController.listarTodos();
                StringBuilder sb = new StringBuilder();
                if (livros.isEmpty()) sb.append("Sem livros cadastrados.\n");
                for (Livro l : livros) sb.append(l).append("\n");
                enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
                return;
            }
            if ("POST".equalsIgnoreCase(metodo) && caminho.equals("/api/livros")) {
                int id = livroController.criar(paraLivro(0, lerFormulario(req)));
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Livro inserido. ID=" + id);
                return;
            }
            if (caminho.startsWith("/api/livros/")) {
                int id = Integer.parseInt(caminho.substring("/api/livros/".length()));
                if ("GET".equalsIgnoreCase(metodo)) {
                    Livro l = livroController.buscarPorId(id);
                    enviarResposta(req, 200, "text/plain; charset=utf-8", l == null ? "Livro nao encontrado." : l.toString());
                    return;
                }
                if ("PUT".equalsIgnoreCase(metodo)) {
                    if (livroController.buscarPorId(id) == null) { enviarResposta(req, 404, "text/plain; charset=utf-8", "Livro inexistente."); return; }
                    livroController.atualizar(paraLivro(id, lerFormulario(req)));
                    enviarResposta(req, 200, "text/plain; charset=utf-8", "Livro atualizado.");
                    return;
                }
                if ("DELETE".equalsIgnoreCase(metodo)) {
                    if (livroController.buscarPorId(id) == null) { enviarResposta(req, 404, "text/plain; charset=utf-8", "Livro inexistente."); return; }
                    exemplarController.excluirPorLivro(id);
                    livroController.excluir(id);
                    enviarResposta(req, 200, "text/plain; charset=utf-8", "Livro, exemplares e reservas excluidos logicamente.");
                    return;
                }
            }
            enviarResposta(req, 405, "text/plain; charset=utf-8", "Operacao nao suportada.");
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarExemplares(HttpExchange req) throws IOException {
        try {
            String caminho = req.getRequestURI().getPath();
            String metodo = req.getRequestMethod();

            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/exemplares")) {
                List<Exemplar> lista = exemplarController.listarTodos();
                StringBuilder sb = new StringBuilder();
                if (lista.isEmpty()) sb.append("Sem exemplares cadastrados.\n");
                for (Exemplar e : lista) sb.append(formatarExemplar(e)).append("\n");
                enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
                return;
            }
            if ("POST".equalsIgnoreCase(metodo) && caminho.equals("/api/exemplares")) {
                int id = exemplarController.criar(paraExemplar(0, lerFormulario(req)));
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Exemplar inserido. ID=" + id);
                return;
            }
            if (caminho.startsWith("/api/exemplares/")) {
                int id = Integer.parseInt(caminho.substring("/api/exemplares/".length()));
                if ("GET".equalsIgnoreCase(metodo)) {
                    Exemplar e = exemplarController.buscarPorId(id);
                    enviarResposta(req, 200, "text/plain; charset=utf-8", e == null ? "Exemplar nao encontrado." : formatarExemplar(e));
                    return;
                }
                if ("PUT".equalsIgnoreCase(metodo)) {
                    if (exemplarController.buscarPorId(id) == null) { enviarResposta(req, 404, "text/plain; charset=utf-8", "Exemplar inexistente."); return; }
                    exemplarController.atualizar(paraExemplar(id, lerFormulario(req)));
                    enviarResposta(req, 200, "text/plain; charset=utf-8", "Exemplar atualizado.");
                    return;
                }
                if ("DELETE".equalsIgnoreCase(metodo)) {
                    boolean ok = exemplarController.excluir(id);
                    enviarResposta(req, ok ? 200 : 404, "text/plain; charset=utf-8", ok ? "Exemplar excluido logicamente." : "Exemplar inexistente.");
                    return;
                }
            }
            enviarResposta(req, 405, "text/plain; charset=utf-8", "Operacao nao suportada.");
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarLeitores(HttpExchange req) throws IOException {
        try {
            String caminho = req.getRequestURI().getPath();
            String metodo = req.getRequestMethod();

            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/leitores")) {
                List<Leitor> lista = leitorController.listarTodos();
                StringBuilder sb = new StringBuilder();
                if (lista.isEmpty()) sb.append("Sem leitores cadastrados.\n");
                for (Leitor l : lista) sb.append(l).append("\n");
                enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
                return;
            }
            if ("POST".equalsIgnoreCase(metodo) && caminho.equals("/api/leitores")) {
                int id = leitorController.criar(paraLeitor(0, lerFormulario(req)));
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Leitor inserido. ID=" + id);
                return;
            }
            if (caminho.startsWith("/api/leitores/")) {
                int id = Integer.parseInt(caminho.substring("/api/leitores/".length()));
                if ("GET".equalsIgnoreCase(metodo)) {
                    Leitor l = leitorController.buscarPorId(id);
                    enviarResposta(req, 200, "text/plain; charset=utf-8", l == null ? "Leitor nao encontrado." : l.toString());
                    return;
                }
                if ("PUT".equalsIgnoreCase(metodo)) {
                    if (leitorController.buscarPorId(id) == null) { enviarResposta(req, 404, "text/plain; charset=utf-8", "Leitor inexistente."); return; }
                    leitorController.atualizar(paraLeitor(id, lerFormulario(req)));
                    enviarResposta(req, 200, "text/plain; charset=utf-8", "Leitor atualizado.");
                    return;
                }
                if ("DELETE".equalsIgnoreCase(metodo)) {
                    if (leitorController.buscarPorId(id) == null) { enviarResposta(req, 404, "text/plain; charset=utf-8", "Leitor inexistente."); return; }
                    leitorController.excluir(id);
                    enviarResposta(req, 200, "text/plain; charset=utf-8", "Leitor e reservas excluidos logicamente.");
                    return;
                }
            }
            enviarResposta(req, 405, "text/plain; charset=utf-8", "Operacao nao suportada.");
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarReservas(HttpExchange req) throws IOException {
        try {
            String caminho = req.getRequestURI().getPath();
            String metodo = req.getRequestMethod();

            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/reservas")) {
                List<Reserva> lista = reservaController.listarTodos();
                StringBuilder sb = new StringBuilder();
                if (lista.isEmpty()) sb.append("Sem reservas cadastradas.\n");
                for (Reserva r : lista) sb.append(r).append("\n");
                enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
                return;
            }
            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/reservas/leitor")) {
                int id = Integer.parseInt(lerConsulta(req.getRequestURI().getRawQuery()).get("id"));
                List<Reserva> lista = reservaController.listarPorLeitor(id);
                StringBuilder sb = new StringBuilder();
                sb.append("Reservas do leitor ID ").append(id).append(":\n");
                if (lista.isEmpty()) sb.append("Nenhuma reserva.\n");
                for (Reserva r : lista) sb.append(r).append("\n");
                enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
                return;
            }
            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/reservas/livro")) {
                int id = Integer.parseInt(lerConsulta(req.getRequestURI().getRawQuery()).get("id"));
                List<Reserva> lista = reservaController.listarPorLivro(id);
                StringBuilder sb = new StringBuilder();
                sb.append("Reservas do livro ID ").append(id).append(":\n");
                if (lista.isEmpty()) sb.append("Nenhuma reserva.\n");
                for (Reserva r : lista) sb.append(r).append("\n");
                enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
                return;
            }
            if ("POST".equalsIgnoreCase(metodo) && caminho.equals("/api/reservas")) {
                Map<String, String> p = lerFormulario(req);
                reservaController.criar(Integer.parseInt(p.get("idLeitor")), Integer.parseInt(p.get("idLivro")));
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Reserva criada.");
                return;
            }
            if ("DELETE".equalsIgnoreCase(metodo) && caminho.equals("/api/reservas")) {
                Map<String, String> p = lerFormulario(req);
                reservaController.cancelar(Integer.parseInt(p.get("idLeitor")), Integer.parseInt(p.get("idLivro")));
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Reserva cancelada.");
                return;
            }
            enviarResposta(req, 405, "text/plain; charset=utf-8", "Operacao nao suportada.");
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarRelacao(HttpExchange req) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(req.getRequestMethod())) {
                enviarResposta(req, 405, "text/plain; charset=utf-8", "Metodo nao permitido.");
                return;
            }
            String caminho = req.getRequestURI().getPath();
            if (!caminho.startsWith("/api/relacao/")) {
                enviarResposta(req, 400, "text/plain; charset=utf-8", "Informe /api/relacao/{livroId}.");
                return;
            }
            int livroId = Integer.parseInt(caminho.substring("/api/relacao/".length()));
            List<Exemplar> lista = exemplarController.listarPorLivro(livroId);
            StringBuilder sb = new StringBuilder();
            sb.append("Livro ID: ").append(livroId).append("\n");
            if (lista.isEmpty()) sb.append("Nenhum exemplar.\n");
            for (Exemplar e : lista) sb.append(formatarExemplar(e)).append("\n");
            enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarLivrosOrdenado(HttpExchange req) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(req.getRequestMethod())) { enviarResposta(req, 405, "text/plain; charset=utf-8", "Metodo nao permitido."); return; }
            List<Livro> ordenados = ordenacaoExterna.ordenarPorTitulo(3);
            StringBuilder sb = new StringBuilder("Livros ordenados por titulo:\n\n");
            if (ordenados.isEmpty()) sb.append("Nenhum livro cadastrado.\n");
            for (Livro l : ordenados) sb.append(l).append("\n");
            enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarOrdenacao(HttpExchange req) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(req.getRequestMethod())) { enviarResposta(req, 405, "text/plain; charset=utf-8", "Metodo nao permitido."); return; }
            int bloco = Integer.parseInt(lerConsulta(req.getRequestURI().getRawQuery()).getOrDefault("bloco", "3"));
            List<Livro> ordenados = ordenacaoExterna.ordenarPorTitulo(bloco);
            StringBuilder sb = new StringBuilder("Ordenacao concluida. Arquivo: data/livros_ordenado_titulo.db\nBloco utilizado: " + bloco + "\n\n");
            for (Livro l : ordenados) sb.append("ID=").append(l.getId()).append(" | Titulo=").append(l.getTitulo()).append("\n");
            enviarResposta(req, 200, "text/plain; charset=utf-8", sb.toString());
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private void tratarArvoreBMais(HttpExchange req) throws IOException {
        try {
            String caminho = req.getRequestURI().getPath();
            String metodo = req.getRequestMethod();
            if ("POST".equalsIgnoreCase(metodo) && caminho.equals("/api/arvore-bmais/inserir")) {
                Map<String, String> p = lerFormulario(req);
                arvoreBMais.inserir(Integer.parseInt(p.get("chave")), Long.parseLong(p.get("valor")));
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Inserido na Arvore B+.");
                return;
            }
            if ("GET".equalsIgnoreCase(metodo) && caminho.startsWith("/api/arvore-bmais/buscar/")) {
                int chave = Integer.parseInt(caminho.substring("/api/arvore-bmais/buscar/".length()));
                Long valor = arvoreBMais.buscar(chave);
                String texto = valor == null
                    ? "--- BUSCA ARVORE B+ ---\nChave: " + chave + "\nResultado: Chave nao encontrada."
                    : "--- BUSCA ARVORE B+ ---\nChave: " + chave + "\nValor encontrado: " + valor;
                enviarResposta(req, 200, "text/plain; charset=utf-8", texto);
                return;
            }
            if ("POST".equalsIgnoreCase(metodo) && caminho.equals("/api/arvore-bmais/carga")) {
                List<Livro> livros = livroController.listarTodos();
                int total = 0;
                for (Livro l : livros) {
                    Long pos = livroDAO.getPosicaoPorId(l.getId());
                    if (pos != null) { arvoreBMais.inserir(l.getId(), pos); total++; }
                }
                enviarResposta(req, 200, "text/plain; charset=utf-8", "Carga na Arvore B+ concluida. Chaves: " + total);
                return;
            }
            if ("GET".equalsIgnoreCase(metodo) && caminho.equals("/api/arvore-bmais/depurar")) {
                enviarResposta(req, 200, "text/plain; charset=utf-8", arvoreBMais.depurarEstrutura());
                return;
            }
            enviarResposta(req, 405, "text/plain; charset=utf-8", "Operacao da Arvore B+ nao suportada.");
        } catch (Exception e) {
            enviarResposta(req, 400, "text/plain; charset=utf-8", "Erro: " + e.getMessage());
        }
    }

    private Livro paraLivro(int id, Map<String, String> p) {
        String titulo = p.getOrDefault("titulo", "");
        String isbn = p.getOrDefault("isbn", "");
        String editora = p.getOrDefault("editora", "");
        int edicao = Integer.parseInt(p.getOrDefault("edicao", "1"));
        float preco = Float.parseFloat(p.getOrDefault("preco", "0").replace(',', '.'));
        String autores = p.getOrDefault("autores", "");
        String categorias = p.getOrDefault("categorias", "");
        long data;
        try { data = new SimpleDateFormat("dd/MM/yyyy").parse(p.getOrDefault("data", "")).getTime(); }
        catch (Exception e) { data = System.currentTimeMillis(); }
        return new Livro(id, isbn, titulo, preco, data, categorias, autores, edicao, editora);
    }

    private Exemplar paraExemplar(int id, Map<String, String> p) {
        return new Exemplar(id,
            Integer.parseInt(p.getOrDefault("livroId", "0")),
            p.getOrDefault("codigoPatrimonio", ""),
            p.getOrDefault("estado", "NOVO"));
    }

    private Leitor paraLeitor(int id, Map<String, String> p) {
        long data;
        try { data = new SimpleDateFormat("dd/MM/yyyy").parse(p.getOrDefault("dataNascimento", "")).getTime(); }
        catch (Exception e) { data = System.currentTimeMillis(); }
        return new Leitor(id,
            p.getOrDefault("nome", ""),
            p.getOrDefault("email", ""),
            data);
    }

    private String formatarExemplar(Exemplar e) {
        return """
--- DADOS DO EXEMPLAR ---
ID: %d | Livro ID: %d
Codigo Patrimonio: %s
Estado: %s
""".formatted(e.getId(), e.getLivroId(), e.getCodigoPatrimonio(), e.getEstado());
    }

    private Map<String, String> lerFormulario(HttpExchange req) throws IOException {
        return lerConsulta(new String(req.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    private Map<String, String> lerConsulta(String raw) {
        Map<String, String> mapa = new HashMap<>();
        if (raw == null || raw.isBlank()) return mapa;
        for (String par : raw.split("&")) {
            String[] kv = par.split("=", 2);
            mapa.put(decodificar(kv[0]), kv.length > 1 ? decodificar(kv[1]) : "");
        }
        return mapa;
    }

    private String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }

    private void enviarResposta(HttpExchange req, int status, String tipo, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        req.getResponseHeaders().set("Content-Type", tipo);
        req.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = req.getResponseBody()) { out.write(bytes); }
    }

    private String gerarHtml() {
        return """
<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <title>Biblioteca Fase 3</title>
  <style>
    :root{--bg:#f8fafc;--card:#ffffff;--ink:#0f172a;--accent:#0ea5e9;--muted:#64748b;--green:#10b981;--red:#ef4444}
    body{margin:0;font-family:Segoe UI,Tahoma,sans-serif;background:linear-gradient(120deg,#e2e8f0,#f8fafc);color:var(--ink)}
    .container{max-width:1200px;margin:24px auto;padding:0 16px}
    h1{margin:0 0 4px}
    .grade{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:12px}
    .cartao{background:var(--card);border-radius:12px;box-shadow:0 6px 20px rgba(2,6,23,.08);padding:14px}
    .cartao h3{margin:0 0 10px}
    .linha{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px}
    input{padding:8px;border:1px solid #cbd5e1;border-radius:8px;min-width:110px;font-size:13px}
    button{border:0;border-radius:8px;background:var(--accent);color:#fff;padding:8px 12px;cursor:pointer;font-size:13px}
    button.sec{background:#334155}
    button.danger{background:var(--red)}
    pre{background:#0b1220;color:#dbeafe;min-height:180px;padding:12px;border-radius:10px;overflow:auto;font-size:13px}
    .legenda{color:var(--muted);font-size:13px;margin:0 0 16px}
    .secao{margin-top:20px}
    details{padding:12px;background:#f0f4f8;border-radius:8px}
    summary{cursor:pointer;font-weight:bold;color:#333}
  </style>
</head>
<body>
<div class="container">
  <h1>Biblioteca</h1>
  <p class="legenda">Sistema de gerenciamento — Fase 3</p>
  <div class="grade">

    <div class="cartao">
      <h3>Livro</h3>
      <div class="linha"><input id="livro-id" placeholder="id"><button onclick="buscarLivro()">Buscar</button><button onclick="excluirLivro()" class="danger">Excluir</button></div>
      <div class="linha"><input id="livro-titulo" placeholder="titulo"><input id="livro-isbn" placeholder="isbn"></div>
      <div class="linha"><input id="livro-editora" placeholder="editora"><input id="livro-edicao" placeholder="edicao"></div>
      <div class="linha"><input id="livro-preco" placeholder="preco"><input id="livro-data" placeholder="dd/MM/yyyy"></div>
      <div class="linha"><input id="livro-autores" placeholder="autores"><input id="livro-categorias" placeholder="categorias"></div>
      <div class="linha">
        <button onclick="inserirLivro()">Inserir</button>
        <button onclick="atualizarLivro()">Atualizar</button>
        <button onclick="listarLivros()" class="sec">Listar</button>
        <button onclick="listarLivrosOrdenado()" class="sec">Listar por Titulo</button>
      </div>
    </div>

    <div class="cartao">
      <h3>Exemplar</h3>
      <div class="linha"><input id="exemplar-id" placeholder="id"><button onclick="buscarExemplar()">Buscar</button><button onclick="excluirExemplar()" class="danger">Excluir</button></div>
      <div class="linha"><input id="exemplar-livro" placeholder="livroId"><input id="exemplar-codigo" placeholder="codigoPatrimonio"><input id="exemplar-estado" placeholder="estado"></div>
      <div class="linha">
        <button onclick="inserirExemplar()">Inserir</button>
        <button onclick="atualizarExemplar()">Atualizar</button>
        <button onclick="listarExemplares()" class="sec">Listar</button>
      </div>
    </div>

    <div class="cartao">
      <h3>Leitor</h3>
      <div class="linha"><input id="leitor-id" placeholder="id"><button onclick="buscarLeitor()">Buscar</button><button onclick="excluirLeitor()" class="danger">Excluir</button></div>
      <div class="linha"><input id="leitor-nome" placeholder="nome"><input id="leitor-email" placeholder="email"></div>
      <div class="linha"><input id="leitor-nascimento" placeholder="dd/MM/yyyy (nascimento)"></div>
      <div class="linha">
        <button onclick="inserirLeitor()">Inserir</button>
        <button onclick="atualizarLeitor()">Atualizar</button>
        <button onclick="listarLeitores()" class="sec">Listar</button>
      </div>
    </div>

    <div class="cartao">
      <h3>Reserva</h3>
      <div class="linha"><input id="reserva-leitor" placeholder="idLeitor"><input id="reserva-livro" placeholder="idLivro"></div>
      <div class="linha">
        <button onclick="criarReserva()">Reservar</button>
        <button onclick="cancelarReserva()" class="danger">Cancelar</button>
        <button onclick="listarReservas()" class="sec">Listar Todas</button>
      </div>
      <div class="linha">
        <button onclick="reservasPorLeitor()" class="sec">Livros do Leitor</button>
        <button onclick="reservasPorLivro()" class="sec">Leitores do Livro</button>
      </div>
    </div>
  </div>

  <details class="secao">
    <summary>Modo Técnico</summary>
    <div class="grade" style="margin-top:12px">
      <div class="cartao">
        <h3>Ordenação Externa</h3>
        <div class="linha"><input id="ordenacao-bloco" value="3" placeholder="bloco"><button onclick="ordenarExternamente()">Ordenar por titulo</button></div>
      </div>
      <div class="cartao">
        <h3>Árvore B+</h3>
        <div class="linha"><input id="arvore-chave" placeholder="chave"><input id="arvore-valor" placeholder="valor"><button onclick="inserirArvore()">Inserir</button></div>
        <div class="linha"><input id="arvore-busca" placeholder="chave busca"><button onclick="buscarArvore()">Buscar</button><button onclick="depurarArvore()" class="sec">Debug</button></div>
      </div>
      <div class="cartao">
        <h3>Relação 1:N</h3>
        <div class="linha"><input id="relacao-livro" placeholder="livroId"><button onclick="listarRelacao()">Exemplares do Livro</button></div>
      </div>
      <div class="cartao">
        <h3>Relação N:N</h3>
        <div class="linha"><input id="nn-leitor" placeholder="idLeitor"><button onclick="nnPorLeitor()">Livros do Leitor</button></div>
        <div class="linha"><input id="nn-livro" placeholder="idLivro"><button onclick="nnPorLivro()">Leitores do Livro</button></div>
      </div>
    </div>
  </details>

  <h3 style="margin-top:20px">Saída</h3>
  <pre id="saida"></pre>
</div>

<script>
  const saida = document.getElementById('saida');
  const cod = obj => new URLSearchParams(obj).toString();
  async function req(url, ops={}) { const r = await fetch(url, ops); saida.textContent = await r.text(); }
  const v = id => document.getElementById(id).value || '';

  function listarLivros(){ req('/api/livros'); }
  function buscarLivro(){ req('/api/livros/'+v('livro-id')); }
  function inserirLivro(){ req('/api/livros',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({titulo:v('livro-titulo'),isbn:v('livro-isbn'),editora:v('livro-editora'),edicao:v('livro-edicao'),preco:v('livro-preco'),data:v('livro-data'),autores:v('livro-autores'),categorias:v('livro-categorias')})}); }
  function atualizarLivro(){ req('/api/livros/'+v('livro-id'),{method:'PUT',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({titulo:v('livro-titulo'),isbn:v('livro-isbn'),editora:v('livro-editora'),edicao:v('livro-edicao'),preco:v('livro-preco'),data:v('livro-data'),autores:v('livro-autores'),categorias:v('livro-categorias')})}); }
  function excluirLivro(){ req('/api/livros/'+v('livro-id'),{method:'DELETE'}); }
  function listarLivrosOrdenado(){ req('/api/livros-ordenado'); }

  function listarExemplares(){ req('/api/exemplares'); }
  function buscarExemplar(){ req('/api/exemplares/'+v('exemplar-id')); }
  function inserirExemplar(){ req('/api/exemplares',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({livroId:v('exemplar-livro'),codigoPatrimonio:v('exemplar-codigo'),estado:v('exemplar-estado')})}); }
  function atualizarExemplar(){ req('/api/exemplares/'+v('exemplar-id'),{method:'PUT',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({livroId:v('exemplar-livro'),codigoPatrimonio:v('exemplar-codigo'),estado:v('exemplar-estado')})}); }
  function excluirExemplar(){ req('/api/exemplares/'+v('exemplar-id'),{method:'DELETE'}); }

  function listarLeitores(){ req('/api/leitores'); }
  function buscarLeitor(){ req('/api/leitores/'+v('leitor-id')); }
  function inserirLeitor(){ req('/api/leitores',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({nome:v('leitor-nome'),email:v('leitor-email'),dataNascimento:v('leitor-nascimento')})}); }
  function atualizarLeitor(){ req('/api/leitores/'+v('leitor-id'),{method:'PUT',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({nome:v('leitor-nome'),email:v('leitor-email'),dataNascimento:v('leitor-nascimento')})}); }
  function excluirLeitor(){ req('/api/leitores/'+v('leitor-id'),{method:'DELETE'}); }

  function criarReserva(){ req('/api/reservas',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({idLeitor:v('reserva-leitor'),idLivro:v('reserva-livro')})}); }
  function cancelarReserva(){ req('/api/reservas',{method:'DELETE',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({idLeitor:v('reserva-leitor'),idLivro:v('reserva-livro')})}); }
  function listarReservas(){ req('/api/reservas'); }
  function reservasPorLeitor(){ req('/api/reservas/leitor?id='+v('reserva-leitor')); }
  function reservasPorLivro(){ req('/api/reservas/livro?id='+v('reserva-livro')); }

  function nnPorLeitor(){ req('/api/reservas/leitor?id='+v('nn-leitor')); }
  function nnPorLivro(){ req('/api/reservas/livro?id='+v('nn-livro')); }

  function listarRelacao(){ req('/api/relacao/'+v('relacao-livro')); }
  function ordenarExternamente(){ req('/api/ordenacao?bloco='+encodeURIComponent(v('ordenacao-bloco'))); }
  function inserirArvore(){ req('/api/arvore-bmais/inserir',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:cod({chave:v('arvore-chave'),valor:v('arvore-valor')})}); }
  function buscarArvore(){ req('/api/arvore-bmais/buscar/'+v('arvore-busca')); }
  function depurarArvore(){ req('/api/arvore-bmais/depurar'); }
</script>
</body>
</html>
""";
    }
}
