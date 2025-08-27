import java.util.*;

public class SimulacaoFila3 {
    
    // Evento simples
    static class Evento {
        double tempo; // tempo
        int tipo; // 1=chegada, 2=saida
        
        Evento(double tempo, int tipo) {
            this.tempo = tempo;
            this.tipo = tipo;
        }
    }
    
    // Lista simples de eventos
    private List<Evento> eventos = new ArrayList<>();
    
    // Parâmetros
    private int capacidade;
    private int servidores;
    private int clientes = 0; // clientes na fila
    private int perdas = 0;
    
    // Tempos
    private double tempo = 0;
    private double[] tempos; // tempo em cada estado
    private double ultimo = 0;
    
    // Intervalos
    private double chegadaMin, chegadaMax;
    private double saidaMin, saidaMax;
    
    // Gerador simples
    private long seed = System.currentTimeMillis();
    
    public SimulacaoFila3(int capacidade, int servidores, 
                         double chegadaMin, double chegadaMax,
                         double saidaMin, double saidaMax, 
                         double tempoInicio) {
        this.capacidade = capacidade;
        this.servidores = servidores;
        this.chegadaMin = chegadaMin;
        this.chegadaMax = chegadaMax;
        this.saidaMin = saidaMin;
        this.saidaMax = saidaMax;
        this.tempo = tempoInicio;
        this.ultimo = tempoInicio;
        this.tempos = new double[capacidade + 1];
    }
    
    // Gerador básico
    private double rand() {
        seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
        return (double)seed / 0x7fffffffL;
    }
    
    private double entre(double min, double max) {
        return min + rand() * (max - min);
    }
    
    // Adicionar evento ordenado
    private void addEvento(double tempo, int tipo) {
        Evento novo = new Evento(tempo, tipo);
        int i = 0;
        while (i < eventos.size() && eventos.get(i).tempo <= tempo) {
            i++;
        }
        eventos.add(i, novo);
    }
    
    // Próximo evento
    private Evento proximoEvento() {
        return eventos.isEmpty() ? null : eventos.remove(0);
    }
    
    // Acumular tempo no estado atual
    private void acumula(double novoTempo) {
        tempos[clientes] += novoTempo - ultimo;
        ultimo = novoTempo;
        tempo = novoTempo;
    }
    
    // Chegada
    private void chegada(Evento e) {
        acumula(e.tempo);
        
        if (clientes < capacidade) {
            clientes++;
            if (clientes <= servidores) {
                addEvento(tempo + entre(saidaMin, saidaMax), 2);
            }
        } else {
            perdas++;
        }
        
        addEvento(tempo + entre(chegadaMin, chegadaMax), 1);
    }
    
    // Saída
    private void saida(Evento e) {
        acumula(e.tempo);
        clientes--;
        
        if (clientes >= servidores) {
            addEvento(tempo + entre(saidaMin, saidaMax), 2);
        }
    }
    
    public void simular() {
        int count = 100000;
        
        // Primeira chegada
        addEvento(tempo + entre(chegadaMin, chegadaMax), 1);
        
        while (count > 0 && !eventos.isEmpty()) {
            Evento e = proximoEvento();
            
            if (e.tipo == 1) {
                chegada(e);
            } else {
                saida(e);
            }
            count--;
        }
        
        // Mostrar resultados
        System.out.println("Tempo total: " + String.format("%.2f", tempo));
        System.out.println("Perdas: " + perdas);
        
        for (int i = 0; i <= capacidade; i++) {
            double p = (tempos[i] / tempo) * 100;
            System.out.println(i + ": " + String.format("%.1f", tempos[i]) + 
                             " (" + String.format("%.1f", p) + "%)");
        }
    }
    
    public static void main(String[] args) {
        // capacidade, servidores, chegadaMin, chegadaMax, saidaMin, saidaMax, inicio
        SimulacaoFila3 sim = new SimulacaoFila3(5, 1, 2.0, 5.0, 3.0, 5.0, 2.0);
        sim.simular();
    }
}