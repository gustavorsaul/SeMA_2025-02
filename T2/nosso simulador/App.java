import java.util.*;

public class App {

    public static void main(String[] args) {
        // ---------------- PARAMETROS DA REDE ----------------
        // Edite os valores abaixo para configurar a rede.
        // chegadaMin/Max: intervalo uniforme entre chegadas EXTERNAS na Fila 1.
        // paramsFilas: {capacidade(0=infinita), servidores, servico_min, servico_max} por fila.
        // matrizRoteamento: linhas=origem, colunas=destino; soma da linha ≤ 1 (resto = saída do sistema).

        double chegadaMin = 5.0;
        double chegadaMax = 10.0;

        List<double[]> paramsFilas = Arrays.asList(
            new double[]{0,     3, 10.0,  20.0},  // Fila 1: {capacidade, servidores, servico_min, servico_max}
            new double[]{20,    2, 30.0,  60.0},  // Fila 2: {capacidade, servidores, servico_min, servico_max}
            new double[]{0,     2, 60.0,  240.0}   // Fila 3: {capacidade, servidores, servico_min, servico_max}
        );

        double[][] matrizRoteamento = {
            // Destino: Fila 1, Fila 2, Fila 3
            {0.0,   0.15,   0.0},  // Origem: Fila 1
            {0.0,   0.0,   0.35},  // Origem: Fila 2
            {0.0,   1.0,   0.0}   // Origem: Fila 3
        };

        // ---------------- PARAMETROS DA SIMULACAO ----------------
        // Defina a 1ª chegada: fixa (apenas para o primeiro cliente) ou sorteada no intervalo.
        // limiteAleatorios: número máximo de amostras aleatórias para encerrar a simulação.
        boolean usarPrimeiraChegadaFixa = true;
        double primeiraChegadaFixa = 5.0;
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


