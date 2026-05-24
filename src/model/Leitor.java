package model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Leitor {
    private int id;
    private String nome;
    private String email;
    private long dataNascimento;

    public Leitor() {}

    public Leitor(int id, String nome, String email, long dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.dataNascimento = dataNascimento;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public long getDataNascimento() { return dataNascimento; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return "\n--- DADOS DO LEITOR ---\nID: " + id +
               "\nNome: " + nome +
               "\nEmail: " + email +
               "\nData de Nascimento: " + sdf.format(new Date(dataNascimento));
    }
}