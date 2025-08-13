public class App {

    public static void main(String[] args) {

        /*
            M
                2**48
            a
                25.214.903.917
            c
                11

            bits
                bits 47 .. 16

         */

        long M = 1L << 48;          // 2^48
        long a = 25214903917L;      // multiplicador
        long c = 11L;               // incremento
        long seed = System.currentTimeMillis() & (M - 1); // semente inicial

        System.out.println(seed);
        // Cabeçalho para facilitar import no Excel
        System.out.println("Indice;Ui");
        

        for (int i = 1; i <= 1000; i++) {
            // Fórmula do LCG (garante valor positivo usando máscara)
            seed = (a * seed + c) & (M - 1);

            // Descarta 16 bits menos significativos via divisão
            long bits = seed / (1L << 16);

            // Normaliza para [0,1)
            double Ui = (double) bits / (1L << 32);

            // Imprime no formato Excel-friendly com 3 casas decimais
            System.out.println(i + ";" + String.format("%.5f", Ui));
        }
    }
}