import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Consulta estoque e registra movimentações em arquivos de texto UTF-8.
 * A saída é um LOG: não altera o saldo no CSV; esse limite é documentado.
 */
public class EstoqueArquivo {
    private final Path estoque;
    private final Path movimentacoes;
    private String ultimaMovimentacao = "";

    public EstoqueArquivo(Path estoque, Path movimentacoes) {
        this.estoque = Objects.requireNonNull(estoque, "estoque");
        this.movimentacoes = Objects.requireNonNull(movimentacoes, "movimentacoes");
    }

    public int consultarQuantidade(String codigo) throws IOException {
        validarCodigo(codigo);
        try (BufferedReader leitor = Files.newBufferedReader(estoque, StandardCharsets.UTF_8)) {
            String linha;
            int numeroLinha = 0;
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                String[] colunas = linha.split(";", -1);
                if (colunas.length != 2) {
                    throw new IOException("CSV inválido na linha " + numeroLinha);
                }
                if (colunas[0].equals(codigo)) {
                    try {
                        int valor = Integer.parseInt(colunas[1]);
                        if (valor < 0) throw new IOException("Estoque negativo na linha " + numeroLinha);
                        return valor;
                    } catch (NumberFormatException ex) {
                        throw new IOException("Quantidade inválida na linha " + numeroLinha, ex);
                    }
                }
            }
        }
        throw new IOException("Código não encontrado: " + codigo);
    }

    public boolean registrarSaida(String codigo, int quantidade) throws IOException {
        validarCodigo(codigo);
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
        int disponivel = consultarQuantidade(codigo);
        if (quantidade > disponivel) return false;
        try (BufferedWriter escrita = Files.newBufferedWriter(movimentacoes, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            escrita.write(codigo + ";" + quantidade);
            escrita.newLine();
        }
        // Atualização de estado só depois de fechar/gravar o arquivo sem falha.
        ultimaMovimentacao = codigo;
        return true;
    }

    public String getUltimaMovimentacao() { return ultimaMovimentacao; }

    private void validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank() || codigo.contains(";") || codigo.contains("\n") || codigo.contains("\r")) {
            throw new IllegalArgumentException("Código inválido");
        }
    }
}
