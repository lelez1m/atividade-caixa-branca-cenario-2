
public class TestesLegado {
    public static void main(String[] args) {
        EstoqueArquivo estoque = new EstoqueArquivo();

        int antes = estoque.consultarQuantidade("A");
        System.out.println("Estoque inicial: " + antes);

        boolean negativo = estoque.registrarSaida("A", -3);
        System.out.println("Saida negativa aceita: " + negativo);

        boolean zero = estoque.registrarSaida("A", 0);
        System.out.println("Saida zero aceita: " + zero);

        boolean normal = estoque.registrarSaida("A", 2);
        System.out.println("Saida de 2 unidades aceita: " + normal);

        int depois = estoque.consultarQuantidade("A");
        System.out.println("Estoque apos as saidas: " + depois);

        System.out.println("Ultima movimentacao: "
                + estoque.getUltimaMovimentacao());
    }
}
