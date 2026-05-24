package controller;

import dao.LivroDAO;
import dao.ReservaDAO;
import model.Livro;

import java.io.IOException;
import java.util.List;

public class LivroController {
    private final LivroDAO livroDAO;
    private final ReservaDAO reservaDAO;

    public LivroController(LivroDAO livroDAO, ReservaDAO reservaDAO) {
        this.livroDAO = livroDAO;
        this.reservaDAO = reservaDAO;
    }

    public int criar(Livro livro) throws IOException {
        return livroDAO.create(livro);
    }

    public Livro buscarPorId(int id) throws IOException {
        return livroDAO.read(id);
    }

    public List<Livro> listarTodos() throws IOException {
        return livroDAO.readAll();
    }

    public boolean atualizar(Livro livro) throws IOException {
        return livroDAO.update(livro);
    }

    public boolean excluir(int id) throws IOException {
        reservaDAO.cancelarPorLivro(id);
        return livroDAO.delete(id);
    }
}
