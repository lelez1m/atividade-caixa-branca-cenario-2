import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestesEstoque {
    static int passou = 0;
    static int falhou = 0;
    interface Caso { void executar() throws Exception; }
    static void testar(String id, Caso caso) {
        try { caso.executar(); passou++; System.out.println("PASSOU " + id); }
        catch (Throwable t) { falhou++; System.out.println("FALHOU " + id + " -> " + t); }
    }
    static void igual(Object esperado, Object observado) {
        if (!java.util.Objects.equals(esperado, observado)) throw new AssertionError("esperado=" + esperado + ", observado=" + observado);
    }
    static void deveLancar(Class<? extends Throwable> tipo, Caso caso) throws Exception {
        try { caso.executar(); } catch (Throwable t) { if (tipo.isInstance(t)) return; throw t; }
        throw new AssertionError("exceção esperada: " + tipo.getSimpleName());
    }
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("caixa-branca-");
        Path csv = pasta.resolve("estoque.csv"), log = pasta.resolve("movimentacoes.txt");
        EstoqueArquivo s = new EstoqueArquivo(csv, log);
        Files.writeString(csv, "A;10\nB;0\nC;7\n", StandardCharsets.UTF_8);
        testar("T01 consulta primeira linha", () -> igual(10, s.consultarQuantidade("A")));
        testar("T02 consulta após iteração", () -> igual(7, s.consultarQuantidade("C")));
        testar("T03 código ausente", () -> deveLancar(IOException.class, () -> s.consultarQuantidade("Z")));
        testar("T04 saída igual ao disponível", () -> { igual(true, s.registrarSaida("A", 10)); igual("A", s.getUltimaMovimentacao()); igual("A;10\n", Files.readString(log).replace("\r\n", "\n")); });
        testar("T05 saída maior que disponível", () -> igual(false, s.registrarSaida("C", 8)));
        testar("T06 saída zero", () -> deveLancar(IllegalArgumentException.class, () -> s.registrarSaida("A", 0)));
        testar("T07 saída negativa", () -> deveLancar(IllegalArgumentException.class, () -> s.registrarSaida("A", -3)));
        testar("T08 código nulo", () -> deveLancar(IllegalArgumentException.class, () -> s.consultarQuantidade(null)));
        testar("T09 injeção de delimitador", () -> deveLancar(IllegalArgumentException.class, () -> s.registrarSaida("A;B", 1)));
        Files.writeString(csv, "A;abc\n", StandardCharsets.UTF_8);
        testar("T10 número malformado", () -> deveLancar(IOException.class, () -> s.consultarQuantidade("A")));
        Files.writeString(csv, "A\n", StandardCharsets.UTF_8);
        testar("T11 coluna faltante", () -> deveLancar(IOException.class, () -> s.consultarQuantidade("A")));
        Files.writeString(csv, "A;-1\n", StandardCharsets.UTF_8);
        testar("T12 estoque negativo", () -> deveLancar(IOException.class, () -> s.consultarQuantidade("A")));
        Files.delete(csv);
        testar("T13 CSV inexistente", () -> deveLancar(IOException.class, () -> s.consultarQuantidade("A")));
        Files.writeString(csv, "A;5\n", StandardCharsets.UTF_8);
        Path diretorioLog = Files.createDirectory(pasta.resolve("sem-permissao-log"));
        EstoqueArquivo erro = new EstoqueArquivo(csv, diretorioLog);
        testar("T14 falha escrita mantém estado", () -> { deveLancar(IOException.class, () -> erro.registrarSaida("A", 1)); igual("", erro.getUltimaMovimentacao()); });
        testar("T15 append múltiplas saídas", () -> { igual(true, s.registrarSaida("A", 1)); igual(2, Files.readAllLines(log).size()); });
        System.out.println("TOTAL " + passou + " passou; " + falhou + " falhou");
        if (falhou > 0) System.exit(1);
    }
}
