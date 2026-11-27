
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Classe responsável por consultar o estoque e registrar as saídas.
 * Aqui foram feitas algumas melhorias para evitar erros que existiam
 * no código original, principalmente na validação e leitura dos arquivos.
 */
public class EstoqueArquivo {

    // Caminhos dos arquivos utilizados pelo sistema.
    private final Path estoque;
    private final Path movimentacoes;

    // Guarda o código do último produto que teve uma saída registrada.
    private String ultimaMovimentacao = "";

    public EstoqueArquivo(Path estoque, Path movimentacoes) {

        // Verifica se os caminhos foram informados antes de utilizá-los.
        this.estoque = Objects.requireNonNull(estoque, "estoque");
        this.movimentacoes = Objects.requireNonNull(movimentacoes, "movimentacoes");
    }

    public int consultarQuantidade(String codigo) throws IOException {

        // Antes de consultar, confere se o código informado é válido.
        validarCodigo(codigo);

        // O try-with-resources fecha o arquivo automaticamente após a leitura.
        // Isso resolve um dos problemas encontrados no código original.
        try (BufferedReader leitor = Files.newBufferedReader(estoque, StandardCharsets.UTF_8)) {

            String linha;
            int numeroLinha = 0;

            // Percorre as linhas do CSV até encontrar o produto ou chegar ao final.
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;

                // Separa o código e a quantidade usando o ponto e vírgula.
                // O -1 mantém os campos vazios para conseguirmos identificar erros.
                String[] colunas = linha.split(";", -1);

                // Uma linha válida precisa ter o código e a quantidade.
                if (colunas.length != 2) {
                    throw new IOException("CSV inválido na linha " + numeroLinha);
                }

                // Verifica se o código da linha é o mesmo que estamos procurando.
                if (colunas[0].equals(codigo)) {
                    try {

                        // Converte a quantidade do arquivo para um número inteiro.
                        int valor = Integer.parseInt(colunas[1]);

                        // Não faz sentido ter quantidade negativa no estoque.
                        if (valor < 0) {
                            throw new IOException("Estoque negativo na linha " + numeroLinha);
                        }

                        // Encontrou o produto e a quantidade está válida.
                        return valor;

                    } catch (NumberFormatException ex) {

                        // Se a quantidade não for um número, informa onde está o erro.
                        throw new IOException("Quantidade inválida na linha " + numeroLinha, ex);
                    }
                }
            }
        }

        // Diferente do original, agora um código inexistente gera um erro claro.
        throw new IOException("Código não encontrado: " + codigo);
    }

    public boolean registrarSaida(String codigo, int quantidade) throws IOException {

        // Confere se o código informado é válido.
        validarCodigo(codigo);

        // Impede saídas com quantidade zero ou negativa.
        // No código original, esses valores eram aceitos.
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }

        // Consulta a quantidade disponível antes de registrar a saída.
        int disponivel = consultarQuantidade(codigo);

        // Se não houver estoque suficiente, a operação não é realizada.
        if (quantidade > disponivel) {
            return false;
        }

        // Abre o arquivo de movimentações e adiciona o novo registro.
        // O arquivo será fechado automaticamente ao terminar a escrita.
        try (BufferedWriter escrita = Files.newBufferedWriter(
                movimentacoes,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            // Registra o código do produto e a quantidade retirada.
            escrita.write(codigo + ";" + quantidade);
            escrita.newLine();
        }

        // Só atualiza a última movimentação depois que a gravação termina.
        ultimaMovimentacao = codigo;

        // Retorna true porque o registro foi realizado sem erro.
        return true;
    }

    public String getUltimaMovimentacao() {

        // Permite consultar qual foi o último produto movimentado.
        return ultimaMovimentacao;
    }

    private void validarCodigo(String codigo) {

        // Evita códigos vazios, nulos ou com caracteres que podem
        // atrapalhar a organização do arquivo CSV e das movimentações.
        if (codigo == null ||
                codigo.isBlank() ||
                codigo.contains(";") ||
                codigo.contains("\n") ||
                codigo.contains("\r")) {

            throw new IllegalArgumentException("Código inválido");
        }
    }
}
