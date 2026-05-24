package controller;

import dao.ReservaDAO;
import model.Reserva;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ReservaController {

    private ReservaDAO reservaDAO;

    public ReservaController() throws IOException {
        reservaDAO = new ReservaDAO();
    }

    public void criar(int idLeitor, int idLivro) throws IOException {
        Reserva r = new Reserva(idLeitor, idLivro, new Date().getTime(), "PENDENTE");
        reservaDAO.create(r);
    }

    public List<Reserva> listarPorLeitor(int idLeitor) throws IOException {
        return reservaDAO.readByLeitor(idLeitor);
    }

    public List<Reserva> listarPorLivro(int idLivro) throws IOException {
        return reservaDAO.readByLivro(idLivro);
    }

    public List<Reserva> listarTodos() throws IOException {
        return reservaDAO.readAll();
    }

    public void cancelar(int idLeitor, int idLivro) throws IOException {
        reservaDAO.cancelar(idLeitor, idLivro);
    }

    public void cancelarPorLivro(int idLivro) throws IOException {
        reservaDAO.cancelarPorLivro(idLivro);
    }

    public void cancelarPorLeitor(int idLeitor) throws IOException {
        reservaDAO.cancelarPorLeitor(idLeitor);
    }

    public void fechar() throws IOException {
        reservaDAO.fechar();
    }
}