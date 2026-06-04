package compression;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BackupManager {

    private static final String DATA_DIR = "data";

    private final Huffman huffman;

    public BackupManager() {
        this.huffman = new Huffman();
    }

    public void gerarBackup(String arquivoDestino) throws Exception {
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

	System.out.println("Passo 4");
        byte[] compactado = huffman.compress(raw.toByteArray());

        System.out.println("Passo 5");

        Files.write(
                Paths.get(arquivoDestino),
                compactado,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
	System.out.println("Saindo de gerar Backup");
    }

    public void restaurarBackup(String arquivoBackup)
            throws Exception {

        byte[] compactado =
                Files.readAllBytes(
                        Paths.get(arquivoBackup)
                );

        byte[] descompactado =
                huffman.decompress(compactado);

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(
                                descompactado
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
