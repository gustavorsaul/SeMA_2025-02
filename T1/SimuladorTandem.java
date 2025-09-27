import java.util.*;

public class SimuladorTandem {

    // Tipos de eventos
    static final int CHEGADA_Q1 = 1;
    static final int SAIDA_Q1 = 2;
    static final int SAIDA_Q2 = 3;

    // Classe de evento
    static class Evento implements Comparable<Evento> {
        double tempo;
        int tipo;
        Evento(double t, int tipo) { this.tempo = t; this.tipo = tipo; }
        public int compareTo(Evento o) {
            return Double.compare(this.tempo, o.tempo);
        }
    }

    // Classe de fila
    static class Fila {
        int capacidade;       // capacidade total (em serviço + espera)
        int servidores;       // número de servidores
        int ocupados = 0;     // em atendimento
        int espera = 0;       // em espera
        int perdas = 0;       // clientes perdidos
        double[] tempos;      // tempo acumulado por estado
        int n = 0;            // clientes totais no sistema

        Fila(int capacidade, int servidores) {
            this.capacidade = capacidade;
            this.servidores = servidores;
            this.tempos = new double[capacidade + 1];
        }

        void acumula(double delta) {
            tempos[Math.min(n, capacidade)] += delta;
        }
    }

    PriorityQueue<Evento> agenda = new PriorityQueue<>();
    Random rng = new Random(12345);

    // Parâmetros de chegada e serviço
    double chegadaMin, chegadaMax;
    double serv1Min, serv1Max;
    double serv2Min, serv2Max;
    Fila q1, q2;

    double tempo = 0.0;
    double ultimo = 0.0;
    long usados = 0;
    long limite = 100000;
    boolean parar = false;

    public SimuladorTandem(
        double chegadaMin, double chegadaMax,
        double serv1Min, double serv1Max, int s1, int cap1,
        double serv2Min, double serv2Max, int s2, int cap2,
        double primeira, long limite
    ) {
        this.chegadaMin = chegadaMin;
        this.chegadaMax = chegadaMax;
        this.serv1Min = serv1Min;
        this.serv1Max = serv1Max;
        this.serv2Min = serv2Min;
        this.serv2Max = serv2Max;
        this.q1 = new Fila(cap1, s1);
        this.q2 = new Fila(cap2, s2);
        this.limite = limite;
        agenda.add(new Evento(primeira, CHEGADA_Q1));
    }

    double uniforme(double a, double b) {
        usados++;
        return a + rng.nextDouble() * (b - a);
    }

    void acumulaTempos(double agora) {
        double delta = agora - ultimo;
        if (delta > 0) {
            q1.acumula(delta);
            q2.acumula(delta);
        }
        ultimo = agora;
    }

    void simular() {
        while (!agenda.isEmpty() && !parar) {
            Evento e = agenda.poll();
            acumulaTempos(e.tempo);
            tempo = e.tempo;

            if (usados >= limite) { parar = true; break; }

            switch (e.tipo) {
                case CHEGADA_Q1: chegadaQ1(e); break;
                case SAIDA_Q1: saidaQ1(e); break;
                case SAIDA_Q2: saidaQ2(e); break;
            }
        }
        relatorio();
    }

    void chegadaQ1(Evento e) {
        double ia = uniforme(chegadaMin, chegadaMax);
        if (usados < limite) agenda.add(new Evento(e.tempo + ia, CHEGADA_Q1));

        if (q1.n >= q1.capacidade) q1.perdas++;
        else {
            q1.n++;
            if (q1.ocupados < q1.servidores) {
                q1.ocupados++;
                double s = uniforme(serv1Min, serv1Max);
                agenda.add(new Evento(e.tempo + s, SAIDA_Q1));
            } else q1.espera++;
        }
    }

    void saidaQ1(Evento e) {
        q1.ocupados--; q1.n--;
        if (q2.n >= q2.capacidade) q2.perdas++;
        else {
            q2.n++;
            if (q2.ocupados < q2.servidores) {
                q2.ocupados++;
                double s = uniforme(serv2Min, serv2Max);
                agenda.add(new Evento(e.tempo + s, SAIDA_Q2));
            } else q2.espera++;
        }
        if (q1.espera > 0) {
            q1.espera--; q1.ocupados++;
            double s = uniforme(serv1Min, serv1Max);
            agenda.add(new Evento(e.tempo + s, SAIDA_Q1));
        }
    }

    void saidaQ2(Evento e) {
        q2.ocupados--; q2.n--;
        if (q2.espera > 0) {
            q2.espera--; q2.ocupados++;
            double s = uniforme(serv2Min, serv2Max);
            agenda.add(new Evento(e.tempo + s, SAIDA_Q2));
        }
    }

    void relatorio() {
        System.out.println("Tempo total: " + tempo);
        mostraFila("Fila 1", q1);
        mostraFila("Fila 2", q2);
        System.out.println("Aleatórios usados: " + usados);
    }

    void mostraFila(String nome, Fila f) {
        System.out.println("--- " + nome + " ---");
        System.out.println("Perdas: " + f.perdas);
        for (int i = 0; i <= f.capacidade; i++) {
            double p = (tempo > 0) ? (f.tempos[i] / tempo * 100) : 0;
            System.out.printf("%2d clientes: tempo=%.4f (%.2f%%)%n", i, f.tempos[i], p);
        }
    }

    public static void main(String[] args) {
        SimuladorTandem sim = new SimuladorTandem(
            1.0, 4.0,   // chegadas
            3.0, 4.0, 2, 3, // serviço fila1, servidores, capacidade
            2.0, 3.0, 1, 5, // serviço fila2, servidores, capacidade
            1.5, 100000 // primeira chegada e limite
        );
        sim.simular();
    }
}