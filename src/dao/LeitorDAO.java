package dao;

import model.Leitor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LeitorDAO {
    private final RandomAccessFile arq;
    private final IndiceDireto indice;

    public LeitorDAO() throws IOException {
        File dir = new File("data");
        dir.mkdirs();
        this.arq = new RandomAccessFile("data/leitores.db", "rw");
        this.indice = new IndiceDireto("data/leitores.idx");
        if (this.arq.length() == 0) {
            this.arq.writeInt(0);
        }
    }

    public int create(Leitor leitor) throws IOException {
        if (existeEmail(leitor.getEmail())) {
            throw new IllegalArgumentException("Email ja cadastrado.");
        }
        this.arq.seek(0);
        int novoId = this.arq.readInt() + 1;
        leitor.setId(novoId);
        this.arq.seek(0);
        this.arq.writeInt(novoId);
        long pos = this.arq.length();
        gravarLeitor(leitor, pos);
        this.indice.atualizar(novoId, pos);
        return novoId;
    }

    public Leitor read(int id) throws IOException {
        Long pos = this.indice.buscar(id);
        if (pos == null) return null;
        this.arq.seek(pos);
        boolean lapide = this.arq.readBoolean();
        if (lapide) return null;
        this.arq.readInt();
        return lerRegistro();
    }

    public List<Leitor> readAll() throws IOException {
        List<Leitor> lista = new ArrayList<>();
        this.arq.seek(4);
        while (this.arq.getFilePointer() < this.arq.length()) {
            long pos = this.arq.getFilePointer();
            boolean lapide = this.arq.readBoolean();
            int tam = this.arq.readInt();
            if (!lapide) {
                lista.add(lerRegistro());
            } else {
                this.arq.seek(pos + 1 + 4 + tam);
            }
        }
        return lista;
    }

    public boolean update(Leitor leitor) throws IOException {
        Long pos = this.indice.buscar(leitor.getId());
        if (pos == null) return false;
        Leitor atual = read(leitor.getId());
        if (atual == null) return false;
        if (!atual.getEmail().equalsIgnoreCase(leitor.getEmail()) && existeEmail(leitor.getEmail())) {
            throw new IllegalArgumentException("Email ja cadastrado.");
        }
        this.arq.seek(pos);
        this.arq.writeBoolean(true);
        long novaPosicao = this.arq.length();
        gravarLeitor(leitor, novaPosicao);
        this.indice.atualizar(leitor.getId(), novaPosicao);
        return true;
    }

    public boolean delete(int id) throws IOException {
        Long pos = this.indice.buscar(id);
        if (pos == null) return false;
        Leitor leitor = read(id);
        if (leitor == null) return false;
        this.arq.seek(pos);
        this.arq.writeBoolean(true);
        this.indice.remover(id);
        return true;
    }

    private boolean existeEmail(String email) throws IOException {
        for (Leitor l : readAll()) {
            if (l.getEmail().equalsIgnoreCase(email)) return true;
        }
        return false;
    }

    private void gravarLeitor(Leitor leitor, long pos) throws IOException {
        this.arq.seek(pos);
        this.arq.writeBoolean(false);
        byte[] nomeBytes  = leitor.getNome().getBytes(StandardCharsets.UTF_8);
        byte[] emailBytes = leitor.getEmail().getBytes(StandardCharsets.UTF_8);
        int tam = 4 + 8 + 4 + nomeBytes.length + 4 + emailBytes.length;
        this.arq.writeInt(tam);
        this.arq.writeInt(leitor.getId());
        this.arq.writeLong(leitor.getDataNascimento());
        this.arq.writeInt(nomeBytes.length);
        this.arq.write(nomeBytes);
        this.arq.writeInt(emailBytes.length);
        this.arq.write(emailBytes);
    }

    private Leitor lerRegistro() throws IOException {
        int id = this.arq.readInt();
        long dataNascimento = this.arq.readLong();
        String nome  = readString();
        String email = readString();
        return new Leitor(id, nome, email, dataNascimento);
    }

    private String readString() throws IOException {
        int tam = this.arq.readInt();
        byte[] bytes = new byte[tam];
        this.arq.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}