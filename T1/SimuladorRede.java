import java.util.*;

/**
 * Simulador de Redes de Filas de propósito geral.
 * Adaptado de um simulador tandem para modelar redes complexas com roteamento probabilístico.
 */
public class SimuladorRede {

    // --- Tipos de Eventos ---
    // Simplificado para apenas dois tipos. O evento em si conterá o ID da fila.
    static final int CHEGADA = 1;
    static final int SAIDA = 2;

    // --- Classe de Evento ---
    // Agora armazena o ID da fila a que se refere.
    static class Evento implements Comparable<Evento> {
        double tempo;
        int tipo;
        int filaId; // ID da Fila (índice no array de filas)

        Evento(double t, int tipo, int filaId) {
            this.tempo = t;
            this.tipo = tipo;
            this.filaId = filaId;
        }

        @Override
        public int compareTo(Evento o) {
            return Double.compare(this.tempo, o.tempo);
        }
    }

    // --- Classe de Fila ---
    // Adicionado parâmetros de serviço para generalização.
    static class Fila {
        int capacidade;
        int servidores;
        double servMin, servMax; // Parâmetros de serviço agora estão aqui

        int ocupados = 0;
        int espera = 0;
        int perdas = 0;
        double[] tempos;
        int n = 0;

        Fila(int capacidade, int servidores, double servMin, double servMax) {
            this.capacidade = (capacidade == 0) ? Integer.MAX_VALUE : capacidade; // Capacidade 0 = infinita
            this.servidores = servidores;
            this.servMin = servMin;
            this.servMax = servMax;
            // Se a capacidade for "infinita", limitamos o vetor de estatísticas por segurança.
            int tamVetor = (this.capacidade == Integer.MAX_VALUE) ? 1000 : this.capacidade + 1;
            this.tempos = new double[tamVetor];
        }

        void acumula(double delta) {
            int estadoAtual = Math.min(n, tempos.length - 1);
            tempos[estadoAtual] += delta;
        }
    }

    // --- Atributos do Simulador ---
    private final PriorityQueue<Evento> agenda = new PriorityQueue<>();
    private final Random rng = new Random(12345); // Semente fixa para reprodutibilidade

    // Parâmetros do sistema
    private final List<Fila> filas = new ArrayList<>();
    private final double[][] matrizRoteamento; // Matriz de probabilidades
    private final double chegadaMin, chegadaMax;
    private final long limiteAleatorios;
    private long usados = 0;

    // Estado da simulação
    private double tempo = 0.0;
    private double ultimoTempo = 0.0;

    /**
     * Construtor do Simulador de Rede.
     * @param chegadaMin Tempo mínimo entre chegadas externas.
     * @param chegadaMax Tempo máximo entre chegadas externas.
     * @param paramsFilas Lista de parâmetros para cada fila [capacidade, servidores, servMin, servMax].
     * @param matrizRoteamento Matriz de probabilidade [de_fila][para_fila].
     * @param primeiraChegada Tempo da primeira chegada.
     * @param limiteAleatorios Limite de números aleatórios a serem gerados para parar a simulação.
     */
    public SimuladorRede(
        double chegadaMin, double chegadaMax,
        List<double[]> paramsFilas,
        double[][] matrizRoteamento,
        double primeiraChegada,
        long limiteAleatorios
    ) {
        this.chegadaMin = chegadaMin;
        this.chegadaMax = chegadaMax;
        this.matrizRoteamento = matrizRoteamento;
        this.limiteAleatorios = limiteAleatorios;

        // Inicializa as filas com base nos parâmetros
        for (double[] params : paramsFilas) {
            filas.add(new Fila((int) params[0], (int) params[1], params[2], params[3]));
        }

        // Agenda a primeira chegada (sempre na Fila 0, por convenção)
        agenda.add(new Evento(primeiraChegada, CHEGADA, 0));
    }

    private double uniforme(double a, double b) {
        if (usados >= limiteAleatorios) return Double.POSITIVE_INFINITY; // Evita gerar mais do que o limite
        usados++;
        return a + rng.nextDouble() * (b - a);
    }

    private void acumulaTempos(double agora) {
        double delta = agora - ultimoTempo;
        if (delta > 0) {
            for (Fila f : filas) {
                f.acumula(delta);
            }
        }
        ultimoTempo = agora;
    }

    public void simular() {
        while (!agenda.isEmpty() && usados < limiteAleatorios) {
            Evento e = agenda.poll();
            acumulaTempos(e.tempo);
            tempo = e.tempo;

            switch (e.tipo) {
                case CHEGADA:
                    processarChegada(e);
                    break;
                case SAIDA:
                    processarSaida(e);
                    break;
            }
        }
        relatorio();
    }

    private void processarChegada(Evento e) {
        // Agenda a próxima chegada externa, pois esta é uma chegada externa
        double proximaChegada = uniforme(chegadaMin, chegadaMax);
        if (proximaChegada != Double.POSITIVE_INFINITY) {
            agenda.add(new Evento(tempo + proximaChegada, CHEGADA, 0));
        }

        // Processa a chegada do cliente na fila de destino
        adicionarNaFila(e.filaId, tempo);
    }

    private void processarSaida(Evento e) {
        Fila f = filas.get(e.filaId);
        f.ocupados--;
        f.n--;

        // Roteia o cliente que acabou o serviço
        rotearCliente(e.filaId, tempo);

        // Se houver cliente na espera, inicia o serviço dele
        if (f.espera > 0) {
            f.espera--;
            f.ocupados++;
            double tempoServico = uniforme(f.servMin, f.servMax);
            if (tempoServico != Double.POSITIVE_INFINITY) {
                agenda.add(new Evento(tempo + tempoServico, SAIDA, e.filaId));
            }
        }
    }
    
    /**
     * Lógica de um cliente (interno ou externo) chegando em uma fila.
     */
    private void adicionarNaFila(int filaId, double tempoAtual) {
        Fila f = filas.get(filaId);
        if (f.n >= f.capacidade) {
            f.perdas++;
        } else {
            f.n++;
            if (f.ocupados < f.servidores) {
                f.ocupados++;
                double tempoServico = uniforme(f.servMin, f.servMax);
                 if (tempoServico != Double.POSITIVE_INFINITY) {
                    agenda.add(new Evento(tempoAtual + tempoServico, SAIDA, filaId));
                }
            } else {
                f.espera++;
            }
        }
    }

    /**
     * Decide para onde o cliente vai após sair da fila 'filaOrigemId'.
     */
    private void rotearCliente(int filaOrigemId, double tempoAtual) {
        double p = rng.nextDouble(); // Não conta como "usado" pois é para roteamento
        double p_acumulada = 0.0;
        
        // Percorre as probabilidades de destino a partir da fila de origem
        double[] destinos = matrizRoteamento[filaOrigemId];
        for (int filaDestinoId = 0; filaDestinoId < destinos.length; filaDestinoId++) {
            p_acumulada += destinos[filaDestinoId];
            if (p < p_acumulada) {
                // O cliente vai para a fila 'filaDestinoId'
                adicionarNaFila(filaDestinoId, tempoAtual);
                return; // Roteamento decidido
            }
        }
        // Se chegou aqui, o cliente saiu do sistema (probabilidade restante)
    }

    private void relatorio() {
        System.out.println("==============================================");
        System.out.printf("Tempo total de simulação: %.4f%n", tempo);
        System.out.println("Números aleatórios usados: " + usados);
        System.out.println("==============================================");

        for (int i = 0; i < filas.size(); i++) {
            mostraFila("Fila " + (i + 1), filas.get(i));
        }
    }

    private void mostraFila(String nome, Fila f) {
        System.out.println("\n--- " + nome + " ---");
        System.out.println("Capacidade: " + (f.capacidade == Integer.MAX_VALUE ? "Infinita" : f.capacidade) +
                           ", Servidores: " + f.servidores);
        System.out.println("Perdas: " + f.perdas);
        System.out.println("Distribuição de tempo por estado:");
        for (int i = 0; i < f.tempos.length; i++) {
            if (f.tempos[i] > 1e-6) { // Mostra apenas estados que ocorreram
                double p = (tempo > 0) ? (f.tempos[i] / tempo * 100) : 0;
                System.out.printf("%3d clientes: tempo=%.4f (%.2f%%)%n", i, f.tempos[i], p);
            }
        }
    }


    public static void main(String[] args) {
        // --- PARÂMETROS DA REDE DO DIAGRAMA ---

        // 1. Parâmetros de Chegada Externa (só para Fila 1)
        double chegadaMin = 2.0;
        double chegadaMax = 4.0;

        // 2. Parâmetros das Filas
        // Formato: {capacidade, servidores, servico_min, servico_max}
        // Nota: Capacidade 0 é tratada como infinita. O diagrama não especifica
        // capacidade para a Fila 1, então assumimos infinita.
        List<double[]> paramsFilas = Arrays.asList(
            new double[]{0,    1, 1.0,  2.0},  // Fila 1
            new double[]{5,    2, 4.0,  6.0},  // Fila 2
            new double[]{10,   2, 5.0, 15.0}  // Fila 3
        );

        // 3. Matriz de Roteamento
        // matriz[i][j] = probabilidade de ir da Fila i+1 para a Fila j+1
        // A probabilidade de SAIR do sistema é 1 - (soma da linha)
        double[][] matrizRoteamento = {
        //   Destino: Fila 1, Fila 2, Fila 3
            {0.0,   0.8,   0.2},  // Origem: Fila 1 (Sai com 1-1=0%)
            {0.3,   0.0,   0.5},  // Origem: Fila 2 (Sai com 1-0.8=20%)
            {0.0,   0.7,   0.0}   // Origem: Fila 3 (Sai com 1-0.7=30%)
        };

        // 4. Parâmetros da Simulação
        double primeiraChegada = uniformeEstatico(chegadaMin, chegadaMax); // Sorteia o tempo da 1a chegada
        long limiteAleatorios = 100000;

        // --- EXECUÇÃO ---
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
    
    // Helper para sortear a primeira chegada na main
    private static double uniformeEstatico(double a, double b) {
        return a + new Random().nextDouble() * (b - a);
    }
}