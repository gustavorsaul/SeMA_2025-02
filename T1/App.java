import java.util.*;

public class App {

    public static void main(String[] args) {
        // ---------------- PARAMETROS DA REDE ----------------
        // Edite os valores abaixo para configurar a rede.
        // chegadaMin/Max: intervalo uniforme entre chegadas EXTERNAS na Fila 1.
        // paramsFilas: {capacidade(0=infinita), servidores, servico_min, servico_max} por fila.
        // matrizRoteamento: linhas=origem, colunas=destino; soma da linha ≤ 1 (resto = saída do sistema).

        double chegadaMin = 2.0;
        double chegadaMax = 4.0;

        List<double[]> paramsFilas = Arrays.asList(
            new double[]{0,    1, 1.0,  2.0},  // Fila 1: {capacidade, servidores, servico_min, servico_max}
            new double[]{5,    2, 4.0,  6.0},  // Fila 2: {capacidade, servidores, servico_min, servico_max}
            new double[]{10,   2, 5.0, 15.0}   // Fila 3: {capacidade, servidores, servico_min, servico_max}
        );

        double[][] matrizRoteamento = {
            // Destino: Fila 1, Fila 2, Fila 3
            {0.0,   0.8,   0.2},  // Origem: Fila 1
            {0.3,   0.0,   0.5},  // Origem: Fila 2
            {0.0,   0.7,   0.0}   // Origem: Fila 3
        };

        // ---------------- PARAMETROS DA SIMULACAO ----------------
        // Defina a 1ª chegada: fixa (apenas para o primeiro cliente) ou sorteada no intervalo.
        // limiteAleatorios: número máximo de amostras aleatórias para encerrar a simulação.
        boolean usarPrimeiraChegadaFixa = true;
        double primeiraChegadaFixa = 2.0;
        double primeiraChegada = usarPrimeiraChegadaFixa
            ? primeiraChegadaFixa
            : uniformeEstatico(chegadaMin, chegadaMax);
        long limiteAleatorios = 100000;

        // ---------------- EXECUCAO ----------------
        // Instancia o simulador e executa. Saída aparece no console.
        SimuladorRede sim = new SimuladorRede(
            chegadaMin,
            chegadaMax,
            paramsFilas,
            matrizRoteamento,
            primeiraChegada,
            limiteAleatorios
        );

        System.out.println("Iniciando simulação da rede...");
        sim.simular();
    }

    // ---------------- FUNCOES AUXILIARES ----------------
    private static double uniformeEstatico(double a, double b) {
        return a + new Random().nextDouble() * (b - a);
    }
}


