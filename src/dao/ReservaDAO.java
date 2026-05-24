package dao;

import model.Reserva;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReservaDAO {

    private static final String ARQUIVO = "data/reservas.db";
    private RandomAccessFile arquivo;
    private IndiceReserva indice;

    public ReservaDAO() throws IOException {
        new File("data").mkdirs();
        arquivo = new RandomAccessFile(ARQUIVO, "rw");
        if (arquivo.length() == 0) {
            arquivo.writeInt(0);
        }
        indice = new IndiceReserva("data/hash", "data/reserva.rel");
    }

    public void create(Reserva r) throws IOException {
        if (existe(r.getIdLeitor(), r.getIdLivro())) {
            throw new IOException("Reserva ja existe para este leitor e livro.");
        }
        arquivo.seek(arquivo.length());
        long pos = arquivo.getFilePointer();
        gravarRegistro(r, false);
        indice.inserir(r.getIdLeitor(), r.getIdLivro(), pos);
        arquivo.seek(0);
        arquivo.writeInt(arquivo.readInt() + 1);
    }

    public Reserva readByLeitorELivro(int idLeitor, int idLivro) throws IOException {
        List<Long> posicoes = indice.listarPorLeitor(idLeitor);
        for (long pos : posicoes) {
            Reserva r = lerRegistro(pos);
            if (r != null && r.getIdLivro() == idLivro) return r;
        }
        return null;
    }

    public List<Reserva> readByLeitor(int idLeitor) throws IOException {
        List<Reserva> lista = new ArrayList<>();
        for (long pos : indice.listarPorLeitor(idLeitor)) {
            Reserva r = lerRegistro(pos);
            if (r != null) lista.add(r);
        }
        return lista;
    }

    public List<Reserva> readByLivro(int idLivro) throws IOException {
        List<Reserva> lista = new ArrayList<>();
        for (long pos : indice.listarPorLivro(idLivro)) {
            Reserva r = lerRegistro(pos);
            if (r != null) lista.add(r);
        }
        return lista;
    }

    public List<Reserva> readAll() throws IOException {
        List<Reserva> lista = new ArrayList<>();
        arquivo.seek(4);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long pos = arquivo.getFilePointer();
            int tam = arquivo.readInt();
            boolean lapide = arquivo.readBoolean();
            if (!lapide) {
                arquivo.seek(pos + 4);
                Reserva r = lerRegistro(pos + 4);
                if (r != null) lista.add(r);
            } else {
                arquivo.seek(pos + 4 + tam);
            }
        }
        return lista;
    }

    public void cancelar(int idLeitor, int idLivro) throws IOException {
        List<Long> posicoes = indice.listarPorLeitor(idLeitor);
        for (long pos : posicoes) {
            arquivo.seek(pos);
            int tam = arquivo.readInt();
            arquivo.seek(pos + 4 + 1);
            int leitor = arquivo.readInt();
            int livro  = arquivo.readInt();
            if (leitor == idLeitor && livro == idLivro) {
                arquivo.seek(pos + 4);
                arquivo.writeBoolean(true);
                indice.remover(idLeitor, idLivro);
                return;
            }
        }
        throw new IOException("Reserva nao encontrada.");
    }

    public void cancelarPorLivro(int idLivro) throws IOException {
        indice.removerPorLivro(idLivro);
        marcarLapidesLivro(idLivro);
    }

    public void cancelarPorLeitor(int idLeitor) throws IOException {
        indice.removerPorLeitor(idLeitor);
        marcarLapidesLeitor(idLeitor);
    }

    public void fechar() throws IOException {
        arquivo.close();
        indice.fechar();
    }

    private boolean existe(int idLeitor, int idLivro) throws IOException {
        return readByLeitorELivro(idLeitor, idLivro) != null;
    }

    private void gravarRegistro(Reserva r, boolean lapide) throws IOException {
        byte[] statusBytes = r.getStatus().getBytes(StandardCharsets.UTF_8);
        int tam = 1 + 4 + 4 + 8 + 4 + statusBytes.length;
        arquivo.writeInt(tam);
        arquivo.writeBoolean(lapide);
        arquivo.writeInt(r.getIdLeitor());
        arquivo.writeInt(r.getIdLivro());
        arquivo.writeLong(r.getDataReserva());
        arquivo.writeInt(statusBytes.length);
        arquivo.write(statusBytes);
    }

    private Reserva lerRegistro(long pos) throws IOException {
        arquivo.seek(pos);
        int tam = arquivo.readInt();
        boolean lapide = arquivo.readBoolean();
        if (lapide) {
            arquivo.seek(pos + 4 + tam);
            return null;
        }
        int idLeitor = arquivo.readInt();
        int idLivro  = arquivo.readInt();
        long data    = arquivo.readLong();
        int tamStatus = arquivo.readInt();
        byte[] statusBytes = new byte[tamStatus];
        arquivo.read(statusBytes);
        String status = new String(statusBytes, StandardCharsets.UTF_8);
        return new Reserva(idLeitor, idLivro, data, status);
    }

    private void marcarLapidesLivro(int idLivro) throws IOException {
        arquivo.seek(4);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long pos = arquivo.getFilePointer();
            int tam = arquivo.readInt();
            boolean lapide = arquivo.readBoolean();
            if (!lapide) {
                int leitor = arquivo.readInt();
                int livro  = arquivo.readInt();
                if (livro == idLivro) {
                    arquivo.seek(pos + 4);
                    arquivo.writeBoolean(true);
                }
            }
            arquivo.seek(pos + 4 + tam);
        }
    }

    private void marcarLapidesLeitor(int idLeitor) throws IOException {
        arquivo.seek(4);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long pos = arquivo.getFilePointer();
            int tam = arquivo.readInt();
            boolean lapide = arquivo.readBoolean();
            if (!lapide) {
                int leitor = arquivo.readInt();
                if (leitor == idLeitor) {
                    arquivo.seek(pos + 4);
                    arquivo.writeBoolean(true);
                }
            }
            arquivo.seek(pos + 4 + tam);
        }
    }
}