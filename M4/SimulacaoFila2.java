public class SimulacaoFila2 {
    // Tipos de evento
    private static final int TIPO_CHEGADA = 1;
    private static final int TIPO_SAIDA = 2;
    
    // Evento simples
    private static class Evento {
        double tempo;
        int tipo;
        
        Evento(double tempo, int tipo) {
            this.tempo = tempo;
            this.tipo = tipo;
        }
    }
    
    // Fila de eventos ordenada por tempo
    private java.util.PriorityQueue<Evento> escalonador = 
        new java.util.PriorityQueue<>((e1, e2) -> Double.compare(e1.tempo, e2.tempo));
    
    // Parâmetros da fila
    private int K; // capacidade
    private int servers; // servidores
    private int status = 0; // tamanho atual
    private int losses = 0; // perdas
    
    // Parâmetros de tempo
    private double chegadaMin, chegadaMax; // intervalo entre chegadas
    private double servicoMin, servicoMax; // tempo de atendimento
    
    // Controle de tempo
    private double TG = 0; // tempo global
    private double[] times; // tempos por estado
    private double lastTime = 0;
    
    // Gerador LCG
    private long M = 1L << 48;
    private long a = 25214903917L;
    private long c = 11L;
    private long seed = System.currentTimeMillis() & (M - 1);
    
    public SimulacaoFila2(int capacity, int servers, 
                         double chegadaMin, double chegadaMax,
                         double servicoMin, double servicoMax) {
        this.K = capacity;
        this.servers = servers;
        this.chegadaMin = chegadaMin;
        this.chegadaMax = chegadaMax;
        this.servicoMin = servicoMin;
        this.servicoMax = servicoMax;
        this.times = new double[K + 1];
    }
    
    // Gerador de números aleatórios
    private double random() {
        seed = (a * seed + c) & (M - 1);
        return (double)(seed / (1L << 16)) / (1L << 32);
    }
    
    private double CH(double min, double max) {
        return min + random() * (max - min);
    }
    
    private double SA(double min, double max) {
        return min + random() * (max - min);
    }
    
    // AcumulaTempo
    private void acumulaTempo(Evento ev) {
        times[status] += ev.tempo - lastTime;
        lastTime = ev.tempo;
        TG = ev.tempo;
    }
    
    // CHEGADA
    private void CHEGADA(Evento ev) {
        acumulaTempo(ev);
        
        if (status < K) {
            status++; // Fila.In()
            if (status <= servers) {
                escalonador.add(new Evento(TG + SA(servicoMin, servicoMax), TIPO_SAIDA));
            }
        } else {
            losses++; // Fila.Loss()
        }
        
        escalonador.add(new Evento(TG + CH(chegadaMin, chegadaMax), TIPO_CHEGADA));
    }
    
    // SAIDA
    private void SAIDA(Evento ev) {
        acumulaTempo(ev);
        status--; // Fila.Out()
        
        if (status >= servers) {
            escalonador.add(new Evento(TG + SA(servicoMin, servicoMax), TIPO_SAIDA));
        }
    }
    
    // NextEvent
    private Evento NextEvent() {
        return escalonador.poll();
    }
    
    public void simular() {
        int count = 100000;
        
        // Primeiro evento
        escalonador.add(new Evento(CH(chegadaMin, chegadaMax), TIPO_CHEGADA));
        
        while (count > 0 && !escalonador.isEmpty()) {
            Evento evento = NextEvent();
            
            if (evento.tipo == TIPO_CHEGADA) {
                CHEGADA(evento);
            } else if (evento.tipo == TIPO_SAIDA) {
                SAIDA(evento);
            }
            count--;
        }
        
        // Resultados
        System.out.println("Tempo total: " + String.format("%.2f", TG));
        System.out.println("Perdas: " + losses);
        System.out.println();
        
        for (int i = 0; i <= K; i++) {
            double prob = (times[i] / TG) * 100;
            System.out.println(i + ": " + String.format("%.2f", times[i]) + 
                             " (" + String.format("%.2f", prob) + "%)");
        }
    }
    
    public static void main(String[] args) {
        // Parâmetros: capacidade, servidores, chegadaMin, chegadaMax, servicoMin, servicoMax
        SimulacaoFila2 sim = new SimulacaoFila2(5, 2, 2.0, 5.0, 3.0, 5.0);
        sim.simular();
    }
}