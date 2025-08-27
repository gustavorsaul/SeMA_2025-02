public class SimulacaoFila {
    // Constantes para tipos de evento
    private static final int TIPO_CHEGADA = 1;
    private static final int TIPO_SAIDA = 2;
    
    // Classe interna para Evento
    private static class Evento {
        private double tempo;
        private int tipo;
        
        public Evento(double tempo, int tipo) {
            this.tempo = tempo;
            this.tipo = tipo;
        }
        
        public double getTempo() { return tempo; }
        public int getTipo() { return tipo; }
    }
    
    // Atributos da simulação
    private java.util.PriorityQueue<Evento> escalonador;
    private int capacidadeFila;
    private int numeroServidores;
    private int tamanhoAtual;
    private int clientesPerdidos;
    private double tempoGlobal;
    private double[] temposAcumulados;
    private double ultimoTempo;
    
    // Gerador de números aleatórios
    private long M = 1L << 48;          // 2^48
    private long a = 25214903917L;      // multiplicador
    private long c = 11L;               // incremento
    private long seed;
    
    // Construtor
    public SimulacaoFila(int capacidade, int servidores) {
        this.capacidadeFila = capacidade;
        this.numeroServidores = servidores;
        this.tamanhoAtual = 0;
        this.clientesPerdidos = 0;
        this.tempoGlobal = 0.0;
        this.ultimoTempo = 0.0;
        this.temposAcumulados = new double[capacidade + 1];
        this.seed = System.currentTimeMillis() & (M - 1);
        this.escalonador = new java.util.PriorityQueue<>((e1, e2) -> 
            Double.compare(e1.getTempo(), e2.getTempo()));
    }
    
    // Gerador de número aleatório
    private double nextRandom() {
        seed = (a * seed + c) & (M - 1);
        long bits = seed / (1L << 16);
        return (double) bits / (1L << 32);
    }
    
    // Geração de tempo de chegada (CH) - distribuição uniforme
    private double CH(double min, double max) {
        return min + nextRandom() * (max - min);
    }
    
    // Geração de tempo de serviço (SA) - distribuição uniforme
    private double SA(double min, double max) {
        return min + nextRandom() * (max - min);
    }
    
    // Métodos da fila
    private void filaIn() {
        if (tamanhoAtual < capacidadeFila) {
            tamanhoAtual++;
        }
    }
    
    private void filaOut() {
        if (tamanhoAtual > 0) {
            tamanhoAtual--;
        }
    }
    
    private int filaStatus() { 
        return tamanhoAtual; 
    }
    
    private void filaLoss() { 
        clientesPerdidos++; 
    }
    
    // Métodos do escalonador
    private void escalonadorAdd(Evento evento) {
        escalonador.offer(evento);
    }
    
    private Evento nextEvent() {
        return escalonador.poll();
    }
    
    private boolean hasEvents() {
        return !escalonador.isEmpty();
    }
    
    // Acumulação de tempo
    private void acumulaTempo(Evento evento) {
        double deltaTempo = evento.getTempo() - ultimoTempo;
        temposAcumulados[filaStatus()] += deltaTempo;
        ultimoTempo = evento.getTempo();
        tempoGlobal = evento.getTempo();
    }
    
    // Procedimento CHEGADA
    private void chegada(Evento evento) {
        acumulaTempo(evento);
        
        if (filaStatus() < capacidadeFila) {
            filaIn();
            
            // Se há servidores disponíveis, agenda saída
            if (filaStatus() <= numeroServidores) {
                escalonadorAdd(new Evento(tempoGlobal + SA(5, 6), TIPO_SAIDA));
            }
        } else {
            filaLoss();
        }
        
        // Agenda próxima chegada
        escalonadorAdd(new Evento(tempoGlobal + CH(1, 3), TIPO_CHEGADA));
    }
    
    // Procedimento SAIDA
    private void saida(Evento evento) {
        acumulaTempo(evento);
        filaOut();
        
        // Se ainda há clientes na fila esperando
        if (filaStatus() >= numeroServidores) {
            escalonadorAdd(new Evento(tempoGlobal + SA(5, 6), TIPO_SAIDA));
        }
    }
    
    // Método principal de simulação
    public void simular(int numeroEventos) {
        System.out.println("Iniciando simulação da fila...");
        System.out.println("Capacidade: " + capacidadeFila + " clientes");
        System.out.println("Servidores: " + numeroServidores);
        System.out.println("Número de eventos: " + numeroEventos);
        System.out.println();
        
        // Primeiro evento de chegada
        escalonadorAdd(new Evento(CH(1, 3), TIPO_CHEGADA));
        
        int count = numeroEventos;
        
        // Loop principal da simulação
        while (count > 0 && hasEvents()) {
            Evento evento = nextEvent();
            
            if (evento.getTipo() == TIPO_CHEGADA) {
                chegada(evento);
            } else if (evento.getTipo() == TIPO_SAIDA) {
                saida(evento);
            }
            
            count--;
        }
        
        // Última acumulação de tempo
        acumulaTempo(new Evento(tempoGlobal, -1));
        
        // Exibição dos resultados
        exibirResultados();
    }
    
    // Exibição dos resultados
    private void exibirResultados() {
        System.out.println("=== RESULTADOS DA SIMULAÇÃO ===");
        System.out.println("Tempo total de simulação: " + String.format("%.2f", tempoGlobal));
        System.out.println("Clientes perdidos: " + clientesPerdidos);
        System.out.println();
        
        System.out.println("Distribuição de Probabilidade dos Estados da Fila:");
        System.out.println("Estado | Tempo Acumulado | Probabilidade (%)");
        System.out.println("-------|-----------------|------------------");
        
        double somaTempos = 0;
        for (int i = 0; i < temposAcumulados.length; i++) {
            somaTempos += temposAcumulados[i];
        }
        
        for (int i = 0; i < temposAcumulados.length; i++) {
            double probabilidade = (temposAcumulados[i] / somaTempos) * 100;
            System.out.printf("%6d | %15.2f | %16.2f%%\n", 
                i, temposAcumulados[i], probabilidade);
        }
        
        System.out.println();
        System.out.println("Tempo total verificado: " + String.format("%.2f", somaTempos));
        
        // Cálculo de métricas básicas
        calcularMetricas();
    }
    
    // Cálculo de métricas de desempenho
    private void calcularMetricas() {
        System.out.println("\n=== MÉTRICAS DE DESEMPENHO ===");
        
        // População média (número médio de clientes na fila)
        double populacaoMedia = 0;
        double tempoTotal = 0;
        
        for (int i = 0; i < temposAcumulados.length; i++) {
            populacaoMedia += i * temposAcumulados[i];
            tempoTotal += temposAcumulados[i];
        }
        populacaoMedia /= tempoTotal;
        
        // Taxa de utilização dos servidores
        double tempoOcioso = temposAcumulados[0];
        double utilizacao = ((tempoTotal - tempoOcioso) / tempoTotal) * 100;
        
        // Probabilidade de bloqueio (fila cheia)
        double probBloqueio = (temposAcumulados[capacidadeFila] / tempoTotal) * 100;
        
        System.out.printf("População média: %.3f clientes\n", populacaoMedia);
        System.out.printf("Utilização do sistema: %.2f%%\n", utilizacao);
        System.out.printf("Probabilidade de bloqueio: %.2f%%\n", probBloqueio);
        System.out.printf("Taxa de perda: %.2f%% (%d clientes perdidos)\n", 
            probBloqueio, clientesPerdidos);
    }
    
    // Método main para execução
    public static void main(String[] args) {
        // Configuração da simulação
        int capacidadeFila = 10;
        int numeroServidores = 2;
        int numeroEventos = 100000;
        
        // Criação e execução da simulação
        SimulacaoFila simulacao = new SimulacaoFila(capacidadeFila, numeroServidores);
        simulacao.simular(numeroEventos);
    }
}