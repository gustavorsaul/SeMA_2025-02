import java.util.*;
public class SimuladorRede {

    // ---------------- TIPOS DE EVENTO ----------------
    static final int CHEGADA = 1;
    static final int SAIDA = 2;

    // ---------------- CLASSE EVENTO ----------------
    static class Evento implements Comparable<Evento> {
        double tempo;
        int tipo;
        int filaId;

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

    // ---------------- CLASSE FILA ----------------
    static class Fila {
        int capacidade;
        int servidores;
        double servMin, servMax;

        int ocupados = 0;
        int espera = 0;
        int perdas = 0;
        int saidas = 0;
        double[] tempos;
        int n = 0;

        Fila(int capacidade, int servidores, double servMin, double servMax) {
            this.capacidade = (capacidade == 0) ? Integer.MAX_VALUE : capacidade;
            this.servidores = servidores;
            this.servMin = servMin;
            this.servMax = servMax;
            int tamVetor = (this.capacidade == Integer.MAX_VALUE) ? 1000 : this.capacidade + 1;
            this.tempos = new double[tamVetor];
        }

        void acumula(double delta) {
            int estadoAtual = Math.min(n, tempos.length - 1);
            tempos[estadoAtual] += delta;
        }
    }

    // ---------------- ATRIBUTOS DO SIMULADOR ----------------
    private final PriorityQueue<Evento> agenda = new PriorityQueue<>();
    private final Random rng = new Random(12345);

    // ---------------- PARAMETROS DO SISTEMA ----------------
    private final List<Fila> filas = new ArrayList<>();
    private final double[][] matrizRoteamento;
    private final double chegadaMin, chegadaMax;
    private final long limiteAleatorios;
    private long usados = 0;

    // ---------------- ESTADO DA SIMULACAO ----------------
    private double tempo = 0.0;
    private double ultimoTempo = 0.0;

    // ---------------- CONSTRUTOR ----------------
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

        for (double[] params : paramsFilas) {
            filas.add(new Fila((int) params[0], (int) params[1], params[2], params[3]));
        }

        agenda.add(new Evento(primeiraChegada, CHEGADA, 0));
    }

    private double uniforme(double a, double b) {
        if (usados >= limiteAleatorios) return Double.POSITIVE_INFINITY;
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

    // ---------------- PROCESSAMENTO DE EVENTOS ----------------
    private void processarChegada(Evento e) {
        double proximaChegada = uniforme(chegadaMin, chegadaMax);
        if (proximaChegada != Double.POSITIVE_INFINITY) {
            agenda.add(new Evento(tempo + proximaChegada, CHEGADA, 0));
        }
        adicionarNaFila(e.filaId, tempo);
    }

    private void processarSaida(Evento e) {
        Fila f = filas.get(e.filaId);
        f.ocupados--;
        f.n--;
        f.saidas++;
        rotearCliente(e.filaId, tempo);
        if (f.espera > 0) {
            f.espera--;
            f.ocupados++;
            double tempoServico = uniforme(f.servMin, f.servMax);
            if (tempoServico != Double.POSITIVE_INFINITY) {
                agenda.add(new Evento(tempo + tempoServico, SAIDA, e.filaId));
            }
        }
    }
    
    // ---------------- LOGICA DE FILA ----------------
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

    // ---------------- ROTEAMENTO ----------------
    private void rotearCliente(int filaOrigemId, double tempoAtual) {
        double p = rng.nextDouble();
        double p_acumulada = 0.0;
        
        double[] destinos = matrizRoteamento[filaOrigemId];
        for (int filaDestinoId = 0; filaDestinoId < destinos.length; filaDestinoId++) {
            p_acumulada += destinos[filaDestinoId];
            if (p < p_acumulada) {
                adicionarNaFila(filaDestinoId, tempoAtual);
                return;
            }
        }
    }

    // ---------------- RELATORIO ----------------
    private void relatorio() {
        System.out.println("==============================================");
        System.out.printf("Tempo total de simulação: %.2f%n", tempo);
        System.out.println("Números aleatórios usados: " + usados);
        System.out.println("==============================================");

        for (int i = 0; i < filas.size(); i++) {
            mostraFila(i + 1, filas.get(i));
        }
    }

    private void mostraFila(int idx, Fila f) {
        String nome = "Fila " + idx;
        System.out.println("\n--- " + nome + " ---");
        System.out.println("Capacidade: " + (f.capacidade == Integer.MAX_VALUE ? "Infinita" : f.capacidade) +
                           ", Servidores: " + f.servidores);
        System.out.printf("Chegadas entre %.0f e %.0f%n", chegadaMin, chegadaMax);
        System.out.printf("Atendimento entre %.0f e %.0f%n", f.servMin, f.servMax);
        System.out.println("Distribuição de tempo por estado:");
        for (int i = 0; i < f.tempos.length; i++) {
            if (f.tempos[i] > 1e-6) {
                double p = (tempo > 0) ? (f.tempos[i] / tempo * 100) : 0;
                System.out.printf("%3d clientes: tempo=%.2f (%.2f%%)%n", i, f.tempos[i], p);
            }
        }

        double somaTempoPonderado = 0.0;
        for (int i = 0; i < f.tempos.length; i++) {
            somaTempoPonderado += i * f.tempos[i];
        }
        double totalTempo = tempo > 0 ? tempo : 1.0;
        double populacaoMedia = somaTempoPonderado / totalTempo;
        double vazao = f.saidas / totalTempo;
        double mediaServico = (f.servMin + f.servMax) / 2.0;
        double utilizacao = Math.min(1.0, (f.servidores > 0 ? (vazao * mediaServico) / f.servidores : 0.0));
        double tempoResposta = vazao > 0 ? (populacaoMedia / vazao) : 0.0;
        StringBuilder estadosSb = new StringBuilder();
        estadosSb.append("{");
        boolean primeiro = true;
        for (int i = 0; i < f.tempos.length; i++) {
            if (f.tempos[i] > 1e-6) {
                double perc = (f.tempos[i] / totalTempo) * 100.0;
                double arred = Math.round(perc * 100.0) / 100.0;
                if (!primeiro) estadosSb.append(", ");
                estadosSb.append(i).append("=").append(arred);
                primeiro = false;
            }
        }
        estadosSb.append("}");
        System.out.printf("   - População média:       %.2f%n", populacaoMedia);
        System.out.printf("   - Vazão:                 %.2f%n", vazao);
        System.out.printf("   - Utilização:            %.2f%n", utilizacao);
        System.out.printf("   - Tempo de resposta:     %.2f%n", tempoResposta);
        System.out.printf("   - Perdas:                %d%n", f.perdas);
        System.out.println("==============================================");
    }

}