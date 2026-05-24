package model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Reserva {

    private int idLeitor;
    private int idLivro;
    private long dataReserva;
    private String status;

    public Reserva() {}

    public Reserva(int idLeitor, int idLivro, long dataReserva, String status) {
        this.idLeitor = idLeitor;
        this.idLivro = idLivro;
        this.dataReserva = dataReserva;
        this.status = status;
    }

    public int getIdLeitor() { return idLeitor; }
    public void setIdLeitor(int idLeitor) { this.idLeitor = idLeitor; }

    public int getIdLivro() { return idLivro; }
    public void setIdLivro(int idLivro) { this.idLivro = idLivro; }

    public long getDataReserva() { return dataReserva; }
    public void setDataReserva(long dataReserva) { this.dataReserva = dataReserva; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return "Leitor: " + idLeitor + " | Livro: " + idLivro +
               " | Data: " + sdf.format(new Date(dataReserva)) +
               " | Status: " + status;
    }
}