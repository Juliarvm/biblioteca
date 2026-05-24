package controller;

import dao.LeitorDAO;
import dao.ReservaDAO;
import model.Leitor;

import java.io.IOException;
import java.util.List;

public class LeitorController {
    private final LeitorDAO leitorDAO;
    private final ReservaDAO reservaDAO;

    public LeitorController(LeitorDAO leitorDAO, ReservaDAO reservaDAO) {
        this.leitorDAO = leitorDAO;
        this.reservaDAO = reservaDAO;
    }

    public int criar(Leitor leitor) throws IOException {
        return leitorDAO.create(leitor);
    }

    public Leitor buscarPorId(int id) throws IOException {
        return leitorDAO.read(id);
    }

    public List<Leitor> listarTodos() throws IOException {
        return leitorDAO.readAll();
    }

    public boolean atualizar(Leitor leitor) throws IOException {
        return leitorDAO.update(leitor);
    }

    public boolean excluir(int id) throws IOException {
        reservaDAO.cancelarPorLeitor(id);
        return leitorDAO.delete(id);
    }
}