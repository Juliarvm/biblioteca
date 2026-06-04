package compression;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BackupManager {

    private static final String DATA_DIR = "data";

    private final Huffman huffman;
    private final Lzw lzw;

    public enum Algoritmo {HUFFMAN, LZW}

    public BackupManager() {
        this.huffman = new Huffman();
        this.lzw = new Lzw();
    }

    public void gerarBackup(String arquivoDestino, Algoritmo algoritmo) throws Exception {
	System.out.println("Passo 1");
        ByteArrayOutputStream raw =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(raw);
	System.out.println("Passo 2");
        List<Path> arquivos =
                listarArquivos();
        System.out.println("Arquivos encontrados: "+ arquivos.size());
        
	out.writeInt(arquivos.size());

        for (Path arquivo : arquivos) {

            byte[] conteudo =
                    Files.readAllBytes(arquivo);

            String nomeRelativo =
                    Paths.get(DATA_DIR)
                         .relativize(arquivo)
                         .toString();

            out.writeUTF(nomeRelativo);

            out.writeLong(conteudo.length);

            out.write(conteudo);
        }
	System.out.println("Passo 3");
        out.flush();

	System.out.println("Passo 4 (compactacao baseado no algoritmo)");
        byte[] compactado;

        switch (algoritmo) {

    case HUFFMAN:
        compactado =
                huffman.compress(
                        raw.toByteArray()
                );
        break;

    case LZW:
        compactado =
                lzw.compress(
                        raw.toByteArray()
                );
        break;

    default:
        throw new IllegalArgumentException(
                "Algoritmo invalido"
        );
}

        System.out.println("Passo 5");

        Files.write(
                Paths.get(arquivoDestino),
                compactado,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
	System.out.println("Saindo de gerar Backup");
    }

    public void restaurarBackup(String arquivoBackup, Algoritmo algoritmo)
            throws Exception {

        byte[] compactado =
                Files.readAllBytes(
                        Paths.get(arquivoBackup)
                );

        byte[] raw;

        switch (algoritmo) {

    case HUFFMAN:
        raw =
                huffman.decompress(
                        compactado
                );
        break;

    case LZW:
        raw =
                lzw.decompress(
                        compactado
                );
        break;

    default:
        throw new IllegalArgumentException(
                "Algoritmo invalido"
        );
}

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(
                                raw
                        )
                );

        int quantidade =
                in.readInt();

        for (int i = 0; i < quantidade; i++) {

            String nome =
                    in.readUTF();

            long tamanho =
                    in.readLong();

            byte[] conteudo =
                    new byte[(int)tamanho];

            in.readFully(conteudo);

            Path destino =
                    Paths.get(
                            DATA_DIR,
                            nome
                    );

            Files.createDirectories(
                    destino.getParent()
            );

            Files.write(
                    destino,
                    conteudo,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    private List<Path> listarArquivos()
            throws IOException {

        List<Path> arquivos =
                new ArrayList<>();

        Files.walk(Paths.get(DATA_DIR))
                .filter(Files::isRegularFile)
                .forEach(arquivos::add);

        return arquivos;
    }
}
