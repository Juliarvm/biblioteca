package dao;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class IndiceReserva {

    private HashExtensivel hashPorLeitor;
    private HashExtensivel hashPorLivro;
    private RandomAccessFile arquivoRelacao;

    public IndiceReserva(String dirHash, String relPath) throws IOException {
        hashPorLeitor = new HashExtensivel(4, dirHash + "/reserva_leitor.dir", dirHash + "/reserva_leitor.bkt");
        hashPorLivro  = new HashExtensivel(4, dirHash + "/reserva_livro.dir",  dirHash + "/reserva_livro.bkt");

        File rel = new File(relPath);
        File pastaPai = rel.getParentFile();
        if (pastaPai != null) pastaPai.mkdirs();
        arquivoRelacao = new RandomAccessFile(rel, "rw");
    }

    public void inserir(int idLeitor, int idLivro, long posRegistro) throws IOException {
        long posNo = arquivoRelacao.length();
        arquivoRelacao.seek(posNo);

        Long cabecaLeitor = hashPorLeitor.buscar(idLeitor);
        long proxLeitor = (cabecaLeitor != null) ? cabecaLeitor : -1;

        Long cabecaLivro = hashPorLivro.buscar(idLivro);
        long proxLivro = (cabecaLivro != null) ? cabecaLivro : -1;

        arquivoRelacao.writeInt(idLeitor);
        arquivoRelacao.writeInt(idLivro);
        arquivoRelacao.writeLong(posRegistro);
        arquivoRelacao.writeLong(proxLeitor);
        arquivoRelacao.writeLong(proxLivro);
        arquivoRelacao.writeBoolean(false);

        hashPorLeitor.inserir(idLeitor, posNo);
        hashPorLivro.inserir(idLivro, posNo);
    }

    public List<Long> listarPorLeitor(int idLeitor) throws IOException {
        List<Long> posicoes = new ArrayList<>();
        Long cabeca = hashPorLeitor.buscar(idLeitor);
        long pos = (cabeca != null) ? cabeca : -1;
        while (pos != -1) {
            arquivoRelacao.seek(pos);
            arquivoRelacao.readInt();
            arquivoRelacao.readInt();
            long posReg = arquivoRelacao.readLong();
            long proxLeitor = arquivoRelacao.readLong();
            arquivoRelacao.readLong();
            boolean lapide = arquivoRelacao.readBoolean();
            if (!lapide) posicoes.add(posReg);
            pos = proxLeitor;
        }
        return posicoes;
    }

    public List<Long> listarPorLivro(int idLivro) throws IOException {
        List<Long> posicoes = new ArrayList<>();
        Long cabeca = hashPorLivro.buscar(idLivro);
        long pos = (cabeca != null) ? cabeca : -1;
        while (pos != -1) {
            arquivoRelacao.seek(pos);
            arquivoRelacao.readInt();
            arquivoRelacao.readInt();
            long posReg = arquivoRelacao.readLong();
            arquivoRelacao.readLong();
            long proxLivro = arquivoRelacao.readLong();
            boolean lapide = arquivoRelacao.readBoolean();
            if (!lapide) posicoes.add(posReg);
            pos = proxLivro;
        }
        return posicoes;
    }

    public void remover(int idLeitor, int idLivro) throws IOException {
        Long cabeca = hashPorLeitor.buscar(idLeitor);
        long pos = (cabeca != null) ? cabeca : -1;
        while (pos != -1) {
            arquivoRelacao.seek(pos);
            int leitor = arquivoRelacao.readInt();
            int livro  = arquivoRelacao.readInt();
            arquivoRelacao.readLong();
            long proxLeitor = arquivoRelacao.readLong();
            arquivoRelacao.readLong();
            if (leitor == idLeitor && livro == idLivro) {
                long posLapide = pos + 4 + 4 + 8 + 8 + 8;
                arquivoRelacao.seek(posLapide);
                arquivoRelacao.writeBoolean(true);
                return;
            }
            pos = proxLeitor;
        }
    }

    public void removerPorLivro(int idLivro) throws IOException {
        Long cabeca = hashPorLivro.buscar(idLivro);
        long pos = (cabeca != null) ? cabeca : -1;
        while (pos != -1) {
            arquivoRelacao.seek(pos);
            arquivoRelacao.readInt();
            arquivoRelacao.readInt();
            arquivoRelacao.readLong();
            arquivoRelacao.readLong();
            long proxLivro = arquivoRelacao.readLong();
            long posLapide = pos + 4 + 4 + 8 + 8 + 8;
            arquivoRelacao.seek(posLapide);
            arquivoRelacao.writeBoolean(true);
            pos = proxLivro;
        }
    }

    public void removerPorLeitor(int idLeitor) throws IOException {
        Long cabeca = hashPorLeitor.buscar(idLeitor);
        long pos = (cabeca != null) ? cabeca : -1;
        while (pos != -1) {
            arquivoRelacao.seek(pos);
            arquivoRelacao.readInt();
            arquivoRelacao.readInt();
            arquivoRelacao.readLong();
            long proxLeitor = arquivoRelacao.readLong();
            arquivoRelacao.readLong();
            long posLapide = pos + 4 + 4 + 8 + 8 + 8;
            arquivoRelacao.seek(posLapide);
            arquivoRelacao.writeBoolean(true);
            pos = proxLeitor;
        }
    }

    public void fechar() throws IOException {
        arquivoRelacao.close();
        hashPorLeitor.close();
        hashPorLivro.close();
    }
}